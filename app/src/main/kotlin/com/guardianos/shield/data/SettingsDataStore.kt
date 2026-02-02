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
    val dataRetentionDays: Int = 30
)

object SettingsKeys {
    val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    val AUTO_BLOCK_MALWARE = booleanPreferencesKey("auto_block_malware")
    val BLOCK_ADULT = booleanPreferencesKey("block_adult")
    val BLOCK_SOCIAL = booleanPreferencesKey("block_social")
    val RETENTION_DAYS = intPreferencesKey("retention_days")
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
                dataRetentionDays = prefs[SettingsKeys.RETENTION_DAYS] ?: 30
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
}
