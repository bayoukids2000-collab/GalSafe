package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScannedProductEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GalSafeDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: GalSafeDatabase? = null

        fun getInstance(context: Context): GalSafeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GalSafeDatabase::class.java,
                    "galsafe_local_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
