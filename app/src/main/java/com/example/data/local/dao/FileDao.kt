package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.FileContentFTS
import com.example.data.local.entity.FileEntity
import com.example.data.local.entity.FileTagCrossRef
import com.example.data.local.entity.FileWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Transaction
    @Query("SELECT * FROM files ORDER BY last_modified DESC")
    fun getAllFilesWithTags(): Flow<List<FileWithTags>>

    @Transaction
    @Query("SELECT * FROM files WHERE id = :id")
    fun getFileWithTagsById(id: Long): Flow<FileWithTags?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Update
    suspend fun updateFile(file: FileEntity)

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(fts: FileContentFTS)

    @Update
    suspend fun updateFts(fts: FileContentFTS)

    @Query("DELETE FROM file_contents_fts WHERE rowid = :rowid")
    suspend fun deleteFts(rowid: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFileTagCrossRef(crossRef: FileTagCrossRef)

    @Query("DELETE FROM file_tag_cross_ref WHERE file_id = :fileId AND tag_id = :tagId")
    suspend fun deleteFileTagCrossRef(fileId: Long, tagId: Long)

    @Query("DELETE FROM file_tag_cross_ref WHERE file_id = :fileId")
    suspend fun deleteTagCrossRefsForFile(fileId: Long)

    @Transaction
    @Query("""
        SELECT f.* FROM files f
        JOIN file_contents_fts fts ON f.id = fts.rowid
        WHERE file_contents_fts MATCH :query
        ORDER BY f.last_modified DESC
    """)
    fun searchFilesByFts(query: String): Flow<List<FileWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT f.* FROM files f
        JOIN file_tag_cross_ref ft ON f.id = ft.file_id
        WHERE ft.tag_id IN (:tagIds)
        ORDER BY f.last_modified DESC
    """)
    fun getFilesByTagIds(tagIds: List<Long>): Flow<List<FileWithTags>>

    @Query("SELECT COUNT(*) FROM files")
    suspend fun getFileCount(): Int
}
