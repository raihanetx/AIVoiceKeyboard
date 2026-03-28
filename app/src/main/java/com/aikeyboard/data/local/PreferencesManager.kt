package com.aikeyboard.data.local

import android.content.Context
import android.util.Log

private const val TAG = "PreferencesManager"

class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "ai_voice_keyboard_prefs"
        private const val PREF_LANGUAGE = "language"
        private const val PREF_GROQ_API_KEY = "groq_api_key"
        private const val LANG_ENGLISH = "en"
        private const val LANG_BENGALI = "bn"
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

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLanguage(): String {
        return sharedPreferences.getString(PREF_LANGUAGE, LANG_ENGLISH) ?: LANG_ENGLISH
    }

    fun setLanguage(language: String) {
        sharedPreferences.edit().putString(PREF_LANGUAGE, language).apply()
        Log.d(TAG, "Language set to: $language")
    }

    fun toggleLanguage(): String {
        val currentLanguage = getLanguage()
        val newLanguage = if (currentLanguage == LANG_ENGLISH) LANG_BENGALI else LANG_ENGLISH
        setLanguage(newLanguage)
        return newLanguage
    }

    fun getGroqApiKey(): String {
        return sharedPreferences.getString(PREF_GROQ_API_KEY, "") ?: ""
    }

    fun setGroqApiKey(key: String) {
        sharedPreferences.edit().putString(PREF_GROQ_API_KEY, key.trim()).apply()
        Log.d(TAG, "Groq API key updated")
    }

    fun isGroqApiKeyConfigured(): Boolean = getGroqApiKey().isNotBlank()

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        Log.d(TAG, "All preferences cleared")
    }

    fun isFirstLaunch(): Boolean {
        return !sharedPreferences.contains(KEY_FIRST_LAUNCH)
    }

    fun markFirstLaunchComplete() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
}
