package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "file_uri")
    val fileUri: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    @ColumnInfo(name = "file_type")
    val fileType: String,
    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "content_text")
    val contentText: String = "",
    @ColumnInfo(name = "file_path")
    val filePath: String? = null,
    @ColumnInfo(name = "is_sample")
    val isSample: Boolean = false
)
