package com.jcoder.linker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jcoder.linker.dao.LinkDao
import com.jcoder.linker.data.Link

@Database(entities = [Link::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao

    companion object {
        private const val DB_NAME = "links"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .build()
        }
    }
}