package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "file_contents_fts")
@Fts4
data class FileContentFTS(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "content_text")
    val contentText: String,
    @ColumnInfo(name = "tags")
    val tags: String
)
