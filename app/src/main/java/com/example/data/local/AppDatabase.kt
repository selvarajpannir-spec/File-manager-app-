package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.FileDao
import com.example.data.local.dao.TagDao
import com.example.data.local.entity.FileContentFTS
import com.example.data.local.entity.FileEntity
import com.example.data.local.entity.FileTagCrossRef
import com.example.data.local.entity.TagEntity

@Database(
    entities = [
        FileEntity::class,
        TagEntity::class,
        FileTagCrossRef::class,
        FileContentFTS::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "file_manager_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
