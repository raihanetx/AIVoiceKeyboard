package com.aikeyboard.data.local

import android.content.Context
import android.util.Log
import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.core.constants.AppConstants.PREF_LANGUAGE
import com.aikeyboard.core.constants.AppConstants.PREF_LAST_PANEL
import com.aikeyboard.core.constants.AppConstants.PREF_STT_ENGINE
import com.aikeyboard.core.constants.AppConstants.PREF_GROQ_API_KEY
import com.aikeyboard.core.constants.AppConstants.PREF_GEMINI_API_KEY
import com.aikeyboard.core.constants.AppConstants.STT_ENGINE_ANDROID
import com.aikeyboard.core.constants.AppConstants.STT_ENGINE_GROQ
import com.aikeyboard.core.constants.AppConstants.STT_ENGINE_GEMINI
import com.aikeyboard.core.constants.AppConstants.LANG_ENGLISH
import com.aikeyboard.core.constants.AppConstants.LANG_BENGALI
import com.aikeyboard.core.constants.AppConstants.PANEL_KEYBOARD

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

    // ==================== Language Preferences ====================

    /**
     * Get the current language code
     */
    fun getLanguage(): String {
        return sharedPreferences.getString(PREF_LANGUAGE, LANG_ENGLISH) ?: LANG_ENGLISH
    }

    /**
     * Set the current language
     */
    fun setLanguage(language: String) {
        sharedPreferences.edit()
            .putString(PREF_LANGUAGE, language)
            .apply()
        Log.d(TAG, "Language set to: $language")
    }

    /**
     * Toggle between English and Bengali
     */
    fun toggleLanguage(): String {
        val currentLanguage = getLanguage()
        val newLanguage = if (currentLanguage == LANG_ENGLISH) LANG_BENGALI else LANG_ENGLISH
        setLanguage(newLanguage)
        return newLanguage
    }

    // ==================== STT Engine Preferences ====================

    /**
     * Get the current STT engine
     */
    fun getSttEngine(): String {
        return sharedPreferences.getString(PREF_STT_ENGINE, STT_ENGINE_ANDROID) ?: STT_ENGINE_ANDROID
    }

    /**
     * Set the STT engine
     */
    fun setSttEngine(engine: String) {
        val validEngine = when (engine) {
            STT_ENGINE_ANDROID, STT_ENGINE_GROQ, STT_ENGINE_GEMINI -> engine
            else -> STT_ENGINE_ANDROID
        }
        sharedPreferences.edit()
            .putString(PREF_STT_ENGINE, validEngine)
            .apply()
        Log.d(TAG, "STT engine set to: $validEngine")
    }

    /**
     * Check if using Android STT
     */
    fun isUsingAndroidStt(): Boolean = getSttEngine() == STT_ENGINE_ANDROID

    /**
     * Check if using Groq STT
     */
    fun isUsingGroqStt(): Boolean = getSttEngine() == STT_ENGINE_GROQ

    /**
     * Check if using Gemini STT
     */
    fun isUsingGeminiStt(): Boolean = getSttEngine() == STT_ENGINE_GEMINI

    // ==================== Panel Preferences ====================

    /**
     * Get the last active panel
     */
    fun getLastPanel(): String {
        return sharedPreferences.getString(PREF_LAST_PANEL, PANEL_KEYBOARD) ?: PANEL_KEYBOARD
    }

    /**
     * Set the last active panel
     */
    fun setLastPanel(panel: String) {
        sharedPreferences.edit()
            .putString(PREF_LAST_PANEL, panel)
            .apply()
        Log.d(TAG, "Last panel set to: $panel")
    }

    // ==================== API Key Preferences ====================

    /**
     * Get Groq API key
     */
    fun getGroqApiKey(): String {
        return sharedPreferences.getString(PREF_GROQ_API_KEY, "") ?: ""
    }

    /**
     * Set Groq API key
     */
    fun setGroqApiKey(key: String) {
        sharedPreferences.edit()
            .putString(PREF_GROQ_API_KEY, key.trim())
            .apply()
        Log.d(TAG, "Groq API key updated")
    }

    /**
     * Check if Groq API key is configured
     */
    fun isGroqApiKeyConfigured(): Boolean = getGroqApiKey().isNotBlank()

    /**
     * Get Gemini API key
     */
    fun getGeminiApiKey(): String {
        return sharedPreferences.getString(PREF_GEMINI_API_KEY, "") ?: ""
    }

    /**
     * Set Gemini API key
     */
    fun setGeminiApiKey(key: String) {
        sharedPreferences.edit()
            .putString(PREF_GEMINI_API_KEY, key.trim())
            .apply()
        Log.d(TAG, "Gemini API key updated")
    }

    /**
     * Check if Gemini API key is configured
     */
    fun isGeminiApiKeyConfigured(): Boolean = getGeminiApiKey().isNotBlank()

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

    /**
     * Check if this is the first launch
     */
    fun isFirstLaunch(): Boolean {
        return !sharedPreferences.contains(KEY_FIRST_LAUNCH)
    }

    /**
     * Mark first launch as complete
     */
    fun markFirstLaunchComplete() {
        sharedPreferences.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }

    companion object {
        private const val KEY_FIRST_LAUNCH = "first_launch"

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
