package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Log
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.math.log10
import kotlin.math.pow

object FileUtil {

    private const val TAG = "FileUtil"

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[digitGroups]
    }

    fun formatDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        if (diff < 60_000) return "Just now"
        if (diff < 3600_000) return "${diff / 60_000}m ago"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun getFileCategory(file: File): FileCategory {
        if (file.isDirectory) return FileCategory.FOLDER
        val name = file.name
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val path = file.absolutePath.lowercase(Locale.ROOT)

        return when {
            extension == "pdf" -> FileCategory.PDF
            extension in listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp", "ico", "heic") -> FileCategory.IMAGE
            extension in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "wmv") -> FileCategory.VIDEO
            extension in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "opus") -> FileCategory.AUDIO
            extension in listOf(
                "kt", "java", "py", "sh", "c", "cpp", "h", "hpp", "html", "css", "js", "ts",
                "json", "xml", "gradle", "properties", "yaml", "yml", "rc", "prop", "conf",
                "sql", "rs", "go", "php", "swift", "rb", "bat", "cmd", "env"
            ) -> FileCategory.CODE
            extension in listOf("txt", "md", "log", "csv", "tsv", "nfo", "ini") -> FileCategory.TEXT
            extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf") -> FileCategory.DOCUMENT
            extension in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso") -> FileCategory.ARCHIVE
            extension in listOf("so", "bin", "dat", "dex", "oat", "elf", "img", "apk", "jar") ||
                    path.contains("/bin/") || path.contains("/lib/") || path.contains("/system/") -> FileCategory.SYSTEM_BINARY
            else -> FileCategory.OTHER
        }
    }

    fun getMimeTypeCategory(fileName: String, mimeType: String?): FileCategory {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val lowerMime = mimeType?.lowercase(Locale.ROOT) ?: ""

        return when {
            lowerMime.contains("pdf") || extension == "pdf" -> FileCategory.PDF
            lowerMime.startsWith("image/") || extension in listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp", "ico") -> FileCategory.IMAGE
            lowerMime.startsWith("video/") || extension in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp") -> FileCategory.VIDEO
            lowerMime.startsWith("audio/") || extension in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "opus") -> FileCategory.AUDIO
            extension in listOf("kt", "java", "py", "sh", "c", "cpp", "h", "html", "css", "js", "ts", "json", "xml", "gradle", "properties", "yaml", "yml", "rc", "prop", "conf", "sql", "rs", "go") -> FileCategory.CODE
            lowerMime.contains("text") || extension in listOf("txt", "md", "log", "csv", "ini", "env") -> FileCategory.TEXT
            extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx") -> FileCategory.DOCUMENT
            extension in listOf("zip", "rar", "7z", "tar", "gz") -> FileCategory.ARCHIVE
            extension in listOf("so", "bin", "dat", "dex", "apk") -> FileCategory.SYSTEM_BINARY
            else -> FileCategory.OTHER
        }
    }

    fun isSystemPath(path: String): Boolean {
        val p = path.lowercase(Locale.ROOT)
        return p.startsWith("/system") || p.startsWith("/etc") || p.startsWith("/proc") ||
                p.startsWith("/sys") || p.startsWith("/dev") || p.startsWith("/apex") ||
                p.startsWith("/vendor") || p.startsWith("/odm") || p.startsWith("/product") ||
                p.startsWith("/system_ext") || p.startsWith("/init") || File(path).name.startsWith(".")
    }

    fun getPermissionsString(file: File): String {
        return buildString {
            append(if (file.isDirectory) "d" else "-")
            append(if (file.canRead()) "r" else "-")
            append(if (file.canWrite()) "w" else "-")
            append(if (file.canExecute()) "x" else "-")
        }
    }

    fun listDirectoryFiles(dir: File, showHidden: Boolean = true): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val files = try { dir.listFiles() } catch (_: Throwable) { null } ?: return emptyList()
        return files.filter { file ->
            if (!showHidden && file.name.startsWith(".")) false else true
        }.toList()
    }

    fun getAvailableStorageLocations(context: Context): List<StorageLocation> {
        val list = mutableListOf<StorageLocation>()

        // 1. Primary Internal Storage
        val primaryExternal = Environment.getExternalStorageDirectory()
        if (primaryExternal != null && primaryExternal.exists()) {
            list.add(
                StorageLocation(
                    title = "Internal Storage",
                    path = primaryExternal.absolutePath,
                    iconEmoji = "📱",
                    isSystem = false
                )
            )
        }

        // 2. Standard User Folders
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads != null && downloads.exists()) {
            list.add(StorageLocation("Downloads", downloads.absolutePath, "📥", isSystem = false))
        }

        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documents != null && documents.exists()) {
            list.add(StorageLocation("Documents", documents.absolutePath, "📄", isSystem = false))
        }

        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (pictures != null && pictures.exists()) {
            list.add(StorageLocation("Pictures & DCIM", pictures.absolutePath, "🖼️", isSystem = false))
        }

        val music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (music != null && music.exists()) {
            list.add(StorageLocation("Music", music.absolutePath, "🎵", isSystem = false))
        }

        val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (movies != null && movies.exists()) {
            list.add(StorageLocation("Movies", movies.absolutePath, "🎬", isSystem = false))
        }

        // 3. System Root and Subdirectories
        val rootDir = File("/")
        if (rootDir.exists()) {
            list.add(StorageLocation("Root (/)", "/", "⚡", isSystem = true))
        }

        val systemDir = File("/system")
        if (systemDir.exists()) {
            list.add(StorageLocation("System (/system)", "/system", "⚙️", isSystem = true))
        }

        val etcDir = File("/etc")
        if (etcDir.exists()) {
            list.add(StorageLocation("Config (/etc)", "/etc", "🛠️", isSystem = true))
        }

        val procDir = File("/proc")
        if (procDir.exists()) {
            list.add(StorageLocation("Processes (/proc)", "/proc", "🧠", isSystem = true))
        }

        val sysDir = File("/sys")
        if (sysDir.exists()) {
            list.add(StorageLocation("Hardware (/sys)", "/sys", "🔌", isSystem = true))
        }

        val appInternal = context.filesDir
        if (appInternal.exists()) {
            list.add(StorageLocation("App Storage", appInternal.absolutePath, "📦", isSystem = false))
        }

        return list
    }

    fun getStorageStats(): StorageStats {
        return try {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes
            val usedPercentage = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0

            StorageStats(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                usedPercentage = usedPercentage
            )
        } catch (e: Exception) {
            StorageStats(0, 0, 0, 0)
        }
    }

    /**
     * Safely renders a PDF page to a Bitmap using the system PdfRenderer.
     */
    fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int = 0): PdfRenderResult {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (!file.exists() || !file.canRead()) return PdfRenderResult(null, 0, 0, "File not accessible")
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }

            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                if (pageCount > 0) {
                    val safeIndex = pageIndex.coerceIn(0, pageCount - 1)
                    page = renderer.openPage(safeIndex)
                    val width = (page.width * 1.5f).toInt().coerceIn(200, 2048)
                    val height = (page.height * 1.5f).toInt().coerceIn(200, 2048)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    PdfRenderResult(bitmap, pageCount, safeIndex)
                } else {
                    PdfRenderResult(null, 0, 0, "Empty PDF document")
                }
            } else {
                PdfRenderResult(null, 0, 0, "Cannot open PDF descriptor")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to render PDF page: ${t.message}")
            PdfRenderResult(null, 0, 0, t.message ?: "Failed to render PDF")
        } finally {
            try { page?.close() } catch (_: Throwable) {}
            try { renderer?.close() } catch (_: Throwable) {}
            try { pfd?.close() } catch (_: Throwable) {}
        }
    }

    fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
        return renderPdfPage(context, uri, 0).bitmap
    }

    fun extractTextPreview(context: Context, uri: Uri, maxChars: Int = 10000): String {
        return try {
            val inputStream: InputStream? = if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists() && f.canRead()) f.inputStream() else null
            } else {
                context.contentResolver.openInputStream(uri)
            }
            inputStream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buffer = CharArray(maxChars)
                val read = reader.read(buffer, 0, maxChars)
                if (read > 0) String(buffer, 0, read) else ""
            } ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract text: ${e.message}")
            ""
        }
    }

    fun readTextFileLines(file: File, maxLines: Int = 400, maxChars: Int = 20000): String {
        return try {
            if (!file.exists() || !file.canRead()) return "Unable to read file: Permission denied or file inaccessible."
            val builder = StringBuilder()
            var lineCount = 0
            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    builder.append(line).append("\n")
                    lineCount++
                    if (lineCount >= maxLines || builder.length >= maxChars) {
                        builder.append("\n... [Preview truncated: file has more content] ...")
                        break
                    }
                }
            }
            builder.toString()
        } catch (e: Exception) {
            "Unable to read file content (${e.javaClass.simpleName}: ${e.message})"
        }
    }

    fun generateHexDump(file: File, maxBytes: Int = 1024): String {
        return try {
            if (!file.exists() || !file.canRead()) return "Permission denied or binary inaccessible"
            val buffer = ByteArray(maxBytes)
            val read = FileInputStream(file).use { it.read(buffer) }
            if (read <= 0) return "Empty binary file (0 bytes)"

            val sb = StringBuilder()
            sb.append("Offset(h)  -- Hex Values ------------------------  ASCII\n")
            sb.append("----------------------------------------------------------\n")

            for (i in 0 until read step 16) {
                sb.append(String.format(Locale.ROOT, "%08X: ", i))
                val hexPart = StringBuilder()
                val asciiPart = StringBuilder()

                for (j in 0 until 16) {
                    val idx = i + j
                    if (idx < read) {
                        val b = buffer[idx]
                        hexPart.append(String.format(Locale.ROOT, "%02X ", b))
                        val c = b.toInt().toChar()
                        asciiPart.append(if (c in ' '..'~') c else '.')
                    } else {
                        hexPart.append("   ")
                    }
                    if (j == 7) hexPart.append(" ")
                }

                sb.append(hexPart.toString()).append(" |").append(asciiPart.toString()).append("|\n")
            }

            if (file.length() > maxBytes) {
                sb.append("\n... Showing first $maxBytes of ${formatFileSize(file.length())} ...")
            }
            sb.toString()
        } catch (e: Exception) {
            "Hex dump error: ${e.message}"
        }
    }

    fun calculateFileChecksum(file: File, algorithm: String = "MD5"): String {
        return try {
            if (!file.exists() || !file.canRead() || file.isDirectory) return "N/A"
            val md = MessageDigest.getInstance(algorithm)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            val digest = md.digest()
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error calculating checksum"
        }
    }

    fun extractAudioMetadata(context: Context, file: File): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            if (file.exists() && file.canRead()) {
                retriever.setDataSource(file.absolutePath)
            } else {
                return AudioMetadata()
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            AudioMetadata(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs
            )
        } catch (e: Exception) {
            AudioMetadata()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun extractAudioMetadata(context: Context, uri: Uri): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists() && f.canRead()) {
                    retriever.setDataSource(f.absolutePath)
                } else {
                    return AudioMetadata()
                }
            } else {
                retriever.setDataSource(context, uri)
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            AudioMetadata(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs
            )
        } catch (e: Exception) {
            AudioMetadata()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun getUriMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L

        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) name = it.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = it.getLong(sizeIndex)
                }
            }
        } else if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            if (file.exists()) {
                name = file.name
                size = file.length()
            }
        }
        return Pair(name, size)
    }

    /**
     * Safely extracts text from PDF files using PDFBox Android with comprehensive error recovery.
     */
    fun extractPdfText(context: Context? = null, file: File, maxPages: Int = 15): String? {
        return try {
            if (context != null) {
                try {
                    PDFBoxResourceLoader.init(context)
                } catch (_: Throwable) {}
            }
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                val pages = minOf(doc.numberOfPages, maxPages)
                stripper.startPage = 1
                stripper.endPage = pages
                stripper.getText(doc)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "extractPdfText safely handled exception: ${t.message}")
            null
        }
    }

    fun readOfficeDocxText(file: File): String? {
        val paragraphs = readOfficeDocxParagraphs(file)
        return if (paragraphs.isNotEmpty()) paragraphs.joinToString("\n\n") else null
    }

    /**
     * Robust XmlPullParser-based DOCX parser that cleanly extracts text without raw XML markup.
     */
    fun readOfficeDocxParagraphs(file: File): List<String> {
        val paragraphs = mutableListOf<String>()
        try {
            ZipInputStream(FileInputStream(file).buffered()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name.lowercase(Locale.ROOT)
                    if (entryName == "word/document.xml" || entryName.endsWith("/document.xml")) {
                        val parser = Xml.newPullParser()
                        parser.setInput(InputStreamReader(zis, Charsets.UTF_8))
                        var eventType = parser.eventType
                        var currentPara = StringBuilder()
                        var insidePara = false

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            val tag = parser.name?.lowercase(Locale.ROOT) ?: ""
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    when (tag) {
                                        "p" -> {
                                            insidePara = true
                                            currentPara = StringBuilder()
                                        }
                                        "tab" -> {
                                            if (insidePara) currentPara.append("    ")
                                        }
                                        "br", "cr" -> {
                                            if (insidePara) currentPara.append("\n")
                                        }
                                        "t" -> {
                                            val text = parser.nextText()
                                            if (insidePara && text.isNotEmpty()) {
                                                currentPara.append(text)
                                            }
                                        }
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    when (tag) {
                                        "p", "tr" -> {
                                            val line = currentPara.toString().trim()
                                            if (line.isNotEmpty()) {
                                                paragraphs.add(line)
                                            }
                                            currentPara = StringBuilder()
                                            insidePara = false
                                        }
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        val remaining = currentPara.toString().trim()
                        if (remaining.isNotEmpty()) {
                            paragraphs.add(remaining)
                        }
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error parsing docx via XmlPullParser: ${t.message}")
        }
        return paragraphs
    }

    /**
     * Parses PowerPoint (.pptx) slides into structured text.
     */
    fun readOfficePptxParagraphs(file: File): List<String> {
        val paragraphs = mutableListOf<String>()
        try {
            ZipInputStream(FileInputStream(file).buffered()).use { zis ->
                var entry = zis.nextEntry
                var slideIndex = 1
                while (entry != null) {
                    val name = entry.name.lowercase(Locale.ROOT)
                    if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                        val parser = Xml.newPullParser()
                        parser.setInput(InputStreamReader(zis, Charsets.UTF_8))
                        var eventType = parser.eventType
                        var slideText = StringBuilder()
                        slideText.append("Slide $slideIndex:\n")

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            val tag = parser.name?.lowercase(Locale.ROOT) ?: ""
                            if (eventType == XmlPullParser.START_TAG && tag == "t") {
                                val t = parser.nextText()
                                if (t.isNotBlank()) {
                                    slideText.append(t).append(" ")
                                }
                            }
                            eventType = parser.next()
                        }
                        val result = slideText.toString().trim()
                        if (result.length > "Slide $slideIndex:".length) {
                            paragraphs.add(result)
                            slideIndex++
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error parsing pptx: ${t.message}")
        }
        return paragraphs
    }

    /**
     * Parses an Excel (.xlsx) spreadsheet into a 2D table grid (List of Rows).
     */
    fun readOfficeXlsxTable(file: File, maxRows: Int = 100, maxCols: Int = 26): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val sharedStrings = mutableListOf<String>()
            var sheetXml: String? = null

            // Pass 1: Extract shared strings and primary sheet
            val zis1 = ZipInputStream(FileInputStream(file).buffered())
            var entry1: ZipEntry? = zis1.nextEntry
            while (entry1 != null) {
                val name = entry1.name.lowercase(Locale.ROOT)
                if (name.endsWith("sharedstrings.xml")) {
                    val parser = Xml.newPullParser()
                    parser.setInput(InputStreamReader(zis1, Charsets.UTF_8))
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val tag = parser.name?.lowercase(Locale.ROOT) ?: ""
                        if (eventType == XmlPullParser.START_TAG && tag == "t") {
                            sharedStrings.add(parser.nextText())
                        }
                        eventType = parser.next()
                    }
                } else if (sheetXml == null && (name.contains("worksheets/sheet1.xml") || (name.contains("sheet") && name.endsWith(".xml")))) {
                    sheetXml = zis1.reader(Charsets.UTF_8).readText()
                }
                zis1.closeEntry()
                entry1 = zis1.nextEntry
            }
            zis1.close()

            if (sheetXml != null) {
                val rowRegex = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
                val rowMatches = rowRegex.findAll(sheetXml)

                for (rMatch in rowMatches.take(maxRows)) {
                    val rContent = rMatch.value
                    val cRegex = Regex("<c r=\"([A-Z]+)[0-9]+\"(?:[^>]*?t=\"([a-z]+)\")?[^>]*>(?:<v>(.*?)</v>)?(?:<is><t>(.*?)</t></is>)?</c>", RegexOption.DOT_MATCHES_ALL)
                    val cMatches = cRegex.findAll(rContent).toList()

                    val rowCells = mutableListOf<String>()
                    var lastColIdx = -1

                    for (cMatch in cMatches.take(maxCols)) {
                        val colLetters = cMatch.groupValues[1]
                        val cellType = cMatch.groupValues[2]
                        val valContent = cMatch.groupValues[3]
                        val inlineText = cMatch.groupValues[4]

                        var colIdx = 0
                        for (ch in colLetters) {
                            colIdx = colIdx * 26 + (ch - 'A' + 1)
                        }
                        colIdx -= 1

                        while (lastColIdx + 1 < colIdx && rowCells.size < maxCols) {
                            rowCells.add("")
                            lastColIdx++
                        }

                        val cellValue = when {
                            inlineText.isNotEmpty() -> inlineText
                            cellType == "s" -> {
                                val idx = valContent.toIntOrNull() ?: -1
                                if (idx in 0 until sharedStrings.size) sharedStrings[idx] else valContent
                            }
                            cellType == "b" -> if (valContent == "1") "TRUE" else "FALSE"
                            else -> valContent
                        }

                        rowCells.add(cellValue.trim())
                        lastColIdx = colIdx
                    }

                    if (rowCells.any { it.isNotBlank() }) {
                        rows.add(rowCells)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error parsing xlsx table: ${t.message}")
        }
        return rows
    }

    /**
     * Parses a CSV or TSV file into a 2D table grid.
     */
    fun readCsvTable(file: File, maxRows: Int = 100): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val delimiter = if (file.extension.equals("tsv", ignoreCase = true)) "\t" else ","
            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines.take(maxRows)) {
                    if (line.isNotBlank()) {
                        val cells = line.split(delimiter).map { it.trim().removeSurrounding("\"") }
                        rows.add(cells)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing csv: ${e.message}")
        }
        return rows
    }

    /**
     * Filters a list of roots to only include top-level disjoint ancestor directories.
     */
    fun getTopLevelRoots(rootPaths: List<String>): List<String> {
        val existing = rootPaths.map { File(it) }.filter { it.exists() }
        val result = mutableListOf<File>()
        for (f in existing) {
            val fPath = f.absolutePath
            val hasAncestor = existing.any { other ->
                other != f && fPath.startsWith(other.absolutePath.trimEnd('/') + "/")
            }
            if (!hasAncestor) {
                result.add(f)
            }
        }
        return result.map { it.absolutePath }.distinct()
    }

    /**
     * Scans storage locations for files matching a specific DocumentTypeFilter.
     */
    fun scanFilesByFilter(
        rootPaths: List<String>,
        filter: DocumentTypeFilter,
        maxResults: Int = 1000,
        searchQuery: String = ""
    ): List<File> {
        val results = mutableListOf<File>()
        val seenPaths = HashSet<String>()
        val queryLower = searchQuery.trim().lowercase(Locale.ROOT)
        val extSet = filter.extensions.map { it.lowercase(Locale.ROOT) }.toSet()
        val topRoots = getTopLevelRoots(rootPaths)

        for (rootPath in topRoots) {
            val root = File(rootPath)
            if (!root.exists()) continue

            val queue = ArrayDeque<File>()
            val visitedDirs = HashSet<String>()
            queue.add(root)
            try { visitedDirs.add(root.canonicalPath) } catch (_: Throwable) { visitedDirs.add(root.absolutePath) }

            while (queue.isNotEmpty() && results.size < maxResults) {
                val currentDir = queue.removeFirst()
                val files = try { currentDir.listFiles() } catch (_: Throwable) { null } ?: continue

                for (f in files) {
                    try {
                        val name = f.name
                        if (name.startsWith(".")) continue

                        if (f.isDirectory) {
                            val cPath = try { f.canonicalPath } catch (_: Throwable) { f.absolutePath }
                            if (!visitedDirs.contains(cPath) &&
                                !isSystemPath(f.absolutePath) &&
                                !name.equals("Android", ignoreCase = true) &&
                                !name.equals("cache", ignoreCase = true) &&
                                !name.equals(".thumbnails", ignoreCase = true) &&
                                !name.equals(".trashed", ignoreCase = true)
                            ) {
                                visitedDirs.add(cPath)
                                queue.add(f)
                            }
                        } else if (f.isFile) {
                            val ext = f.extension.lowercase(Locale.ROOT)
                            if (ext in extSet) {
                                val fPath = f.absolutePath
                                if (!seenPaths.contains(fPath)) {
                                    if (queryLower.isEmpty() || name.lowercase(Locale.ROOT).contains(queryLower)) {
                                        seenPaths.add(fPath)
                                        results.add(f)
                                        if (results.size >= maxResults) break
                                    }
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }
            }
        }
        return results
    }

    /**
     * Fast recursive filename search across entire storage tree.
     */
    fun searchFilesByNameAcrossStorage(
        rootPaths: List<String>,
        query: String,
        maxResults: Int = 500
    ): List<File> {
        val results = mutableListOf<File>()
        val seenPaths = HashSet<String>()
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isEmpty()) return emptyList()
        val topRoots = getTopLevelRoots(rootPaths)

        for (rootPath in topRoots) {
            val root = File(rootPath)
            if (!root.exists()) continue

            val queue = ArrayDeque<File>()
            val visitedDirs = HashSet<String>()
            queue.add(root)
            try { visitedDirs.add(root.canonicalPath) } catch (_: Throwable) { visitedDirs.add(root.absolutePath) }

            while (queue.isNotEmpty() && results.size < maxResults) {
                val currentDir = queue.removeFirst()
                val files = try { currentDir.listFiles() } catch (_: Throwable) { null } ?: continue

                for (f in files) {
                    try {
                        val name = f.name
                        if (name.startsWith(".")) continue

                        val fPath = f.absolutePath
                        if (!seenPaths.contains(fPath) && name.lowercase(Locale.ROOT).contains(cleanQuery)) {
                            seenPaths.add(fPath)
                            results.add(f)
                            if (results.size >= maxResults) break
                        }

                        if (f.isDirectory) {
                            val cPath = try { f.canonicalPath } catch (_: Throwable) { f.absolutePath }
                            if (!visitedDirs.contains(cPath) &&
                                !isSystemPath(f.absolutePath) &&
                                !name.equals("Android", ignoreCase = true) &&
                                !name.equals("cache", ignoreCase = true) &&
                                !name.equals(".thumbnails", ignoreCase = true) &&
                                !name.equals(".trashed", ignoreCase = true)
                            ) {
                                visitedDirs.add(cPath)
                                queue.add(f)
                            }
                        }
                    } catch (_: Throwable) {}
                }
            }
        }
        return results
    }
}

data class PdfRenderResult(
    val bitmap: Bitmap?,
    val pageCount: Int,
    val currentPage: Int,
    val error: String? = null
)

enum class DocumentTypeFilter(
    val title: String,
    val iconEmoji: String,
    val extensions: List<String>,
    val description: String
) {
    ALL_PDF("PDF Documents", "📄", listOf("pdf"), "All .pdf files across storage"),
    ALL_WORD("Word Documents", "📝", listOf("docx", "doc", "rtf", "odt", "dotx", "dot"), "All Word & Rich Text docs"),
    ALL_EXCEL("Spreadsheets & Excel", "📊", listOf("xlsx", "xls", "csv", "tsv", "ods", "xltx"), "All Excel, Sheets & CSV tables"),
    ALL_PPT("Presentations & Slides", "📽️", listOf("pptx", "ppt", "odp", "ppsx"), "All PowerPoint & Slide decks"),
    ALL_TEXT("Text & Markdown", "📜", listOf("txt", "md", "log", "ini", "json", "xml", "nfo"), "All plain text, markdown & logs"),
    ALL_CODE("Source Code Files", "💻", listOf("kt", "java", "py", "js", "ts", "html", "css", "c", "cpp", "h", "sql", "sh", "rs", "go"), "All programming source code files"),
    ALL_IMAGES("Photos & Images", "🖼️", listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp", "heic", "ico"), "All pictures and graphics"),
    ALL_AUDIO("Audio & Music", "🎵", listOf("mp3", "wav", "m4a", "aac", "flac", "ogg", "opus", "wma"), "All songs, recordings & audio"),
    ALL_VIDEO("Videos & Movies", "🎬", listOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "flv"), "All video recordings & clips"),
    ALL_ARCHIVES("Archives & ZIPs", "📦", listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "apk"), "All compressed archives and packages")
}

enum class FileCategory {
    FOLDER,
    PDF,
    IMAGE,
    VIDEO,
    AUDIO,
    TEXT,
    CODE,
    DOCUMENT,
    ARCHIVE,
    SYSTEM_BINARY,
    OTHER
}

data class StorageLocation(
    val title: String,
    val path: String,
    val iconEmoji: String,
    val isSystem: Boolean = false
)

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usedPercentage: Int
)

data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L
)
