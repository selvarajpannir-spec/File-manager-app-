package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FileContentFTS
import com.example.data.local.entity.FileEntity
import com.example.data.local.entity.FileTagCrossRef
import com.example.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("File Manager", appName)
    }

    @Test
    fun `insert file and tags and retrieve with cross ref`() = runBlocking {
        val fileDao = db.fileDao()
        val tagDao = db.tagDao()

        // 1. Insert tag
        val tagId = tagDao.insertTag(TagEntity(tagName = "finance", colorHex = "#10B981"))

        // 2. Insert file
        val fileId = fileDao.insertFile(
            FileEntity(
                fileUri = "content://com.example/invoices/inv_2026.pdf",
                fileName = "inv_2026.pdf",
                fileSize = 1048576L,
                fileType = "application/pdf",
                contentText = "Invoice Acme Corp consulting fees"
            )
        )

        // 3. Associate file and tag
        fileDao.insertFileTagCrossRef(FileTagCrossRef(fileId = fileId, tagId = tagId))

        // 4. Index in FTS
        fileDao.insertFts(
            FileContentFTS(
                rowid = fileId,
                fileName = "inv_2026.pdf",
                contentText = "Invoice Acme Corp consulting fees",
                tags = "finance"
            )
        )

        // 5. Query and verify
        val filesWithTags = fileDao.getAllFilesWithTags().first()
        assertEquals(1, filesWithTags.size)
        assertEquals("inv_2026.pdf", filesWithTags[0].file.fileName)
        assertEquals(1, filesWithTags[0].tags.size)
        assertEquals("finance", filesWithTags[0].tags[0].tagName)
    }
}
