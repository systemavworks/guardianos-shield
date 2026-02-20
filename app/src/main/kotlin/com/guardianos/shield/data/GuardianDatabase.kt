package com.guardianos.shield.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BlockedSiteEntity::class,
        StatisticEntity::class,
        UserProfileEntity::class,
        CustomFilterEntity::class,
        DnsLogEntity::class,
        SensitiveAppEntity::class,
        PetitionEntity::class   // ← Pacto Digital Familiar
    ],
    version = 6,               // ← v5 → v6: TrustFlow minutosAutonomiaDiarios
    exportSchema = false
)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun blockedSiteDao(): BlockedSiteDao
    abstract fun statisticsDao(): StatisticDao
    abstract fun profileDao(): UserProfileDao
    abstract fun customFilterDao(): CustomFilterDao
    abstract fun dnsLogDao(): DnsLogDao
    abstract fun sensitiveAppDao(): SensitiveAppDao
    abstract fun petitionDao(): PetitionDao   // ← Pacto Digital

    companion object {
        @Volatile
        private var INSTANCE: GuardianDatabase? = null

        /**
         * Migración v4 → v5:
         * 1. Crea tabla `petitions` para el Pacto Digital Familiar
         * 2. Añade columnas de racha a `user_profiles` para la gamificación
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Tabla de peticiones del Pacto Digital
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS petitions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        profileId INTEGER NOT NULL DEFAULT 1,
                        tipo TEXT NOT NULL,
                        valorSolicitado TEXT NOT NULL,
                        razonHijo TEXT NOT NULL DEFAULT '',
                        estado TEXT NOT NULL DEFAULT 'PENDING',
                        notaPadre TEXT NOT NULL DEFAULT '',
                        creadoEn INTEGER NOT NULL,
                        respondidoEn INTEGER,
                        minutosAprobados INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Campos de gamificación (racha diaria) en perfiles
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN rachaActual INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN rachaMaxima INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN ultimoDiaLimpio TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Migración v5 → v6: añade presupuesto diario de autonomía para el TrustFlow Engine */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE user_profiles ADD COLUMN minutosAutonomiaDiarios INTEGER NOT NULL DEFAULT 60"
                )
            }
        }

        fun getDatabase(context: Context): GuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    "guardian_shield.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration() // fallback solo si la migración falla
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
