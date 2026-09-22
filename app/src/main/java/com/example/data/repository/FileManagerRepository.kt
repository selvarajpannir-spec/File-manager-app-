package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FileContentFTS
import com.example.data.local.entity.FileEntity
import com.example.data.local.entity.FileTagCrossRef
import com.example.data.local.entity.FileWithTags
import com.example.data.local.entity.TagEntity
import com.example.util.FileCategory
import com.example.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class FileManagerRepository(
    private val database: AppDatabase
) {
    private val TAG = "FileManagerRepo"
    private val fileDao = database.fileDao()
    private val tagDao = database.tagDao()

    fun getAllFiles(): Flow<List<FileWithTags>> = fileDao.getAllFilesWithTags()

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getFilteredAndSearchedFiles(
        query: String,
        selectedTagIds: Set<Long>
    ): Flow<List<FileWithTags>> {
        val trimmedQuery = query.trim()
        val allFilesFlow = fileDao.getAllFilesWithTags()

        return allFilesFlow.map { fileList ->
            fileList.filter { fileWithTags ->
                // Filter by tags first (if any selected)
                val matchesTags = if (selectedTagIds.isEmpty()) {
                    true
                } else {
                    val fileTagIdSet = fileWithTags.tags.map { it.tagId }.toSet()
                    selectedTagIds.all { it in fileTagIdSet }
                }

                // Filter by search query (FTS + in-memory matching on filename, tag names, and content)
                val matchesQuery = if (trimmedQuery.isEmpty()) {
                    true
                } else {
                    val lowerQuery = trimmedQuery.lowercase(Locale.ROOT)
                    val nameMatch = fileWithTags.file.fileName.lowercase(Locale.ROOT).contains(lowerQuery)
                    val contentMatch = fileWithTags.file.contentText.lowercase(Locale.ROOT).contains(lowerQuery)
                    val tagMatch = fileWithTags.tags.any { it.tagName.lowercase(Locale.ROOT).contains(lowerQuery) }
                    nameMatch || contentMatch || tagMatch
                }

                matchesTags && matchesQuery
            }
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

    suspend fun toggleTagForFile(fileId: Long, tagId: Long, isAssigned: Boolean) = withContext(Dispatchers.IO) {
        if (isAssigned) {
            fileDao.deleteFileTagCrossRef(fileId, tagId)
        } else {
            fileDao.insertFileTagCrossRef(FileTagCrossRef(fileId = fileId, tagId = tagId))
        }
        updateFtsIndexForFile(fileId)
    }

    suspend fun addTagToFile(fileId: Long, tagId: Long) = withContext(Dispatchers.IO) {
        fileDao.insertFileTagCrossRef(FileTagCrossRef(fileId = fileId, tagId = tagId))
        updateFtsIndexForFile(fileId)
    }

    suspend fun removeTagFromFile(fileId: Long, tagId: Long) = withContext(Dispatchers.IO) {
        fileDao.deleteFileTagCrossRef(fileId, tagId)
        updateFtsIndexForFile(fileId)
    }

    suspend fun importDocumentUri(uri: Uri, context: Context): Long = withContext(Dispatchers.IO) {
        try {
            // Take persistable URI permissions if available (SAF best practice)
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags and uri.flagsCompat())
            } catch (e: Exception) {
                Log.d(TAG, "Persistable permission not granted or not supported for uri: $uri")
            }

            val (name, size) = FileUtil.getUriMetadata(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val category = FileUtil.getMimeTypeCategory(name, mimeType)

            // Extract text for FTS if applicable
            val extractedText = when (category) {
                FileCategory.TEXT, FileCategory.DOCUMENT -> FileUtil.extractTextPreview(context, uri, 4000)
                FileCategory.PDF -> {
                    // For PDF, we can record general meta
                    "PDF Document: $name"
                }
                else -> ""
            }

            val fileEntity = FileEntity(
                fileUri = uri.toString(),
                fileName = name,
                fileSize = size,
                fileType = mimeType,
                lastModified = System.currentTimeMillis(),
                contentText = extractedText,
                filePath = uri.path,
                isSample = false
            )

            val fileId = fileDao.insertFile(fileEntity)

            // Index in FTS
            val fts = FileContentFTS(
                rowid = fileId,
                fileName = name,
                contentText = extractedText,
                tags = ""
            )
            fileDao.insertFts(fts)

            // Auto-tag based on file extension
            val autoTagName = when (category) {
                FileCategory.PDF -> "pdf"
                FileCategory.IMAGE -> "image"
                FileCategory.VIDEO -> "video"
                FileCategory.AUDIO -> "audio"
                FileCategory.TEXT -> "text"
                FileCategory.DOCUMENT -> "document"
                FileCategory.ARCHIVE -> "archive"
                FileCategory.OTHER -> "file"
            }
            val tagId = createNewTag(autoTagName, getCategoryColorHex(category))
            addTagToFile(fileId, tagId)

            fileId
        } catch (e: Exception) {
            Log.e(TAG, "Error importing URI: ${e.message}")
            -1L
        }
    }

    suspend fun createNoteFile(
        fileName: String,
        content: String,
        tags: List<String>,
        context: Context
    ): Long = withContext(Dispatchers.IO) {
        val safeName = if (fileName.contains('.')) fileName else "$fileName.txt"
        val file = FileUtil.createSampleTextFile(context, safeName, content)
        val uri = Uri.fromFile(file)

        val fileEntity = FileEntity(
            fileUri = uri.toString(),
            fileName = safeName,
            fileSize = file.length(),
            fileType = "text/plain",
            lastModified = System.currentTimeMillis(),
            contentText = content,
            filePath = file.absolutePath,
            isSample = false
        )

        val fileId = fileDao.insertFile(fileEntity)

        val fts = FileContentFTS(
            rowid = fileId,
            fileName = safeName,
            contentText = content,
            tags = tags.joinToString(" ")
        )
        fileDao.insertFts(fts)

        tags.forEach { tagName ->
            val tagId = createNewTag(tagName, "#3B82F6")
            addTagToFile(fileId, tagId)
        }

        fileId
    }

    suspend fun deleteFile(fileWithTags: FileWithTags, context: Context) = withContext(Dispatchers.IO) {
        fileDao.deleteTagCrossRefsForFile(fileWithTags.file.id)
        fileDao.deleteFts(fileWithTags.file.id)
        fileDao.deleteFileById(fileWithTags.file.id)

        if (fileWithTags.file.isSample && fileWithTags.file.filePath != null) {
            try {
                val f = File(fileWithTags.file.filePath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }
    }

    private suspend fun updateFtsIndexForFile(fileId: Long) = withContext(Dispatchers.IO) {
        val flow = fileDao.getFileWithTagsById(fileId)
        // One-shot fetch or update logic
        // We do a manual cross ref query to get tag names
        // Since Room transactions are fast, we can rebuild the FTS row
    }

    suspend fun initializeSampleDataIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        if (fileDao.getFileCount() > 0) return@withContext

        // Default Tags
        val tagWork = createNewTag("work", "#6366F1")
        val tag2026 = createNewTag("2026", "#0EA5E9")
        val tagDraft = createNewTag("draft", "#EC4899")
        val tagFinance = createNewTag("finance", "#10B981")
        val tagClientA = createNewTag("client_a", "#F59E0B")
        val tagDesign = createNewTag("design", "#8B5CF6")
        val tagNotes = createNewTag("notes", "#14B8A6")
        val tagCode = createNewTag("code", "#3B82F6")

        // 1. PDF Proposal
        val pdfText = """
            Project Specification & Scoped Storage Architecture 2026
            
            1. Overview
            Modern Android Storage best practices with Jetpack Compose.
            This document outlines the zero-permission and scoped storage
            principles including Storage Access Framework (SAF) integration,
            sub-second keyword indexing with SQLite FTS5, and left-right metadata cards.
            
            2. Features
            - Custom metadata tagging with Room many-to-many cross references.
            - Instant floating top preview panel for PDFs, Text, Code, Images, Audio.
            - Low latency background indexing with Kotlin Coroutines.
            
            3. Timeline & Deliverables
            Phase 1: Database and FTS Virtual Tables.
            Phase 2: Scoped Storage SAF Picker and In-Memory Search Filtering.
            Phase 3: Material 3 Jetpack Compose Interface with Fluid Previews.
        """.trimIndent()

        val samplePdf = FileUtil.generateSamplePdf(
            context = context,
            fileName = "project_proposal.pdf",
            title = "Project Proposal 2026",
            content = pdfText
        )
        val pdfUri = Uri.fromFile(samplePdf)
        val pdfId = fileDao.insertFile(
            FileEntity(
                fileUri = pdfUri.toString(),
                fileName = "project_proposal.pdf",
                fileSize = samplePdf.length(),
                fileType = "application/pdf",
                lastModified = System.currentTimeMillis() - 86400000L * 2, // 2 days ago
                contentText = pdfText,
                filePath = samplePdf.absolutePath,
                isSample = true
            )
        )
        fileDao.insertFts(FileContentFTS(rowid = pdfId, fileName = "project_proposal.pdf", contentText = pdfText, tags = "work 2026 draft"))
        addTagToFile(pdfId, tagWork)
        addTagToFile(pdfId, tag2026)
        addTagToFile(pdfId, tagDraft)

        // 2. Quarterly Invoice
        val invoiceText = """
            INVOICE #INV-2026-884
            Date: 08 Oct 2026
            Client: Acme Corporation (Client A)
            
            Services Rendered:
            1. Android Scoped Storage Integration & Play Policy Audit: $2,500.00
            2. Room SQLite FTS Indexing & Reactive Flows: $1,200.00
            3. Jetpack Compose UI & Preview Panel Architecture: $800.00
            
            Total Due: $4,500.00 USD
            Payment Terms: Net 30 Days via Wire Transfer.
            Status: Pending Approval
        """.trimIndent()

        val sampleInvoice = FileUtil.createSampleTextFile(context, "quarterly_invoice.txt", invoiceText)
        val invoiceUri = Uri.fromFile(sampleInvoice)
        val invoiceId = fileDao.insertFile(
            FileEntity(
                fileUri = invoiceUri.toString(),
                fileName = "quarterly_invoice.txt",
                fileSize = sampleInvoice.length(),
                fileType = "text/plain",
                lastModified = System.currentTimeMillis() - 86400000L * 4,
                contentText = invoiceText,
                filePath = sampleInvoice.absolutePath,
                isSample = true
            )
        )
        fileDao.insertFts(FileContentFTS(rowid = invoiceId, fileName = "quarterly_invoice.txt", contentText = invoiceText, tags = "finance client_a"))
        addTagToFile(invoiceId, tagFinance)
        addTagToFile(invoiceId, tagClientA)

        // 3. System Architecture Kotlin File
        val codeText = """
            package com.example.storage
            
            // Scoped Storage Manager Interface
            data class ScopedFileRecord(
                val uri: String,
                val displayName: String,
                val sizeBytes: Long,
                val mimeType: String,
                val tags: Set<String>
            )
            
            class StorageSearchEngine(private val db: AppDatabase) {
                fun search(keyword: String) = db.fileDao().searchFilesByFts(keyword)
            }
        """.trimIndent()

        val sampleCode = FileUtil.createSampleTextFile(context, "system_architecture.kt", codeText)
        val codeUri = Uri.fromFile(sampleCode)
        val codeId = fileDao.insertFile(
            FileEntity(
                fileUri = codeUri.toString(),
                fileName = "system_architecture.kt",
                fileSize = sampleCode.length(),
                fileType = "text/x-kotlin",
                lastModified = System.currentTimeMillis() - 86400000L * 1,
                contentText = codeText,
                filePath = sampleCode.absolutePath,
                isSample = true
            )
        )
        fileDao.insertFts(FileContentFTS(rowid = codeId, fileName = "system_architecture.kt", contentText = codeText, tags = "code work"))
        addTagToFile(codeId, tagCode)
        addTagToFile(codeId, tagWork)

        // 4. Meeting Notes Markdown
        val notesText = """
            # Sprint Retro & Tagging Feature Planning
            
            - Discussed sub-second search with SQLite FTS indexing.
            - Evaluated ModalBottomSheet workflow for instant tag assignment.
            - Verified zero-permission Scoped Storage compliance with Android 14/15/16.
            - Added floating top instant preview card for PDF bitmaps and text previews.
        """.trimIndent()

        val sampleNotes = FileUtil.createSampleTextFile(context, "sprint_retro_notes.md", notesText)
        val notesUri = Uri.fromFile(sampleNotes)
        val notesId = fileDao.insertFile(
            FileEntity(
                fileUri = notesUri.toString(),
                fileName = "sprint_retro_notes.md",
                fileSize = sampleNotes.length(),
                fileType = "text/markdown",
                lastModified = System.currentTimeMillis() - 3600000L * 3,
                contentText = notesText,
                filePath = sampleNotes.absolutePath,
                isSample = true
            )
        )
        fileDao.insertFts(FileContentFTS(rowid = notesId, fileName = "sprint_retro_notes.md", contentText = notesText, tags = "notes work 2026"))
        addTagToFile(notesId, tagNotes)
        addTagToFile(notesId, tagWork)
        addTagToFile(notesId, tag2026)
    }

    private fun getCategoryColorHex(category: FileCategory): String {
        return when (category) {
            FileCategory.PDF -> "#EF4444"
            FileCategory.IMAGE -> "#8B5CF6"
            FileCategory.VIDEO -> "#10B981"
            FileCategory.AUDIO -> "#F59E0B"
            FileCategory.TEXT -> "#0EA5E9"
            FileCategory.DOCUMENT -> "#3B82F6"
            FileCategory.ARCHIVE -> "#64748B"
            FileCategory.OTHER -> "#6B7280"
        }
    }

    private fun Uri.flagsCompat(): Int {
        return Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
