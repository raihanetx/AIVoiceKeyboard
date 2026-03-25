package com.aikeyboard.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aikeyboard.data.local.PreferencesManager
import com.aikeyboard.domain.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for settings screen
 * 
 * Manages the UI state for the settings activity.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager.getInstance(application)

    // UI State
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    /**
     * Load saved preferences
     */
    private fun loadPreferences() {
        viewModelScope.launch {
            val language = preferencesManager.getLanguage()
            val sttEngine = preferencesManager.getSttEngine()
            val lastPanel = preferencesManager.getLastPanel()
            val groqApiKey = preferencesManager.getGroqApiKey()
            val geminiApiKey = preferencesManager.getGeminiApiKey()

            _uiState.value = _uiState.value.copy(
                selectedLanguage = Language.fromCode(language),
                sttEngine = sttEngine,
                lastPanel = lastPanel,
                groqApiKey = groqApiKey,
                geminiApiKey = geminiApiKey,
                groqKeyConfigured = groqApiKey.isNotBlank(),
                geminiKeyConfigured = geminiApiKey.isNotBlank()
            )
        }
    }

    /**
     * Set the current language
     */
    fun setLanguage(language: Language) {
        preferencesManager.setLanguage(language.code)
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    /**
     * Set the STT engine
     */
    fun setSttEngine(engine: String) {
        preferencesManager.setSttEngine(engine)
        _uiState.value = _uiState.value.copy(sttEngine = engine)
    }

    /**
     * Set Groq API key
     */
    fun setGroqApiKey(key: String) {
        preferencesManager.setGroqApiKey(key)
        _uiState.value = _uiState.value.copy(
            groqApiKey = key,
            groqKeyConfigured = key.isNotBlank()
        )
    }

    /**
     * Set Gemini API key
     */
    fun setGeminiApiKey(key: String) {
        preferencesManager.setGeminiApiKey(key)
        _uiState.value = _uiState.value.copy(
            geminiApiKey = key,
            geminiKeyConfigured = key.isNotBlank()
        )
    }

    /**
     * Save the last active panel
     */
    fun setLastPanel(panel: String) {
        preferencesManager.setLastPanel(panel)
        _uiState.value = _uiState.value.copy(lastPanel = panel)
    }

    /**
     * Reset all settings to default
     */
    fun resetSettings() {
        preferencesManager.clearAll()
        loadPreferences()
    }
}

/**
 * UI State for settings screen
 */
data class SettingsUiState(
    val selectedLanguage: Language = Language.ENGLISH,
    val sttEngine: String = "android",
    val lastPanel: String = "keyboard",
    val groqApiKey: String = "",
    val geminiApiKey: String = "",
    val groqKeyConfigured: Boolean = false,
    val geminiKeyConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
