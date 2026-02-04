package com.guardianos.shield.data

import android.content.Context
import com.guardianos.shield.service.LocalBlocklist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class GuardianRepository(
    private val context: Context,
    private val database: GuardianDatabase
) {

    private val blockedDao = database.blockedSiteDao()
    private val statisticsDao = database.statisticsDao()
    private val profileDao = database.profileDao()
    private val filterDao = database.customFilterDao()
    private val dnsLogDao = database.dnsLogDao()

    // ==================== BLOQUEOS ====================

    suspend fun addBlockedSite(
        domain: String,
        category: String,
        threatLevel: Int = 2
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

    suspend fun logDnsQuery(domain: String) {
        val cleanDomain = domain.lowercase().trim()
        val activeProfile = profileDao.getActiveProfile().firstOrNull()
        val profileId = activeProfile?.id?.toLong() ?: 1L

        val isBlocked = LocalBlocklist.isBlocked(cleanDomain, this)
        val category = getCategoryFromDomain(cleanDomain)

        if (isBlocked && category != null) {
            addBlockedSite(cleanDomain, category, 2)
        }

        dnsLogDao.insert(
            DnsLogEntity(
                profileId = profileId,
                domain = cleanDomain,
                blocked = isBlocked
            )
        )
    }

    private fun getCategoryFromDomain(domain: String): String? {
        val lower = domain.lowercase()
        return when {
            lower.contains("porn") || lower.contains("sex") || lower.contains("xxx") ||
                    lower.contains("adult") || lower.contains("erotic") -> "ADULT"
            lower.contains("casino") || lower.contains("gambling") || lower.contains("betting") ||
                    lower.contains("poker") -> "GAMBLING"
            lower.contains("violence") || lower.contains("gore") -> "VIOLENCE"
            lower.contains("facebook") || lower.contains("instagram") || lower.contains("tiktok") ||
                    lower.contains("twitter") || lower.contains("snapchat") -> "SOCIAL_MEDIA"
            else -> null
        }
    }

    // ==================== FLUJOS OBSERVABLES ====================

    val recentBlocked: Flow<List<BlockedSiteEntity>> = blockedDao.getRecentBlocked(20)
    val todayBlockedCount: Flow<Int> = blockedDao.getTodayBlockedCount(getTodayStartTimestamp())

    // ✅ MÉTODO AÑADIDO: getRecentBlockedSites
    suspend fun getRecentBlockedSites(limit: Int): List<BlockedSiteEntity> {
        return blockedDao.getRecentBlockedList(limit)
    }

    suspend fun getBlockedSitesByDateRange(start: Long, end: Long): List<BlockedSiteEntity> =
        blockedDao.getBlockedSitesByDateRange(start, end)

    // ==================== ESTADÍSTICAS ====================

    private suspend fun updateTodayStatistics() {
        val today = getTodayDateString()
        val blockedToday = blockedDao.getBlockedForDate(
            getTodayStartTimestamp(),
            getTodayEndTimestamp()
        )

        val stats = StatisticEntity(
            dateKey = today,
            totalBlocked = blockedToday.size,
            uniqueDomains = blockedToday.map { it.domain }.distinct().size,
            malwareBlocked = blockedToday.count { it.category == "MALWARE" },
            adultContentBlocked = blockedToday.count { it.category == "ADULT" },
            violenceBlocked = blockedToday.count { it.category == "VIOLENCE" },
            socialMediaBlocked = blockedToday.count { it.category == "SOCIAL_MEDIA" },
            lastUpdated = System.currentTimeMillis()
        )

        val existing = statisticsDao.getStatisticByDate(today)
        if (existing != null) {
            statisticsDao.update(stats)
        } else {
            statisticsDao.insert(stats)
        }
    }

    val last30DaysStats: Flow<List<StatisticEntity>> = statisticsDao.getLast30Days()

    suspend fun getTotalBlockedCount(): Int = statisticsDao.getTotalBlockedCount() ?: 0

    // ==================== PERFILES ====================

    val activeProfile: Flow<UserProfileEntity?> = profileDao.getActiveProfile()

    suspend fun getActiveProfile(): UserProfileEntity? = activeProfile.firstOrNull()

    suspend fun updateProfile(profile: UserProfileEntity) {
        profileDao.update(profile)
    }

    suspend fun setActiveProfile(profileId: Int) {
        // Desactivar todos
        val all = profileDao.getAll()
        all.forEach { p ->
            if (p.isActive) profileDao.update(p.copy(isActive = false))
        }

        // Activar el seleccionado
        val profile = profileDao.getByIdSuspend(profileId)
        if (profile != null) {
            profileDao.update(profile.copy(isActive = true))
        }
    }

    suspend fun createProfile(
        name: String,
        age: Int?,
        restrictionLevel: String = "MEDIUM",
        parentalPin: String? = null
    ) {
        val ageGroup = when {
            age == null -> "8-12"
            age <= 12 -> "8-12"
            age <= 17 -> "13-17"
            else -> "18+"
        }

        val newProfile = UserProfileEntity(
            name = name,
            age = age,
            ageGroup = ageGroup,
            parentalPin = parentalPin,
            restrictionLevel = restrictionLevel,
            scheduleEnabled = false,
            startTimeMinutes = 480,
            endTimeMinutes = 1260,
            blockAdultContent = true,
            blockGambling = true,
            blockSocialMedia = false,
            blockGaming = false,
            blockStreaming = false,
            isActive = true
        )

        profileDao.insert(newProfile)
    }

    suspend fun getAllProfiles(): List<UserProfileEntity> = profileDao.getAll()

    suspend fun deleteProfile(id: Int) = profileDao.deleteByIdSuspend(id)

    // ==================== FILTROS PERSONALIZADOS ====================

    val blacklist: Flow<List<CustomFilterEntity>> = filterDao.getBlacklist()
    val whitelist: Flow<List<CustomFilterEntity>> = filterDao.getWhitelist()

    // ✅ MÉTODO AÑADIDO: getAllCustomFilters
    suspend fun getAllCustomFilters(): List<CustomFilterEntity> {
        return filterDao.getAllFilters()
    }

    suspend fun addToBlacklist(domain: String) {
        val clean = domain.lowercase().trim()
        // isActive = true (bloqueado); isEnabled = false (no forzar whitelist)
        filterDao.insert(
            CustomFilterEntity(
                domain = clean,
                pattern = clean,
                isActive = true,
                isEnabled = false,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addToWhitelist(domain: String) {
        val clean = domain.lowercase().trim()
        // isEnabled = true (permitido); isActive = false (no bloquear)
        filterDao.insert(
            CustomFilterEntity(
                domain = clean,
                pattern = clean,
                isActive = false,
                isEnabled = true,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFilter(domain: String) {
        filterDao.deleteByDomain(domain.lowercase().trim())
    }

    suspend fun isInWhitelist(domain: String): Boolean =
        filterDao.isInWhitelist(domain.lowercase().trim())

    suspend fun isInBlacklist(domain: String): Boolean =
        filterDao.isInBlacklist(domain.lowercase().trim())

    // ==================== APPS SENSIBLES (persistentes) ====================
    val sensitiveAppsFlow: Flow<List<SensitiveAppEntity>> = database.sensitiveAppDao().getAll()

    suspend fun addSensitiveApp(packageName: String, label: String = "") {
        database.sensitiveAppDao().insert(SensitiveAppEntity(packageName = packageName, label = label))
    }
    suspend fun removeSensitiveApp(packageName: String) {
        database.sensitiveAppDao().deleteByPackage(packageName)
    }
    suspend fun isAppSensitivePersisted(packageName: String): Boolean {
        return database.sensitiveAppDao().isSensitive(packageName)
    }

    // ==================== FILTRADO PARENTAL DINÁMICO ====================
    private var sensitiveCategories: Map<String, Boolean> = mapOf(
        "Sexo" to true,
        "Violencia" to true,
        "Apuestas" to true,
        "Redes sociales" to true
    )
    private var sensitiveDomains: List<String> = emptyList()
    private var sensitiveApps: List<String> = emptyList()

    fun updateSensitiveCategories(categories: Map<String, Boolean>) {
        sensitiveCategories = categories
    }
    fun updateSensitiveDomains(domains: List<String>) {
        sensitiveDomains = domains
    }
    fun updateSensitiveApps(apps: List<String>) {
        sensitiveApps = apps
    }

    fun isDomainSensitive(domain: String): Boolean {
        val d = domain.lowercase()
        if (sensitiveDomains.any { d.contains(it.lowercase()) }) return true
        if (sensitiveCategories["Sexo"] == true && (d.contains("porn") || d.contains("sex") || d.contains("xxx"))) return true
        if (sensitiveCategories["Violencia"] == true && (d.contains("violence") || d.contains("gore"))) return true
        if (sensitiveCategories["Apuestas"] == true && (d.contains("casino") || d.contains("gambling") || d.contains("bet"))) return true
        if (sensitiveCategories["Redes sociales"] == true && (d.contains("facebook") || d.contains("instagram") || d.contains("tiktok") || d.contains("twitter") || d.contains("snapchat"))) return true
        return false
    }

    fun isAppSensitive(packageName: String): Boolean {
        val p = packageName.lowercase()
        if (sensitiveApps.any { p.contains(it.lowercase()) }) return true
        if (sensitiveCategories["Redes sociales"] == true && (p.contains("facebook") || p.contains("instagram") || p.contains("tiktok") || p.contains("twitter") || p.contains("snapchat") || p.contains("discord") || p.contains("telegram"))) return true
        if (sensitiveCategories["Apuestas"] == true && (p.contains("bet") || p.contains("casino") || p.contains("gambling"))) return true
        if (sensitiveCategories["Violencia"] == true && (p.contains("violence") || p.contains("gore"))) return true
        if (sensitiveCategories["Sexo"] == true && (p.contains("sex") || p.contains("xxx") || p.contains("porn"))) return true
        return false
    }

    // ==================== LIMPIEZA DE DATOS ====================

    suspend fun clearAllData() {
        blockedDao.deleteAll()
        statisticsDao.deleteAll()
        dnsLogDao.deleteAll()
    }

    suspend fun cleanOldData(retentionDays: Int) {
        val cutoff = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000L)
        blockedDao.deleteOlderThan(cutoff)
        dnsLogDao.deleteOldLogs(cutoff)
        statisticsDao.deleteOldLogs(getDateString(cutoff))
    }

    // ==================== HELPERS ====================

    private fun getTodayStartTimestamp(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun getTodayEndTimestamp(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

    private fun getTodayDateString(): String {
        val cal = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun getDateString(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
