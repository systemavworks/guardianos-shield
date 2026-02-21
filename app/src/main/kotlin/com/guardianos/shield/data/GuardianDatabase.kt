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
    version = 9,               // ← v8 → v9: bonus gaming para recompensas del padre
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

        /** Migración v6 → v7: añade horario diferenciado para fin de semana */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN weekendScheduleEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN weekendStartTimeMinutes INTEGER NOT NULL DEFAULT 600")
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN weekendEndTimeMinutes INTEGER NOT NULL DEFAULT 1320")
            }
        }

        /** Migración v7 → v8: añade horario de colegio (bloqueo L-V en horas lectivas) */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN schoolScheduleEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN schoolStartTimeMinutes INTEGER NOT NULL DEFAULT 540")  // 09:00
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN schoolEndTimeMinutes INTEGER NOT NULL DEFAULT 840")    // 14:00
            }
        }

        /** Migración v8 → v9: bonus de gaming concedido manualmente por el padre */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE user_profiles ADD COLUMN minutosGamingExtra INTEGER NOT NULL DEFAULT 0"
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration() // fallback solo si la migración falla
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
