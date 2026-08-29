package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CivicNoticeEntity::class,
        SubscriptionEntity::class,
        SavedObjectionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TarewDatabase : RoomDatabase() {
    abstract fun tarewDao(): TarewDao

    companion object {
        @Volatile
        private var INSTANCE: TarewDatabase? = null

        fun getInstance(context: Context): TarewDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TarewDatabase::class.java,
                    "tarew_civic_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
