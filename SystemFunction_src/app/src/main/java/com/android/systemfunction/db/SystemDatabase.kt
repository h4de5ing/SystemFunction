package com.android.systemfunction.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Config::class,
        PackageList::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SystemDatabase : RoomDatabase() {
    abstract fun barcodeDao(): SystemDao

    companion object {
        @Volatile
        private var INSTANCE: SystemDatabase? = null
        fun create(context: Context): SystemDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                SystemDatabase::class.java,
                "data.db"
            ).allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
    }
}