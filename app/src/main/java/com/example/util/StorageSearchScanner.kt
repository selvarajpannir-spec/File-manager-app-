package com.example.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.model.FileSystemItem
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

    /**
     * Splits comma-separated keywords (e.g. "report, 2024, confidential" -> ["report", "2024", "confidential"]).
     */
    fun parseKeywords(query: String): List<String> {
        return query.split(",")
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
    }

    /**
     * Recursively scans storage locations or directory for files matching any or all keywords.
     */
    suspend fun searchStorage(
        context: Context,
        keywords: List<String>,
        rootPath: String? = null,
        matchAllKeywords: Boolean = false,
        onProgress: ((scannedCount: Int, foundCount: Int) -> Unit)? = null
    ): List<SearchMatchResult> = withContext(Dispatchers.IO) {
        if (keywords.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<SearchMatchResult>()
        val rootsToScan = mutableListOf<File>()

        if (!rootPath.isNullOrBlank()) {
            val root = File(rootPath)
            if (root.exists()) rootsToScan.add(root)
        } else {
            // Add primary storage roots
            val external = Environment.getExternalStorageDirectory()
            if (external != null && external.exists()) rootsToScan.add(external)

            val internal = context.filesDir
            if (internal.exists()) rootsToScan.add(internal)
        }

        var scannedCount = 0
        val maxFilesToScan = 2500 // Prevent infinite loop on deep Android root

        for (root in rootsToScan) {
            val stack = ArrayDeque<File>()
            stack.add(root)

            while (stack.isNotEmpty() && scannedCount < maxFilesToScan) {
                val current = stack.removeFirst()
                scannedCount++

                if (scannedCount % 20 == 0) {
                    onProgress?.invoke(scannedCount, results.size)
                }

                if (current.isDirectory) {
                    // Skip inaccessible or virtual kernel mounts during broad scan
                    val path = current.absolutePath
                    if (path.startsWith("/proc") || path.startsWith("/sys") || path.startsWith("/dev") || path.startsWith("/acct")) {
                        continue
                    }

                    val children = current.listFiles()
                    if (children != null) {
                        for (child in children) {
                            stack.add(child)
                        }
                    }
                } else {
                    // File matching inspection
                    val match = inspectFileForKeywords(current, keywords, matchAllKeywords)
                    if (match != null) {
                        results.add(match)
                    }
                }
            }
        }

        onProgress?.invoke(scannedCount, results.size)
        results
    }

    /**
     * Inspects a single file against the keywords (in file name, text content, docx/xlsx text, or PDF streams).
     */
    fun inspectFileForKeywords(
        file: File,
        keywords: List<String>,
        matchAllKeywords: Boolean = false
    ): SearchMatchResult? {
        if (!file.exists() || !file.canRead()) return null
        val fileNameLower = file.name.lowercase(Locale.ROOT)
        val category = FileUtil.getFileCategory(file)

        val matchedInName = keywords.filter { fileNameLower.contains(it) }

        // If matched in name and matches condition
        if (matchAllKeywords && matchedInName.size == keywords.size) {
            val item = FileSystemItem(
                file = file,
                name = file.name,
                path = file.absolutePath,
                isDirectory = false,
                size = file.length(),
                formattedSize = FileUtil.formatFileSize(file.length()),
                lastModified = file.lastModified(),
                category = category
            )
            return SearchMatchResult(
                item = item,
                matchedKeywords = matchedInName,
                snippet = "Matched in filename: ${file.name}",
                matchType = MatchType.FILENAME
            )
        } else if (!matchAllKeywords && matchedInName.isNotEmpty()) {
            val item = FileSystemItem(
                file = file,
                name = file.name,
                path = file.absolutePath,
                isDirectory = false,
                size = file.length(),
                formattedSize = FileUtil.formatFileSize(file.length()),
                lastModified = file.lastModified(),
                category = category
            )
            return SearchMatchResult(
                item = item,
                matchedKeywords = matchedInName,
                snippet = "Matched in filename: ${file.name}",
                matchType = MatchType.FILENAME
            )
        }

        // Search Content in Text, Code, Markdown, CSV, XML, JSON, Configs
        if (category == FileCategory.TEXT || category == FileCategory.CODE || file.name.endsWith(".log") || file.name.endsWith(".csv")) {
            val (matchedInContent, snippet) = scanTextFileContent(file, keywords)
            val combined = (matchedInName + matchedInContent).distinct()
            val isMatch = if (matchAllKeywords) combined.size >= keywords.size else combined.isNotEmpty()

            if (isMatch) {
                val item = FileSystemItem(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = false,
                    size = file.length(),
                    formattedSize = FileUtil.formatFileSize(file.length()),
                    lastModified = file.lastModified(),
                    category = category
                )
                return SearchMatchResult(
                    item = item,
                    matchedKeywords = combined,
                    snippet = snippet,
                    matchType = MatchType.CONTENT_TEXT
                )
            }
        }

        // Search Content in Office Open XML Documents (.docx, .xlsx, .pptx)
        val ext = file.extension.lowercase(Locale.ROOT)
        if (ext in listOf("docx", "xlsx", "pptx", "odt", "ods")) {
            val (matchedInOffice, snippet) = scanZipXmlDocument(file, keywords)
            val combined = (matchedInName + matchedInOffice).distinct()
            val isMatch = if (matchAllKeywords) combined.size >= keywords.size else combined.isNotEmpty()

            if (isMatch) {
                val item = FileSystemItem(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = false,
                    size = file.length(),
                    formattedSize = FileUtil.formatFileSize(file.length()),
                    lastModified = file.lastModified(),
                    category = category
                )
                return SearchMatchResult(
                    item = item,
                    matchedKeywords = combined,
                    snippet = snippet ?: "Matched in document body",
                    matchType = MatchType.DOCUMENT_BODY
                )
            }
        }

        // Search Content in PDF files (raw stream/text scanning)
        if (category == FileCategory.PDF) {
            val (matchedInPdf, snippet) = scanPdfContent(file, keywords)
            val combined = (matchedInName + matchedInPdf).distinct()
            val isMatch = if (matchAllKeywords) combined.size >= keywords.size else combined.isNotEmpty()

            if (isMatch) {
                val item = FileSystemItem(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = false,
                    size = file.length(),
                    formattedSize = FileUtil.formatFileSize(file.length()),
                    lastModified = file.lastModified(),
                    category = category
                )
                return SearchMatchResult(
                    item = item,
                    matchedKeywords = combined,
                    snippet = snippet ?: "Matched in PDF document content",
                    matchType = MatchType.DOCUMENT_BODY
                )
            }
        }

        return null
    }

    private fun scanTextFileContent(file: File, keywords: List<String>): Pair<List<String>, String?> {
        val matched = mutableListOf<String>()
        var foundSnippet: String? = null

        try {
            if (file.length() > 20_000_000) return Pair(emptyList(), null) // Skip files > 20MB for fast response

            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                var lineIdx = 0
                for (line in lines) {
                    lineIdx++
                    val lineLower = line.lowercase(Locale.ROOT)
                    for (kw in keywords) {
                        if (lineLower.contains(kw) && !matched.contains(kw)) {
                            matched.add(kw)
                            if (foundSnippet == null) {
                                val trimmed = line.trim()
                                foundSnippet = "Line $lineIdx: ${if (trimmed.length > 90) trimmed.take(90) + "..." else trimmed}"
                            }
                        }
                    }
                    if (matched.size == keywords.size && foundSnippet != null) {
                        break
                    }
                    if (lineIdx > 1500) break
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning text content: ${e.message}")
        }

        return Pair(matched, foundSnippet)
    }

    private fun scanZipXmlDocument(file: File, keywords: List<String>): Pair<List<String>, String?> {
        val matched = mutableListOf<String>()
        var foundSnippet: String? = null

        try {
            if (file.length() > 30_000_000) return Pair(emptyList(), null)

            FileInputStream(file).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name.lowercase(Locale.ROOT)
                        if (name.endsWith(".xml") && (name.contains("document") || name.contains("sharedstrings") || name.contains("slide") || name.contains("content"))) {
                            val buffer = ByteArray(8192)
                            val sb = StringBuilder()
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1 && sb.length < 50_000) {
                                sb.append(String(buffer, 0, read, Charsets.UTF_8))
                            }

                            // Strip XML tags to get raw text
                            val textOnly = sb.toString().replace(Regex("<[^>]*>"), " ")
                            val textLower = textOnly.lowercase(Locale.ROOT)

                            for (kw in keywords) {
                                if (textLower.contains(kw) && !matched.contains(kw)) {
                                    matched.add(kw)
                                    if (foundSnippet == null) {
                                        val idx = textLower.indexOf(kw)
                                        val start = (idx - 30).coerceAtLeast(0)
                                        val end = (idx + kw.length + 40).coerceAtMost(textOnly.length)
                                        foundSnippet = "..." + textOnly.substring(start, end).trim() + "..."
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        if (matched.size == keywords.size) break
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning office doc: ${e.message}")
        }

        return Pair(matched, foundSnippet)
    }

    private fun scanPdfContent(file: File, keywords: List<String>): Pair<List<String>, String?> {
        val matched = mutableListOf<String>()
        var foundSnippet: String? = null

        try {
            if (file.length() > 15_000_000) return Pair(emptyList(), null)

            // Read raw buffer and extract ASCII text sequences
            val buffer = ByteArray(minOf(file.length().toInt(), 100_000))
            val read = FileInputStream(file).use { it.read(buffer) }
            if (read > 0) {
                val rawString = String(buffer, 0, read, Charsets.ISO_8859_1).lowercase(Locale.ROOT)
                for (kw in keywords) {
                    if (rawString.contains(kw) && !matched.contains(kw)) {
                        matched.add(kw)
                        if (foundSnippet == null) {
                            val idx = rawString.indexOf(kw)
                            val start = (idx - 25).coerceAtLeast(0)
                            val end = (idx + kw.length + 35).coerceAtMost(rawString.length)
                            val rawPart = rawString.substring(start, end).filter { it in ' '..'~' }
                            foundSnippet = "... $rawPart ..."
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning PDF: ${e.message}")
        }

        return Pair(matched, foundSnippet)
    }
}
