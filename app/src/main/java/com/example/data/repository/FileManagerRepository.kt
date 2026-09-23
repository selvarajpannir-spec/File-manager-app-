package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FileContentFTS
import com.example.data.local.entity.FileEntity
import com.example.data.local.entity.FileTagCrossRef
import com.example.data.local.entity.FileWithTags
import com.example.data.local.entity.TagEntity
import com.example.data.model.FileSystemItem
import com.example.util.FileCategory
import com.example.util.FileUtil
import com.example.util.SearchMatchResult
import com.example.util.StorageSearchScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

class FileManagerRepository(
    private val database: AppDatabase
) {
    private val TAG = "FileManagerRepo"
    private val fileDao = database.fileDao()
    private val tagDao = database.tagDao()

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getAllFilesWithTags(): Flow<List<FileWithTags>> = fileDao.getAllFilesWithTags()

    suspend fun getDirectoryItems(
        directoryPath: String,
        showHidden: Boolean = true
    ): List<FileSystemItem> = withContext(Dispatchers.IO) {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) {
            return@withContext emptyList()
        }

        val rawFiles = FileUtil.listDirectoryFiles(dir, showHidden)
        rawFiles.map { file ->
            val dbRecord = fileDao.getFileWithTagsByPath(file.absolutePath)
            FileSystemItem(
                file = file,
                tags = dbRecord?.tags ?: emptyList(),
                dbFileId = dbRecord?.file?.id
            )
        }
    }

    suspend fun getOrInsertFileEntityForPath(file: File): Long = withContext(Dispatchers.IO) {
        val existing = fileDao.getFileByPath(file.absolutePath)
        if (existing != null) return@withContext existing.id

        val mime = if (file.isDirectory) "resource/folder" else "application/octet-stream"
        val category = FileUtil.getFileCategory(file)
        val extractedText = if (category == FileCategory.TEXT || category == FileCategory.CODE) {
            FileUtil.readTextFileLines(file, maxLines = 100, maxChars = 2000)
        } else ""

        val entity = FileEntity(
            fileUri = Uri.fromFile(file).toString(),
            fileName = file.name,
            fileSize = if (file.isDirectory) 0L else file.length(),
            fileType = mime,
            lastModified = file.lastModified(),
            contentText = extractedText,
            filePath = file.absolutePath,
            isSample = false
        )
        val id = fileDao.insertFile(entity)
        fileDao.insertFts(FileContentFTS(rowid = id, fileName = file.name, contentText = extractedText, tags = ""))
        id
    }

    suspend fun toggleTagForFilePath(file: File, tag: TagEntity, isAssigned: Boolean) = withContext(Dispatchers.IO) {
        val fileId = getOrInsertFileEntityForPath(file)
        if (isAssigned) {
            fileDao.deleteFileTagCrossRef(fileId, tag.tagId)
        } else {
            fileDao.insertFileTagCrossRef(FileTagCrossRef(fileId = fileId, tagId = tag.tagId))
        }
    }

    suspend fun createNewTag(name: String, colorHex: String): Long = withContext(Dispatchers.IO) {
        val cleanName = name.trim().removePrefix("#")
        val existing = tagDao.getTagByName(cleanName)
        if (existing != null) {
            existing.tagId
        } else {
            val newTag = TagEntity(tagName = cleanName, colorHex = colorHex)
            tagDao.insertTag(newTag)
        }
    }

    suspend fun deleteTag(tagId: Long) = withContext(Dispatchers.IO) {
        tagDao.deleteTagById(tagId)
    }

    suspend fun createFolder(parentPath: String, folderName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val newDir = File(parentPath, folderName.trim())
            if (!newDir.exists()) newDir.mkdirs() else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create folder: ${e.message}")
            false
        }
    }

    suspend fun createTextFile(parentPath: String, fileName: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val safeName = if (fileName.contains('.')) fileName else "$fileName.txt"
            val newFile = File(parentPath, safeName)
            newFile.writeText(content)
            getOrInsertFileEntityForPath(newFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create text file: ${e.message}")
            false
        }
    }

    suspend fun deleteFileOrFolder(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbRecord = fileDao.getFileByPath(file.absolutePath)
            if (dbRecord != null) {
                fileDao.deleteTagCrossRefsForFile(dbRecord.id)
                fileDao.deleteFts(dbRecord.id)
                fileDao.deleteFileById(dbRecord.id)
            }
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete: ${e.message}")
            false
        }
    }

    suspend fun renameFileOrFolder(file: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = File(file.parentFile, newName.trim())
            val success = file.renameTo(target)
            if (success) {
                val dbRecord = fileDao.getFileByPath(file.absolutePath)
                if (dbRecord != null) {
                    fileDao.updateFile(dbRecord.copy(filePath = target.absolutePath, fileName = target.name))
                }
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename: ${e.message}")
            false
        }
    }

    suspend fun importDocumentUri(uri: Uri, context: Context, targetDirectory: String? = null): Long = withContext(Dispatchers.IO) {
        try {
            val (name, size) = FileUtil.getUriMetadata(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val category = FileUtil.getMimeTypeCategory(name, mimeType)

            val destDir = if (targetDirectory != null) File(targetDirectory) else context.filesDir
            val destFile = File(destDir, name)

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not copy physical file, saving URI reference: ${e.message}")
            }

            val extractedText = when (category) {
                FileCategory.TEXT, FileCategory.CODE -> FileUtil.readTextFileLines(destFile, 100, 2000)
                else -> ""
            }

            val fileEntity = FileEntity(
                fileUri = if (destFile.exists()) Uri.fromFile(destFile).toString() else uri.toString(),
                fileName = name,
                fileSize = if (destFile.exists()) destFile.length() else size,
                fileType = mimeType,
                lastModified = System.currentTimeMillis(),
                contentText = extractedText,
                filePath = if (destFile.exists()) destFile.absolutePath else null,
                isSample = false
            )

            val fileId = fileDao.insertFile(fileEntity)
            fileDao.insertFts(FileContentFTS(rowid = fileId, fileName = name, contentText = extractedText, tags = ""))

            // Auto-tag based on file category
            val autoTagName = when (category) {
                FileCategory.PDF -> "pdf"
                FileCategory.IMAGE -> "image"
                FileCategory.VIDEO -> "video"
                FileCategory.AUDIO -> "audio"
                FileCategory.TEXT -> "text"
                FileCategory.CODE -> "code"
                FileCategory.DOCUMENT -> "document"
                FileCategory.ARCHIVE -> "archive"
                FileCategory.SYSTEM_BINARY -> "binary"
                else -> "file"
            }
            val tagId = createNewTag(autoTagName, getCategoryColorHex(category))
            fileDao.insertFileTagCrossRef(FileTagCrossRef(fileId = fileId, tagId = tagId))

            fileId
        } catch (e: Exception) {
            Log.e(TAG, "Error importing URI: ${e.message}")
            -1L
        }
    }

    /**
     * Deep search for comma-separated keywords across files & content.
     */
    suspend fun searchKeywordsAcrossStorage(
        context: Context,
        keywords: List<String>,
        rootPath: String? = null,
        matchAllKeywords: Boolean = false,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<SearchMatchResult> = withContext(Dispatchers.IO) {
        val matches = StorageSearchScanner.searchStorage(
            context = context,
            keywords = keywords,
            rootPath = rootPath,
            matchAllKeywords = matchAllKeywords,
            onProgress = onProgress
        )

        // Attach existing tags from Room database to matched items
        matches.map { match ->
            val dbRecord = fileDao.getFileWithTagsByPath(match.item.path)
            if (dbRecord != null && dbRecord.tags.isNotEmpty()) {
                match.copy(item = match.item.copy(tags = dbRecord.tags, dbFileId = dbRecord.file.id))
            } else {
                match
            }
        }
    }

    /**
     * Initializes default color tags and cleans up any previously generated sample files.
     */
    suspend fun initializeDefaultTagsAndCleanupSamples(context: Context) = withContext(Dispatchers.IO) {
        // Ensure default useful tags exist
        createNewTag("system", "#EF4444")
        createNewTag("config", "#F59E0B")
        createNewTag("important", "#EC4899")
        createNewTag("media", "#8B5CF6")
        createNewTag("code", "#3B82F6")
        createNewTag("docs", "#10B981")
        createNewTag("work", "#6366F1")
        createNewTag("download", "#0EA5E9")

        // Actively remove all sample files created previously
        try {
            val sampleNotesFile = File(context.filesDir, "system_storage_notes.txt")
            if (sampleNotesFile.exists()) {
                sampleNotesFile.delete()
            }
            val samplePdfFile = File(context.filesDir, "file_manager_guide.pdf")
            if (samplePdfFile.exists()) {
                samplePdfFile.delete()
            }

            // Clean up DB entries for sample files
            val sampleDbFiles = fileDao.getSampleFiles()
            for (sample in sampleDbFiles) {
                fileDao.deleteTagCrossRefsForFile(sample.id)
                fileDao.deleteFts(sample.id)
                fileDao.deleteFileById(sample.id)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup sample files: ${e.message}")
        }
    }

    private fun getCategoryColorHex(category: FileCategory): String {
        return when (category) {
            FileCategory.FOLDER -> "#FBBF24"
            FileCategory.PDF -> "#EF4444"
            FileCategory.IMAGE -> "#8B5CF6"
            FileCategory.VIDEO -> "#10B981"
            FileCategory.AUDIO -> "#F59E0B"
            FileCategory.TEXT -> "#0EA5E9"
            FileCategory.CODE -> "#3B82F6"
            FileCategory.DOCUMENT -> "#6366F1"
            FileCategory.ARCHIVE -> "#64748B"
            FileCategory.SYSTEM_BINARY -> "#EC4899"
            FileCategory.OTHER -> "#6B7280"
        }
    }
}
