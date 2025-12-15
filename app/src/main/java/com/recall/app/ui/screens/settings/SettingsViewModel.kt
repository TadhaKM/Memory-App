package com.recall.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recall.app.BuildConfig
import com.recall.app.data.preferences.DarkModeOption
import com.recall.app.data.preferences.ExportFormatOption
import com.recall.app.data.preferences.UserPreferences
import com.recall.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { preferences ->
                _uiState.update {
                    it.copy(
                        preferences = preferences,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateDarkMode(option: DarkModeOption) {
        viewModelScope.launch {
            userPreferencesRepository.updateDarkMode(option)
        }
    }

    fun updateAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateAutoSync(enabled)
        }
    }

    fun updateSyncOnWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateSyncOnWifiOnly(enabled)
        }
    }

    fun updateEnableAiProcessing(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateEnableAiProcessing(enabled)
        }
    }

    fun updateEnableTranscription(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateEnableTranscription(enabled)
        }
    }

    fun updateEnableOcr(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateEnableOcr(enabled)
        }
    }

    fun updateEnableDailyRecall(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateEnableDailyRecall(enabled)
        }
    }

    fun updateDailyRecallCount(count: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateDailyRecallCount(count)
        }
    }

    fun updateEnableNotifications(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateEnableNotifications(enabled)
        }
    }

    fun updateEnableAnalytics(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateEnableAnalytics(enabled)
        }
    }

    fun updateEnableCrashReporting(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateEnableCrashReporting(enabled)
        }
    }

    fun updateDefaultExportFormat(format: ExportFormatOption) {
        viewModelScope.launch {
            userPreferencesRepository.updateDefaultExportFormat(format)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            userPreferencesRepository.clearAllPreferences()
            _uiState.update { it.copy(showClearDataSuccess = true) }
        }
    }

    fun dismissClearDataSuccess() {
        _uiState.update { it.copy(showClearDataSuccess = false) }
    }
}

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val showClearDataSuccess: Boolean = false,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val buildNumber: Int = BuildConfig.VERSION_CODE
)
