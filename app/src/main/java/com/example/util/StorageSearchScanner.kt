package com.example.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.model.FileSystemItem
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class SearchMatchResult(
    val item: FileSystemItem,
    val matchedKeywords: List<String>,
    val snippet: String? = null,
    val matchType: MatchType = MatchType.FILENAME
)

enum class MatchType {
    FILENAME,
    CONTENT_TEXT,
    DOCUMENT_BODY,
    TAG_NAME
}

object StorageSearchScanner {
    private const val TAG = "StorageSearchScanner"
    private var isPdfBoxInitialized = false

    /**
     * Initializes PDFBoxResourceLoader safely once.
     */
    fun initPdfBoxIfNeeded(context: Context) {
        if (!isPdfBoxInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isPdfBoxInitialized = true
                Log.d(TAG, "PDFBoxResourceLoader initialized successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize PDFBoxResourceLoader: ${e.message}", e)
            }
        }
    }

    /**
     * Splits comma-separated keywords (e.g. "John Doe, 9876543210" -> ["john doe", "9876543210"]).
     */
    fun parseKeywords(query: String): List<String> {
        return query.split(",")
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
    }

    /**
     * Recursively scans storage locations or directory for files matching any or all keywords.
     * Uses PDFBox for PDF text extraction and deep multi-format parsing.
     */
    suspend fun searchStorage(
        context: Context,
        keywords: List<String>,
        rootPath: String? = null,
        matchAllKeywords: Boolean = false,
        onProgress: ((scannedCount: Int, foundCount: Int) -> Unit)? = null
    ): List<SearchMatchResult> = withContext(Dispatchers.IO) {
        if (keywords.isEmpty()) return@withContext emptyList()

        initPdfBoxIfNeeded(context)

        val results = mutableListOf<SearchMatchResult>()
        val seenPaths = HashSet<String>()
        val rootsToScan = mutableListOf<File>()

        if (!rootPath.isNullOrBlank()) {
            val root = File(rootPath)
            if (root.exists()) rootsToScan.add(root)
        } else {
            // Prioritized order: Documents & Downloads first, then root storage, then others
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val publicDCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val publicPictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val external = Environment.getExternalStorageDirectory()
            val emulatedRoot = File("/storage/emulated/0")
            val internal = context.filesDir
            val externalAppFiles = context.getExternalFilesDir(null)

            listOfNotNull(
                publicDocs,
                publicDownloads,
                publicDCIM,
                publicPictures,
                external,
                emulatedRoot,
                internal,
                externalAppFiles
            ).forEach { dir ->
                if (dir.exists() && seenPaths.add(dir.canonicalPath)) {
                    rootsToScan.add(dir)
                }
            }
        }

        var scannedCount = 0
        val maxFilesToScan = 35000 // Deep scan limit

        for (root in rootsToScan) {
            val stack = ArrayDeque<File>()
            stack.add(root)

            while (stack.isNotEmpty() && scannedCount < maxFilesToScan) {
                val current = stack.removeFirst()
                val canonical = try { current.canonicalPath } catch (_: Exception) { current.absolutePath }
                
                // Skip virtual or kernel mounts & noisy thumbnail/cache folders
                if (isIgnoredDirectory(canonical)) {
                    continue
                }

                if (current.isDirectory) {
                    val children = try { current.listFiles() } catch (_: Exception) { null }
                    if (children != null) {
                        for (child in children) {
                            stack.add(child)
                        }
                    }
                } else {
                    scannedCount++
                    if (scannedCount % 15 == 0) {
                        onProgress?.invoke(scannedCount, results.size)
                    }

                    // Inspect file content
                    try {
                        val match = inspectFileForKeywords(current, keywords, matchAllKeywords)
                        if (match != null && seenPaths.add(match.item.path)) {
                            results.add(match)
                            onProgress?.invoke(scannedCount, results.size)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error inspecting ${current.name}: ${e.message}")
                    }
                }
            }
        }

        onProgress?.invoke(scannedCount, results.size)
        results
    }

    private fun isIgnoredDirectory(path: String): Boolean {
        val p = path.lowercase(Locale.ROOT)
        return p.startsWith("/proc") ||
                p.startsWith("/sys") ||
                p.startsWith("/dev") ||
                p.startsWith("/acct") ||
                p.contains("/.thumbnails") ||
                p.contains("/.cache") ||
                p.contains("/android/data/com.google.android.gms") ||
                p.contains("/android/data/com.google.android.googlequicksearchbox")
    }

    /**
     * Inspects a single file against the keywords (in file name, text content, docx/xlsx text, contacts VCF, or PDF pages).
     */
    fun inspectFileForKeywords(
        file: File,
        keywords: List<String>,
        matchAllKeywords: Boolean = false
    ): SearchMatchResult? {
        if (!file.exists() || !file.canRead() || file.isDirectory) return null
        val fileNameLower = file.name.lowercase(Locale.ROOT)
        val category = FileUtil.getFileCategory(file)
        val ext = file.extension.lowercase(Locale.ROOT)

        val matchedInName = keywords.filter { kw -> isKeywordMatched(fileNameLower, kw) }

        // If matched in filename and meets match requirement
        if (matchAllKeywords && matchedInName.size == keywords.size) {
            val item = createFileSystemItem(file, category)
            return SearchMatchResult(
                item = item,
                matchedKeywords = matchedInName,
                snippet = "Matched in filename: ${file.name}",
                matchType = MatchType.FILENAME
            )
        } else if (!matchAllKeywords && matchedInName.isNotEmpty()) {
            val item = createFileSystemItem(file, category)
            return SearchMatchResult(
                item = item,
                matchedKeywords = matchedInName,
                snippet = "Matched in filename: ${file.name}",
                matchType = MatchType.FILENAME
            )
        }

        // 1. PDF Deep Text Extraction (PDFBox)
        if (category == FileCategory.PDF || ext == "pdf") {
            val (matchedInPdf, snippet) = scanPdfContent(file, keywords)
            val combined = (matchedInName + matchedInPdf).distinct()
            val isMatch = if (matchAllKeywords) combined.size >= keywords.size else combined.isNotEmpty()

            if (isMatch) {
                val item = createFileSystemItem(file, category)
                return SearchMatchResult(
                    item = item,
                    matchedKeywords = combined,
                    snippet = snippet ?: "Matched in PDF document content",
                    matchType = MatchType.DOCUMENT_BODY
                )
            }
        }

        // 2. Contacts VCF Backup Files (.vcf)
        if (ext == "vcf") {
            val (matchedInVcf, snippet) = scanVcfFileContent(file, keywords)
            val combined = (matchedInName + matchedInVcf).distinct()
            val isMatch = if (matchAllKeywords) combined.size >= keywords.size else combined.isNotEmpty()

            if (isMatch) {
                val item = createFileSystemItem(file, category)
                return SearchMatchResult(
                    item = item,
                    matchedKeywords = combined,
                    snippet = snippet ?: "Matched in contact vCard record",
                    matchType = MatchType.CONTENT_TEXT
                )
            }
        }

        // 3. Office Open XML Documents (.docx, .xlsx, .pptx, .odt, .ods)
        if (ext in listOf("docx", "xlsx", "pptx", "odt", "ods")) {
            val (matchedInOffice, snippet) = scanZipXmlDocument(file, keywords)
            val combined = (matchedInName + matchedInOffice).distinct()
            val isMatch = if (matchAllKeywords) combined.size >= keywords.size else combined.isNotEmpty()

            if (isMatch) {
                val item = createFileSystemItem(file, category)
                return SearchMatchResult(
                    item = item,
                    matchedKeywords = combined,
                    snippet = snippet ?: "Matched in document body",
                    matchType = MatchType.DOCUMENT_BODY
                )
            }
        }

        // 4. Text, Code, CSV, TSV, Markdown, JSON, XML, Logs, HTML, RTF
        if (category == FileCategory.TEXT || category == FileCategory.CODE ||
            ext in listOf("txt", "md", "csv", "tsv", "log", "json", "xml", "yml", "yaml", "html", "htm", "rtf", "ini", "conf", "env", "sql", "kt", "java", "py", "js", "ts", "cpp", "c", "h")
        ) {
            val (matchedInContent, snippet) = scanTextFileContent(file, keywords)
            val combined = (matchedInName + matchedInContent).distinct()
            val isMatch = if (matchAllKeywords) combined.size >= keywords.size else combined.isNotEmpty()

            if (isMatch) {
                val item = createFileSystemItem(file, category)
                return SearchMatchResult(
                    item = item,
                    matchedKeywords = combined,
                    snippet = snippet,
                    matchType = MatchType.CONTENT_TEXT
                )
            }
        }

        return null
    }

    private fun createFileSystemItem(file: File, category: FileCategory): FileSystemItem {
        return FileSystemItem(
            file = file,
            name = file.name,
            path = file.absolutePath,
            isDirectory = false,
            size = file.length(),
            formattedSize = FileUtil.formatFileSize(file.length()),
            lastModified = file.lastModified(),
            category = category
        )
    }

    /**
     * Checks if keyword matches target text.
     * Supports:
     * - Case-insensitive substring
     * - Normalized mobile number / digit matching (e.g. "+91 98765-43210" matches "9876543210")
     * - Whitespace-collapsed phrase matching
     */
    fun isKeywordMatched(sourceText: String, keyword: String): Boolean {
        val srcLower = sourceText.lowercase(Locale.ROOT)
        val kwLower = keyword.trim().lowercase(Locale.ROOT)
        if (kwLower.isEmpty()) return false

        // 1. Direct Substring
        if (srcLower.contains(kwLower)) return true

        // 2. Normalized spaces match
        val kwCleanSpaces = kwLower.replace(Regex("\\s+"), " ")
        val srcCleanSpaces = srcLower.replace(Regex("\\s+"), " ")
        if (srcCleanSpaces.contains(kwCleanSpaces)) return true

        // 3. Phone Number / Digits Normalization
        val kwDigits = kwLower.filter { it.isDigit() }
        if (kwDigits.length >= 4) {
            val srcDigits = srcLower.filter { it.isDigit() }
            if (srcDigits.contains(kwDigits)) {
                return true
            }
        }

        return false
    }

    /**
     * Extracts a relevant excerpt snippet containing the keyword.
     */
    fun extractSnippet(sourceText: String, keyword: String, prefix: String? = null): String {
        val kwLower = keyword.trim().lowercase(Locale.ROOT)
        val srcLower = sourceText.lowercase(Locale.ROOT)
        val prefixStr = if (!prefix.isNullOrBlank()) "[$prefix] " else ""

        var idx = srcLower.indexOf(kwLower)

        // If not found directly, check for phone number digits match location
        if (idx < 0) {
            val kwDigits = kwLower.filter { it.isDigit() }
            if (kwDigits.length >= 4) {
                // Approximate regex for digit sequence with intervening punctuation/spaces
                try {
                    val pattern = kwDigits.map { Regex.escape(it.toString()) }.joinToString("[^0-9a-zA-Z]*")
                    val match = Regex(pattern).find(sourceText)
                    if (match != null) {
                        idx = match.range.first
                    }
                } catch (_: Exception) {}
            }
        }

        if (idx >= 0) {
            val start = (idx - 40).coerceAtLeast(0)
            val end = (idx + keyword.length + 55).coerceAtMost(sourceText.length)
            val excerpt = sourceText.substring(start, end)
                .replace("\n", " ")
                .replace("\r", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            val leadingEllipsis = if (start > 0) "..." else ""
            val trailingEllipsis = if (end < sourceText.length) "..." else ""
            return "$prefixStr$leadingEllipsis$excerpt$trailingEllipsis"
        }

        val trimmed = sourceText.take(120)
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return "$prefixStr$trimmed..."
    }

    /**
     * Deep scan for PDF files using PDFBox TextStripper.
     */
    private fun scanPdfContent(file: File, keywords: List<String>): Pair<List<String>, String?> {
        val matched = mutableListOf<String>()
        var foundSnippet: String? = null

        try {
            if (file.length() > 60_000_000) return Pair(emptyList(), null) // Up to 60MB PDFs

            PDDocument.load(file).use { document ->
                // Check metadata
                val info = document.documentInformation
                if (info != null) {
                    val metadata = listOfNotNull(
                        info.title,
                        info.author,
                        info.subject,
                        info.keywords,
                        info.creator,
                        info.producer
                    ).joinToString(" ")

                    for (kw in keywords) {
                        if (isKeywordMatched(metadata, kw) && !matched.contains(kw)) {
                            matched.add(kw)
                            if (foundSnippet == null) {
                                foundSnippet = extractSnippet(metadata, kw, "PDF Info")
                            }
                        }
                    }
                }

                val numPages = document.numberOfPages
                val stripper = PDFTextStripper()
                val maxPages = minOf(numPages, 400) // Scan up to 400 pages per PDF

                for (page in 1..maxPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val pageText = try { stripper.getText(document) } catch (_: Exception) { null } ?: continue

                    for (kw in keywords) {
                        if (isKeywordMatched(pageText, kw) && !matched.contains(kw)) {
                            matched.add(kw)
                            if (foundSnippet == null) {
                                foundSnippet = extractSnippet(pageText, kw, "Page $page")
                            }
                        }
                    }

                    if (matched.size == keywords.size && foundSnippet != null) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "PDFBox scan failed for ${file.name}, trying fallback: ${e.message}")
            return scanPdfRawStreamFallback(file, keywords)
        }

        return Pair(matched, foundSnippet)
    }

    /**
     * Fallback stream scanner for encrypted or corrupted PDFs.
     */
    private fun scanPdfRawStreamFallback(file: File, keywords: List<String>): Pair<List<String>, String?> {
        val matched = mutableListOf<String>()
        var foundSnippet: String? = null

        try {
            val buffer = ByteArray(minOf(file.length().toInt(), 500_000))
            val read = FileInputStream(file).use { it.read(buffer) }
            if (read > 0) {
                val rawString = String(buffer, 0, read, Charsets.ISO_8859_1)
                for (kw in keywords) {
                    if (isKeywordMatched(rawString, kw) && !matched.contains(kw)) {
                        matched.add(kw)
                        if (foundSnippet == null) {
                            foundSnippet = extractSnippet(rawString, kw, "Raw PDF Stream")
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return Pair(matched, foundSnippet)
    }

    /**
     * Dedicated scanner for vCard (.vcf) mobile number & contacts backup files.
     */
    private fun scanVcfFileContent(file: File, keywords: List<String>): Pair<List<String>, String?> {
        val matched = mutableListOf<String>()
        var foundSnippet: String? = null

        try {
            if (file.length() > 25_000_000) return Pair(emptyList(), null)

            var currentContactName = "Contact"
            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                var lineIdx = 0
                for (line in lines) {
                    lineIdx++
                    val trimmed = line.trim()
                    if (trimmed.startsWith("FN:", ignoreCase = true) || trimmed.startsWith("N:", ignoreCase = true)) {
                        currentContactName = trimmed.substringAfter(":").replace(";", " ").trim()
                    }

                    for (kw in keywords) {
                        if (isKeywordMatched(trimmed, kw) && !matched.contains(kw)) {
                            matched.add(kw)
                            if (foundSnippet == null) {
                                foundSnippet = "[$currentContactName] $trimmed"
                            }
                        }
                    }

                    if (matched.size == keywords.size && foundSnippet != null) {
                        break
                    }
                    if (lineIdx > 15000) break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning VCF: ${e.message}")
        }

        return Pair(matched, foundSnippet)
    }

    /**
     * Line-by-line scanner for Text, Markdown, CSV, Code, and Log files.
     */
    private fun scanTextFileContent(file: File, keywords: List<String>): Pair<List<String>, String?> {
        val matched = mutableListOf<String>()
        var foundSnippet: String? = null

        try {
            if (file.length() > 25_000_000) return Pair(emptyList(), null)

            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                var lineIdx = 0
                for (line in lines) {
                    lineIdx++
                    for (kw in keywords) {
                        if (isKeywordMatched(line, kw) && !matched.contains(kw)) {
                            matched.add(kw)
                            if (foundSnippet == null) {
                                foundSnippet = extractSnippet(line, kw, "Line $lineIdx")
                            }
                        }
                    }
                    if (matched.size == keywords.size && foundSnippet != null) {
                        break
                    }
                    if (lineIdx > 10000) break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning text content: ${e.message}")
        }

        return Pair(matched, foundSnippet)
    }

    /**
     * Unzips and scans Office Open XML documents (.docx, .xlsx, .pptx, .odt).
     */
    private fun scanZipXmlDocument(file: File, keywords: List<String>): Pair<List<String>, String?> {
        val matched = mutableListOf<String>()
        var foundSnippet: String? = null

        try {
            if (file.length() > 40_000_000) return Pair(emptyList(), null)

            FileInputStream(file).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name.lowercase(Locale.ROOT)
                        if (name.endsWith(".xml") && (
                                name.contains("document") ||
                                name.contains("sharedstrings") ||
                                name.contains("sheet") ||
                                name.contains("slide") ||
                                name.contains("content")
                            )
                        ) {
                            val buffer = ByteArray(16384)
                            val sb = StringBuilder()
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1 && sb.length < 250_000) {
                                sb.append(String(buffer, 0, read, Charsets.UTF_8))
                            }

                            // Strip XML tags to get clean human text
                            val textOnly = sb.toString().replace(Regex("<[^>]*>"), " ")

                            for (kw in keywords) {
                                if (isKeywordMatched(textOnly, kw) && !matched.contains(kw)) {
                                    matched.add(kw)
                                    if (foundSnippet == null) {
                                        foundSnippet = extractSnippet(textOnly, kw, "Document")
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        if (matched.size == keywords.size && foundSnippet != null) break
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning office doc: ${e.message}")
        }

        return Pair(matched, foundSnippet)
    }
}
