package com.aikeyboard.data.local

import android.content.Context
import android.util.Log
import com.aikeyboard.core.constants.AppConstants

private const val TAG = "PreferencesManager"

/**
 * Manages application preferences using SharedPreferences
 *
 * Provides a clean interface for storing and retrieving user preferences.
 */
class PreferencesManager(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        AppConstants.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ==================== STT Engine Preferences ====================

    /**
     * Get the current STT engine
     */
    fun getSttEngine(): String {
        return sharedPreferences.getString(AppConstants.PREF_STT_ENGINE, AppConstants.STT_ENGINE_ANDROID)
            ?: AppConstants.STT_ENGINE_ANDROID
    }

    /**
     * Set the STT engine
     * Uses commit() for synchronous saving to ensure immediate availability
     */
    fun setSttEngine(engine: String): Boolean {
        val validEngine = when (engine) {
            AppConstants.STT_ENGINE_ANDROID, AppConstants.STT_ENGINE_GROQ, AppConstants.STT_ENGINE_GEMINI -> engine
            else -> AppConstants.STT_ENGINE_ANDROID
        }
        val success = sharedPreferences.edit()
            .putString(AppConstants.PREF_STT_ENGINE, validEngine)
            .commit()  // Synchronous save!
        Log.d(TAG, "STT engine set to: $validEngine, success=$success")
        return success
    }

    // ==================== Language Preferences ====================

    /**
     * Get the current language code
     */
    fun getLanguage(): String {
        return sharedPreferences.getString(AppConstants.PREF_LANGUAGE, AppConstants.LANG_ENGLISH)
            ?: AppConstants.LANG_ENGLISH
    }

    /**
     * Set the current language
     */
    fun setLanguage(language: String) {
        sharedPreferences.edit()
            .putString(AppConstants.PREF_LANGUAGE, language)
            .apply()
        Log.d(TAG, "Language set to: $language")
    }

    // ==================== Panel Preferences ====================

    /**
     * Get the last active panel
     */
    fun getLastPanel(): String {
        return sharedPreferences.getString(AppConstants.PREF_LAST_PANEL, AppConstants.PANEL_KEYBOARD)
            ?: AppConstants.PANEL_KEYBOARD
    }

    /**
     * Set the last active panel
     */
    fun setLastPanel(panel: String) {
        sharedPreferences.edit()
            .putString(AppConstants.PREF_LAST_PANEL, panel)
            .apply()
        Log.d(TAG, "Last panel set to: $panel")
    }

    // ==================== API Key Preferences ====================

    /**
     * Get Groq API key
     */
    fun getGroqApiKey(): String {
        return sharedPreferences.getString(AppConstants.PREF_GROQ_API_KEY, "") ?: ""
    }

    /**
     * Set Groq API key
     * Uses commit() for synchronous saving to ensure immediate availability
     */
    fun setGroqApiKey(key: String): Boolean {
        val trimmedKey = key.trim()
        val success = sharedPreferences.edit()
            .putString(AppConstants.PREF_GROQ_API_KEY, trimmedKey)
            .commit()  // Synchronous save!
        Log.d(TAG, "Groq API key saved: ${if (trimmedKey.isNotBlank()) "***saved***" else "empty"}, success=$success")
        return success
    }

    /**
     * Check if Groq API key is configured
     * Returns true if user has saved a key OR we have fallback
     */
    fun hasGroqApiKey(): Boolean {
        val savedKey = getGroqApiKey().trim()
        return savedKey.isNotEmpty()
    }

    /**
     * Clear Groq API key
     */
    fun clearGroqApiKey() {
        sharedPreferences.edit()
            .remove(AppConstants.PREF_GROQ_API_KEY)
            .apply()
        Log.d(TAG, "Groq API key cleared")
    }

    /**
     * Get Gemini API key
     */
    fun getGeminiApiKey(): String {
        return sharedPreferences.getString(AppConstants.PREF_GEMINI_API_KEY, "") ?: ""
    }

    /**
     * Set Gemini API key
     * Uses commit() for synchronous saving to ensure immediate availability
     */
    fun setGeminiApiKey(key: String): Boolean {
        val trimmedKey = key.trim()
        val success = sharedPreferences.edit()
            .putString(AppConstants.PREF_GEMINI_API_KEY, trimmedKey)
            .commit()  // Synchronous save!
        Log.d(TAG, "Gemini API key saved: ${if (trimmedKey.isNotBlank()) "***saved***" else "empty"}, success=$success")
        return success
    }

    /**
     * Check if Gemini API key is configured
     */
    fun hasGeminiApiKey(): Boolean = getGeminiApiKey().isNotBlank()

    /**
     * Clear Gemini API key
     */
    fun clearGeminiApiKey() {
        sharedPreferences.edit()
            .remove(AppConstants.PREF_GEMINI_API_KEY)
            .apply()
        Log.d(TAG, "Gemini API key cleared")
    }

    // ==================== Utility Methods ====================

    /**
     * Clear all preferences
     */
    fun clearAll() {
        sharedPreferences.edit()
            .clear()
            .apply()
        Log.d(TAG, "All preferences cleared")
    }

    companion object {
        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
