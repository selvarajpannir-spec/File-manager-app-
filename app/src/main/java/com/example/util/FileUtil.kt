package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
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
        return try {
            val list = dir.listFiles() ?: emptyArray()
            val filtered = if (showHidden) list else list.filter { !it.name.startsWith(".") }.toTypedArray()
            filtered.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
        } catch (e: Exception) {
            Log.w(TAG, "Error reading directory ${dir.absolutePath}: ${e.message}")
            emptyList()
        }
    }

    fun getAvailableStorageLocations(context: Context): List<StorageLocation> {
        val list = mutableListOf<StorageLocation>()

        // 1. Internal Storage / SDCard
        val extStorage = Environment.getExternalStorageDirectory()
        if (extStorage != null && extStorage.exists()) {
            list.add(StorageLocation("Internal Storage", extStorage.absolutePath, "📱", isSystem = false))
        }

        // 2. Standard Public Directories
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads != null && downloads.exists()) {
            list.add(StorageLocation("Downloads", downloads.absolutePath, "📥", isSystem = false))
        }

        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documents != null && documents.exists()) {
            list.add(StorageLocation("Documents", documents.absolutePath, "📄", isSystem = false))
        }

        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (dcim != null && dcim.exists()) {
            list.add(StorageLocation("DCIM / Camera", dcim.absolutePath, "📷", isSystem = false))
        }

        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (pictures != null && pictures.exists()) {
            list.add(StorageLocation("Pictures", pictures.absolutePath, "🖼️", isSystem = false))
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

    fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (!file.exists()) return null
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }

            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0) {
                    page = renderer.openPage(0)
                    val width = (page.width * 1.5f).toInt().coerceAtLeast(200)
                    val height = (page.height * 1.5f).toInt().coerceAtLeast(200)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to render PDF preview: ${e.message}")
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
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
                // Offset
                sb.append(String.format(Locale.ROOT, "%08X: ", i))

                // Hex bytes
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

    fun extractAudioMetadata(context: Context, uri: Uri): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) retriever.setDataSource(f.absolutePath) else return AudioMetadata()
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

    fun extractPdfText(file: File, maxPages: Int = 15): String? {
        return try {
            PDDocument.load(file).use { doc ->
                val stripper = PDFTextStripper()
                val pages = minOf(doc.numberOfPages, maxPages)
                stripper.startPage = 1
                stripper.endPage = pages
                stripper.getText(doc)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun readOfficeDocxText(file: File): String? {
        return try {
            val sb = StringBuilder()
            val zis = ZipInputStream(FileInputStream(file))
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("document.xml") || entry.name.endsWith("sharedStrings.xml") || entry.name.endsWith("content.xml")) {
                    val xml = zis.reader(Charsets.UTF_8).readText()
                    val stripped = xml.replace(Regex("<[^>]*>"), " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    sb.append(stripped).append("\n")
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            if (sb.isNotEmpty()) sb.toString() else null
        } catch (e: Exception) {
            null
        }
    }
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
