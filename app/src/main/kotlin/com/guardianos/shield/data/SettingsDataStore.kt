package com.guardianos.shield.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
    val protectionMode: String = "Recommended"  // "Recommended", "Advanced", "CustomStats"
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
                protectionMode = prefs[SettingsKeys.PROTECTION_MODE] ?: "Recommended"
            )
        }

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
}
