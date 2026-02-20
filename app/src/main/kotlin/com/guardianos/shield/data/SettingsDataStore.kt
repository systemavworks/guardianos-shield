package com.guardianos.shield.data

import android.content.Context
import com.guardianos.shield.BuildConfig
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ShieldSettings(
    val notificationsEnabled: Boolean = true,
    val autoBlockMalware: Boolean = true,
    val blockAdultContent: Boolean = true,
    val blockSocialMedia: Boolean = false,
    val dataRetentionDays: Int = 30,
    // Safe-mode: si true, no iniciar LightweightMonitorService automáticamente (útil para debugging/compatibilidad OEM)
    val disableLightweightMonitoring: Boolean = false,
    // Persistencia de estado VPN y modo
    val isVpnActive: Boolean = false,
    val isMonitoringActive: Boolean = false,
    val protectionMode: String = "Recommended",  // "Recommended", "Advanced", "CustomStats"
    val isPremium: Boolean = false // Estado premium (comprado)
)

object SettingsKeys {
    val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    val AUTO_BLOCK_MALWARE = booleanPreferencesKey("auto_block_malware")
    val BLOCK_ADULT = booleanPreferencesKey("block_adult")
    val BLOCK_SOCIAL = booleanPreferencesKey("block_social")
    val RETENTION_DAYS = intPreferencesKey("retention_days")
    // Safe-mode key
    val DISABLE_LIGHTWEIGHT_MONITORING = booleanPreferencesKey("disable_lightweight_monitoring")
    // Persistencia de estado
    val VPN_ACTIVE = booleanPreferencesKey("vpn_active")
    val MONITORING_ACTIVE = booleanPreferencesKey("monitoring_active")
    val PROTECTION_MODE = stringPreferencesKey("protection_mode")
    val PREMIUM = booleanPreferencesKey("premium_enabled")
    // Trial de 48 horas
    val FREE_TRIAL_START_TIME = longPreferencesKey("free_trial_start_time")
    val HAS_USED_FREE_TRIAL = booleanPreferencesKey("has_used_free_trial")
}

class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore

    val settings: Flow<ShieldSettings> = dataStore.data
        .map { prefs ->
            ShieldSettings(
                notificationsEnabled = prefs[SettingsKeys.NOTIFICATIONS] ?: true,
                autoBlockMalware = prefs[SettingsKeys.AUTO_BLOCK_MALWARE] ?: true,
                blockAdultContent = prefs[SettingsKeys.BLOCK_ADULT] ?: true,
                blockSocialMedia = prefs[SettingsKeys.BLOCK_SOCIAL] ?: false,
                dataRetentionDays = prefs[SettingsKeys.RETENTION_DAYS] ?: 30,
                disableLightweightMonitoring = prefs[SettingsKeys.DISABLE_LIGHTWEIGHT_MONITORING] ?: false,
                isVpnActive = prefs[SettingsKeys.VPN_ACTIVE] ?: false,
                isMonitoringActive = prefs[SettingsKeys.MONITORING_ACTIVE] ?: false,
                protectionMode = prefs[SettingsKeys.PROTECTION_MODE] ?: "Recommended",
                isPremium = prefs[SettingsKeys.PREMIUM] ?: false
            )
        }
    suspend fun setPremium(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.PREMIUM] = enabled }
    }
    val isPremium: Flow<Boolean> = settings.map { it.isPremium }

    suspend fun updateNotifications(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.NOTIFICATIONS] = enabled }
    }

    suspend fun updateAutoBlockMalware(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.AUTO_BLOCK_MALWARE] = enabled }
    }

    suspend fun updateBlockAdult(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.BLOCK_ADULT] = enabled }
    }

    suspend fun updateBlockSocial(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.BLOCK_SOCIAL] = enabled }
    }

    suspend fun updateRetentionDays(days: Int) {
        dataStore.edit { prefs -> prefs[SettingsKeys.RETENTION_DAYS] = days }
    }

    suspend fun updateDisableLightweightMonitoring(disable: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.DISABLE_LIGHTWEIGHT_MONITORING] = disable }
    }

    suspend fun updateVpnActive(active: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.VPN_ACTIVE] = active }
    }

    suspend fun updateMonitoringActive(active: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.MONITORING_ACTIVE] = active }
    }

    suspend fun updateProtectionMode(mode: String) {
        dataStore.edit { prefs -> prefs[SettingsKeys.PROTECTION_MODE] = mode }
    }

    // ── Trial de 48 horas ──────────────────────────────────────────────────────

    companion object {
        const val FREE_TRIAL_DURATION_MS = 48L * 60 * 60 * 1_000 // 48 horas en ms
    }

    /** Registra el timestamp de inicio del trial (solo la primera vez). */
    suspend fun setFreeTrialStartTimeIfNotSet() {
        val prefs = dataStore.data.first()
        val yaRegistrado = prefs[SettingsKeys.HAS_USED_FREE_TRIAL] ?: false
        if (!yaRegistrado) {
            dataStore.edit {
                it[SettingsKeys.FREE_TRIAL_START_TIME] = System.currentTimeMillis()
                it[SettingsKeys.HAS_USED_FREE_TRIAL] = true
            }
        }
    }

    /** Devuelve el timestamp de inicio del trial (0 si nunca se estableció). */
    suspend fun getFreeTrialStartTime(): Long =
        dataStore.data.first()[SettingsKeys.FREE_TRIAL_START_TIME] ?: 0L

    /** Devuelve true si el trial de 48h sigue activo. */
    suspend fun isFreeTrialActive(): Boolean {
        val startTime = getFreeTrialStartTime()
        if (startTime == 0L) return true // no inicializado aún — trial activo
        return (System.currentTimeMillis() - startTime) < FREE_TRIAL_DURATION_MS
    }

    /** Horas restantes del trial (0f si ya expiró). */
    suspend fun getFreeTrialRemainingHours(): Float {
        val startTime = getFreeTrialStartTime()
        if (startTime == 0L) return 48f
        val remainingMs = FREE_TRIAL_DURATION_MS - (System.currentTimeMillis() - startTime)
        return if (remainingMs > 0) remainingMs / (60f * 60 * 1_000f) else 0f
    }

    /**
     * Solo disponible en builds DEBUG.
     * Mueve el inicio del trial 49 horas hacia atrás para simular la expiración sin esperar.
     *
     * Uso desde MainActivity (DEBUG únicamente):
     *   lifecycleScope.launch { settingsRepository.simulateTrialExpired() }
     */
    suspend fun simulateTrialExpired() {
        if (!BuildConfig.DEBUG) return
        dataStore.edit {
            it[SettingsKeys.FREE_TRIAL_START_TIME] =
                System.currentTimeMillis() - (49L * 60 * 60 * 1_000)
            it[SettingsKeys.HAS_USED_FREE_TRIAL] = true
        }
    }

    /**
     * Solo disponible en builds DEBUG.
     * Resetea el trial como si fuera primera instalación (para volver a probar el flujo completo).
     */
    suspend fun resetTrialForTesting() {
        if (!BuildConfig.DEBUG) return
        dataStore.edit {
            it.remove(SettingsKeys.FREE_TRIAL_START_TIME)
            it.remove(SettingsKeys.HAS_USED_FREE_TRIAL)
        }
    }
}
