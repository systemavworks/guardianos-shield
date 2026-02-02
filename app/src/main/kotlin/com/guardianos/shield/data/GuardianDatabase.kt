// app/src/main/java/com/guardianos/shield/data/GuardianDatabase.kt
package com.guardianos.shield.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BlockedSiteEntity::class,
        StatisticEntity::class,
        UserProfileEntity::class,
        CustomFilterEntity::class,
        DnsLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun blockedSiteDao(): BlockedSiteDao
    abstract fun statisticsDao(): StatisticDao
    abstract fun profileDao(): UserProfileDao
    abstract fun customFilterDao(): CustomFilterDao
    abstract fun dnsLogDao(): DnsLogDao

    companion object {
        @Volatile
        private var INSTANCE: GuardianDatabase? = null

        fun getDatabase(context: Context): GuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    "guardian_shield.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
