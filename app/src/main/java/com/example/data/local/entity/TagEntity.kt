package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["tag_name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "tag_id")
    val tagId: Long = 0,
    @ColumnInfo(name = "tag_name")
    val tagName: String,
    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#6366F1"
)
