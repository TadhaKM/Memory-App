package com.recall.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences stored in DataStore
 */
data class UserPreferences(
    // Onboarding
    val hasSeenOnboarding: Boolean = false,

    // Appearance
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,

    // Sync
    val autoSync: Boolean = true,
    val syncOnWifiOnly: Boolean = false,

    // AI
    val enableAiProcessing: Boolean = true,
    val enableTranscription: Boolean = true,
    val enableOcr: Boolean = true,

    // Daily Recall
    val enableDailyRecall: Boolean = true,
    val dailyRecallCount: Int = 5,
    val dailyRecallTime: String = "09:00", // HH:mm format

    // Notifications
    val enableNotifications: Boolean = true,

    // Privacy
    val enableAnalytics: Boolean = true,
    val enableCrashReporting: Boolean = true,

    // Export
    val defaultExportFormat: ExportFormatOption = ExportFormatOption.MARKDOWN
)

enum class DarkModeOption {
    LIGHT, DARK, SYSTEM
}

enum class ExportFormatOption {
    TXT, MARKDOWN
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val SYNC_ON_WIFI_ONLY = booleanPreferencesKey("sync_on_wifi_only")
        val ENABLE_AI_PROCESSING = booleanPreferencesKey("enable_ai_processing")
        val ENABLE_TRANSCRIPTION = booleanPreferencesKey("enable_transcription")
        val ENABLE_OCR = booleanPreferencesKey("enable_ocr")
        val ENABLE_DAILY_RECALL = booleanPreferencesKey("enable_daily_recall")
        val DAILY_RECALL_COUNT = intPreferencesKey("daily_recall_count")
        val DAILY_RECALL_TIME = stringPreferencesKey("daily_recall_time")
        val ENABLE_NOTIFICATIONS = booleanPreferencesKey("enable_notifications")
        val ENABLE_ANALYTICS = booleanPreferencesKey("enable_analytics")
        val ENABLE_CRASH_REPORTING = booleanPreferencesKey("enable_crash_reporting")
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading preferences")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }

    private fun mapPreferences(preferences: Preferences): UserPreferences {
        return UserPreferences(
            hasSeenOnboarding = preferences[PreferencesKeys.HAS_SEEN_ONBOARDING] ?: false,
            darkMode = preferences[PreferencesKeys.DARK_MODE]?.let {
                try { DarkModeOption.valueOf(it) } catch (e: Exception) { DarkModeOption.SYSTEM }
            } ?: DarkModeOption.SYSTEM,
            autoSync = preferences[PreferencesKeys.AUTO_SYNC] ?: true,
            syncOnWifiOnly = preferences[PreferencesKeys.SYNC_ON_WIFI_ONLY] ?: false,
            enableAiProcessing = preferences[PreferencesKeys.ENABLE_AI_PROCESSING] ?: true,
            enableTranscription = preferences[PreferencesKeys.ENABLE_TRANSCRIPTION] ?: true,
            enableOcr = preferences[PreferencesKeys.ENABLE_OCR] ?: true,
            enableDailyRecall = preferences[PreferencesKeys.ENABLE_DAILY_RECALL] ?: true,
            dailyRecallCount = preferences[PreferencesKeys.DAILY_RECALL_COUNT] ?: 5,
            dailyRecallTime = preferences[PreferencesKeys.DAILY_RECALL_TIME] ?: "09:00",
            enableNotifications = preferences[PreferencesKeys.ENABLE_NOTIFICATIONS] ?: true,
            enableAnalytics = preferences[PreferencesKeys.ENABLE_ANALYTICS] ?: true,
            enableCrashReporting = preferences[PreferencesKeys.ENABLE_CRASH_REPORTING] ?: true,
            defaultExportFormat = preferences[PreferencesKeys.DEFAULT_EXPORT_FORMAT]?.let {
                try { ExportFormatOption.valueOf(it) } catch (e: Exception) { ExportFormatOption.MARKDOWN }
            } ?: ExportFormatOption.MARKDOWN
        )
    }

    suspend fun updateHasSeenOnboarding(seen: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_ONBOARDING] = seen
        }
    }

    suspend fun updateDarkMode(darkMode: DarkModeOption) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = darkMode.name
        }
    }

    suspend fun updateAutoSync(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SYNC] = enabled
        }
    }

    suspend fun updateSyncOnWifiOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_ON_WIFI_ONLY] = enabled
        }
    }

    suspend fun updateEnableAiProcessing(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_AI_PROCESSING] = enabled
        }
    }

    suspend fun updateEnableTranscription(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_TRANSCRIPTION] = enabled
        }
    }

    suspend fun updateEnableOcr(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_OCR] = enabled
        }
    }

    suspend fun updateEnableDailyRecall(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_DAILY_RECALL] = enabled
        }
    }

    suspend fun updateDailyRecallCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_RECALL_COUNT] = count.coerceIn(1, 10)
        }
    }

    suspend fun updateDailyRecallTime(time: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_RECALL_TIME] = time
        }
    }

    suspend fun updateEnableNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_NOTIFICATIONS] = enabled
        }
    }

    suspend fun updateEnableAnalytics(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_ANALYTICS] = enabled
        }
    }

    suspend fun updateEnableCrashReporting(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_CRASH_REPORTING] = enabled
        }
    }

    suspend fun updateDefaultExportFormat(format: ExportFormatOption) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_EXPORT_FORMAT] = format.name
        }
    }

    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
