package com.example.data.model

import com.example.data.local.entity.TagEntity
import com.example.util.FileCategory
import com.example.util.FileUtil
import java.io.File

data class FileSystemItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0L else file.length(),
    val formattedSize: String = if (file.isDirectory) "" else FileUtil.formatFileSize(file.length()),
    val lastModified: Long = file.lastModified(),
    val category: FileCategory = FileUtil.getFileCategory(file),
    val isSystemFile: Boolean = FileUtil.isSystemPath(file.absolutePath),
    val permissions: String = FileUtil.getPermissionsString(file),
    val canRead: Boolean = file.canRead(),
    val canWrite: Boolean = file.canWrite(),
    val childCount: Int? = if (file.isDirectory) file.listFiles()?.size else null,
    val tags: List<TagEntity> = emptyList(),
    val dbFileId: Long? = null
)
