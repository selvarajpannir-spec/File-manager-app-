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
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun getMimeTypeCategory(fileName: String, mimeType: String?): FileCategory {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val lowerMime = mimeType?.lowercase(Locale.ROOT) ?: ""

        return when {
            lowerMime.contains("pdf") || extension == "pdf" -> FileCategory.PDF
            lowerMime.startsWith("image/") || extension in listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp") -> FileCategory.IMAGE
            lowerMime.startsWith("video/") || extension in listOf("mp4", "mkv", "webm", "avi", "mov") -> FileCategory.VIDEO
            lowerMime.startsWith("audio/") || extension in listOf("mp3", "wav", "ogg", "m4a", "flac", "aac") -> FileCategory.AUDIO
            lowerMime.contains("text") || extension in listOf("txt", "md", "json", "xml", "kt", "java", "py", "html", "css", "js", "ts", "csv", "log") -> FileCategory.TEXT
            extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx") -> FileCategory.DOCUMENT
            extension in listOf("zip", "rar", "7z", "tar", "gz") -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }

    fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
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

    fun extractTextPreview(context: Context, uri: Uri, maxChars: Int = 4000): String {
        return try {
            val inputStream: InputStream? = if (uri.scheme == "file") {
                File(uri.path ?: "").inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }
            inputStream?.bufferedReader()?.use { reader ->
                val buffer = CharArray(maxChars)
                val read = reader.read(buffer, 0, maxChars)
                if (read > 0) String(buffer, 0, read) else ""
            } ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract text: ${e.message}")
            ""
        }
    }

    fun extractAudioMetadata(context: Context, uri: Uri): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
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

    fun generateSamplePdf(context: Context, fileName: String, title: String, content: String): File {
        val file = File(context.filesDir, fileName)
        if (file.exists()) return file

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val paintSub = Paint().apply {
            color = Color.parseColor("#4F46E5")
            textSize = 13f
            isAntiAlias = true
        }

        val paintBody = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 12f
            isAntiAlias = true
        }

        val paintBg = Paint().apply {
            color = Color.parseColor("#F8FAFC")
        }
        canvas.drawRect(0f, 0f, 595f, 842f, paintBg)

        // Draw header bar
        val paintHeader = Paint().apply {
            color = Color.parseColor("#4338CA")
        }
        canvas.drawRect(0f, 0f, 595f, 80f, paintHeader)

        val paintWhite = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(title, 40f, 48f, paintWhite)

        var y = 120f
        canvas.drawText("Document Summary & Technical Scope", 40f, y, paintTitle)
        y += 24f
        canvas.drawText("Generated for Scoped Storage & Tag Search Indexing", 40f, y, paintSub)
        y += 40f

        val lines = content.split("\n")
        for (line in lines) {
            canvas.drawText(line, 40f, y, paintBody)
            y += 20f
            if (y > 800f) break
        }

        pdfDocument.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    fun createSampleTextFile(context: Context, fileName: String, content: String): File {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            file.writeText(content)
        }
        return file
    }
}

enum class FileCategory {
    PDF,
    IMAGE,
    VIDEO,
    AUDIO,
    TEXT,
    DOCUMENT,
    ARCHIVE,
    OTHER
}

data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long = 0L
)
