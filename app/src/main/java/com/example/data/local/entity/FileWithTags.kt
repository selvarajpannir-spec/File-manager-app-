package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class FileWithTags(
    @Embedded val file: FileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tag_id",
        associateBy = Junction(
            value = FileTagCrossRef::class,
            parentColumn = "file_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity> = emptyList()
)
