// app/src/main/java/com/guardianos/shield/data/GuardianDatabase.kt
package com.guardianos.shield.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Database(
    entities = [
        BlockedSiteEntity::class,
        StatisticEntity::class,
        CustomFilterEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun blockedSiteDao(): BlockedSiteDao
    abstract fun statisticDao(): StatisticDao
    abstract fun customFilterDao(): CustomFilterDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: GuardianDatabase? = null

        fun getDatabase(context: Context): GuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    "guardian_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Convertidores de tipos
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

// ============ ENTIDADES ============

@Entity(tableName = "blocked_sites")
data class BlockedSiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val domain: String,
    val category: String,
    val timestamp: Date,
    val userId: Long? = null
)

@Entity(tableName = "statistics")
data class StatisticEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Date,
    val totalBlocked: Int,
    val adultContentBlocked: Int,
    val violenceBlocked: Int,
    val malwareBlocked: Int,
    val socialMediaBlocked: Int,
    val userId: Long? = null
)

@Entity(tableName = "custom_filters")
data class CustomFilterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val isWhitelisted: Boolean,
    val isBlacklisted: Boolean,
    val addedDate: Date,
    val userId: Long? = null
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val age: Int,
    val restrictionLevel: String, // "STRICT", "MODERATE", "MILD"
    val allowSocialMedia: Boolean,
    val allowedHoursStart: Int, // 0-23
    val allowedHoursEnd: Int, // 0-23
    val parentalPin: String? = null,
    val createdDate: Date
)

// ============ DAOs ============

@Dao
interface BlockedSiteDao {
    @Query("SELECT * FROM blocked_sites ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentBlocked(limit: Int = 50): Flow<List<BlockedSiteEntity>>

    @Query("SELECT * FROM blocked_sites WHERE DATE(timestamp/1000, 'unixepoch') = DATE('now') ORDER BY timestamp DESC")
    fun getTodayBlocked(): Flow<List<BlockedSiteEntity>>

    @Query("SELECT COUNT(*) FROM blocked_sites WHERE DATE(timestamp/1000, 'unixepoch') = DATE('now')")
    fun getTodayBlockedCount(): Flow<Int>

    @Query("SELECT * FROM blocked_sites WHERE category = :category ORDER BY timestamp DESC LIMIT :limit")
    fun getBlockedByCategory(category: String, limit: Int = 50): Flow<List<BlockedSiteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedSite: BlockedSiteEntity)

    @Query("DELETE FROM blocked_sites WHERE timestamp < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: Date)

    @Query("SELECT COUNT(*) FROM blocked_sites")
    suspend fun getTotalCount(): Int
}

@Dao
interface StatisticDao {
    @Query("SELECT * FROM statistics ORDER BY date DESC LIMIT 30")
    fun getLast30Days(): Flow<List<StatisticEntity>>

    @Query("SELECT * FROM statistics WHERE DATE(date/1000, 'unixepoch') = DATE('now')")
    fun getTodayStats(): Flow<StatisticEntity?>

    @Query("SELECT SUM(totalBlocked) FROM statistics WHERE date >= :fromDate")
    fun getTotalBlockedSince(fromDate: Date): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(statistic: StatisticEntity)

    @Update
    suspend fun update(statistic: StatisticEntity)

    @Query("SELECT * FROM statistics WHERE date BETWEEN :startDate AND :endDate")
    fun getStatsBetween(startDate: Date, endDate: Date): Flow<List<StatisticEntity>>
}

@Dao
interface CustomFilterDao {
    @Query("SELECT * FROM custom_filters WHERE isBlacklisted = 1")
    fun getBlacklist(): Flow<List<CustomFilterEntity>>

    @Query("SELECT * FROM custom_filters WHERE isWhitelisted = 1")
    fun getWhitelist(): Flow<List<CustomFilterEntity>>

    @Query("SELECT * FROM custom_filters WHERE domain = :domain")
    suspend fun getByDomain(domain: String): CustomFilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: CustomFilterEntity)

    @Delete
    suspend fun delete(filter: CustomFilterEntity)

    @Query("DELETE FROM custom_filters WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getProfile(id: Long): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfileEntity): Long

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Delete
    suspend fun delete(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun getActiveProfile(): Flow<UserProfileEntity?>
}

// ============ REPOSITORY ============

class GuardianRepository(private val database: GuardianDatabase) {
    private val blockedSiteDao = database.blockedSiteDao()
    private val statisticDao = database.statisticDao()
    private val customFilterDao = database.customFilterDao()
    private val userProfileDao = database.userProfileDao()

    // Sitios bloqueados
    val recentBlocked = blockedSiteDao.getRecentBlocked(50)
    val todayBlocked = blockedSiteDao.getTodayBlocked()
    val todayBlockedCount = blockedSiteDao.getTodayBlockedCount()

    suspend fun addBlockedSite(url: String, category: String) {
        val domain = extractDomain(url)
        blockedSiteDao.insert(
            BlockedSiteEntity(
                url = url,
                domain = domain,
                category = category,
                timestamp = Date()
            )
        )
        updateTodayStatistics(category)
    }

    private suspend fun updateTodayStatistics(category: String) {
        val today = statisticDao.getTodayStats()
        // Implementar lógica de actualización
    }

    private fun extractDomain(url: String): String {
        return url.split("/").getOrNull(2) ?: url
    }

    // Filtros personalizados
    val blacklist = customFilterDao.getBlacklist()
    val whitelist = customFilterDao.getWhitelist()

    suspend fun addToBlacklist(domain: String) {
        customFilterDao.insert(
            CustomFilterEntity(
                domain = domain,
                isWhitelisted = false,
                isBlacklisted = true,
                addedDate = Date()
            )
        )
    }

    suspend fun addToWhitelist(domain: String) {
        customFilterDao.insert(
            CustomFilterEntity(
                domain = domain,
                isWhitelisted = true,
                isBlacklisted = false,
                addedDate = Date()
            )
        )
    }

    suspend fun removeFilter(domain: String) {
        customFilterDao.deleteByDomain(domain)
    }

    // Perfiles de usuario
    val activeProfile = userProfileDao.getActiveProfile()

    suspend fun createProfile(
        name: String,
        age: Int,
        restrictionLevel: String,
        parentalPin: String?
    ): Long {
        return userProfileDao.insert(
            UserProfileEntity(
                name = name,
                age = age,
                restrictionLevel = restrictionLevel,
                allowSocialMedia = restrictionLevel != "STRICT",
                allowedHoursStart = 8,
                allowedHoursEnd = 21,
                parentalPin = parentalPin,
                createdDate = Date()
            )
        )
    }

    // Estadísticas
    val last30DaysStats = statisticDao.getLast30Days()

    suspend fun cleanOldData(daysToKeep: Int = 30) {
        val cutoffDate = Date(System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L))
        blockedSiteDao.deleteOlderThan(cutoffDate)
    }
}
