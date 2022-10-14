package com.android.droidwall.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FirewallData::class],
    version = 1,
    exportSchema = false
)
abstract class FWDatabase : RoomDatabase() {
    abstract fun fwDao(): FWDao

    companion object {
        @Volatile
        private var INSTANCE: FWDatabase? = null
        fun create(context: Context): FWDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                FWDatabase::class.java,
                "data.db"
            ).allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
    }
}