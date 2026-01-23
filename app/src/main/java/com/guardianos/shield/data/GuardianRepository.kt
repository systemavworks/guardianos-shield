// app/src/main/java/com/guardianos/shield/data/GuardianRepository.kt
package com.guardianos.shield.data

import kotlinx.coroutines.flow.Flow
import java.util.*

class GuardianRepository(private val database: GuardianDatabase) {

    private val blockedDao = database.blockedSiteDao()
    private val statisticsDao = database.statisticsDao()
    private val profileDao = database.profileDao()
    private val filterDao = database.customFilterDao()

    // ========== BLOCKED SITES ==========
    suspend fun addBlockedSite(
        domain: String,
        category: String,
        threatLevel: String = "MEDIUM"
    ) {
        val entity = BlockedSiteEntity(
            domain = domain,
            category = category,
            timestamp = System.currentTimeMillis(),
            threatLevel = threatLevel
        )
        blockedDao.insert(entity)
        updateTodayStatistics()
    }

    val recentBlocked: Flow<List<BlockedSiteEntity>> = blockedDao.getRecentBlocked(20)
    val todayBlockedCount: Flow<Int> = blockedDao.getTodayBlockedCount(getTodayStartTimestamp())

    suspend fun getBlockedSitesByDateRange(start: Long, end: Long): List<BlockedSiteEntity> {
        return blockedDao.getBlockedSitesByDateRange(start, end)
    }

    // ========== STATISTICS ==========
    private suspend fun updateTodayStatistics() {
        val today = getTodayDateString()
        val allBlocked = blockedDao.getBlockedForDate(getTodayStartTimestamp(), getTodayEndTimestamp())

        val malwareCount = allBlocked.count { it.category == "MALWARE" }
        val adultCount = allBlocked.count { it.category == "ADULT" }
        val violenceCount = allBlocked.count { it.category == "VIOLENCE" }
        val socialCount = allBlocked.count { it.category == "SOCIAL_MEDIA" }
        val totalCount = allBlocked.size

        val existing = statisticsDao.getStatisticByDate(today)
        if (existing != null) {
            statisticsDao.update(existing.copy(
                totalBlocked = totalCount,
                malwareBlocked = malwareCount,
                adultContentBlocked = adultCount,
                violenceBlocked = violenceCount,
                socialMediaBlocked = socialCount
            ))
        } else {
            statisticsDao.insert(
                StatisticEntity(
                    date = today,
                    totalBlocked = totalCount,
                    malwareBlocked = malwareCount,
                    adultContentBlocked = adultCount,
                    violenceBlocked = violenceCount,
                    socialMediaBlocked = socialCount
                )
            )
        }
    }

    val last30DaysStats: Flow<List<StatisticEntity>> = statisticsDao.getLast30Days()

    suspend fun getTotalBlockedCount(): Int {
        return statisticsDao.getTotalBlockedCount()
    }

    // ========== USER PROFILES ==========
    val activeProfile: Flow<UserProfileEntity?> = profileDao.getActiveProfile()

    suspend fun updateProfile(profile: UserProfileEntity) {
        profileDao.update(profile)
    }

    suspend fun createProfile(
        name: String,
        age: Int?,
        restrictionLevel: String,
        parentalPin: String? = null
    ) {
        profileDao.deactivateAll()
        val profile = UserProfileEntity(
            name = name,
            age = age,
            parentalPin = parentalPin,
            restrictionLevel = restrictionLevel,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        profileDao.insert(profile)
    }

    suspend fun getAllProfiles(): List<UserProfileEntity> {
        return profileDao.getAllProfiles()
    }

    suspend fun deleteProfile(profile: UserProfileEntity) {
        profileDao.delete(profile)
    }

    // ========== CUSTOM FILTERS ==========
    val blacklist: Flow<List<CustomFilterEntity>> = filterDao.getBlacklist()
    val whitelist: Flow<List<CustomFilterEntity>> = filterDao.getWhitelist()

    suspend fun addToBlacklist(domain: String) {
        val filter = CustomFilterEntity(
            domain = domain.lowercase().trim(),
            type = "blacklist",
            addedAt = System.currentTimeMillis()
        )
        filterDao.insert(filter)
    }

    suspend fun addToWhitelist(domain: String) {
        val filter = CustomFilterEntity(
            domain = domain.lowercase().trim(),
            type = "whitelist",
            addedAt = System.currentTimeMillis()
        )
        filterDao.insert(filter)
    }

    suspend fun removeFilter(domain: String) {
        filterDao.deleteByDomain(domain)
    }

    suspend fun isInWhitelist(domain: String): Boolean {
        return filterDao.isInWhitelist(domain.lowercase().trim())
    }

    suspend fun isInBlacklist(domain: String): Boolean {
        return filterDao.isInBlacklist(domain.lowercase().trim())
    }

    // ========== DATA MANAGEMENT ==========
    suspend fun clearAllData() {
        blockedDao.deleteAll()
        statisticsDao.deleteAll()
    }

    suspend fun cleanOldData(retentionDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L)
        blockedDao.deleteOlderThan(cutoffTime)
        val cutoffDate = getDateString(cutoffTime)
        statisticsDao.deleteOlderThan(cutoffDate)
    }

    // ========== HELPER FUNCTIONS ==========
    private fun getTodayStartTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getTodayEndTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    private fun getTodayDateString(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun getDateString(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
