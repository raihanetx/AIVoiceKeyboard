package com.aikeyboard.core.constants

/**
 * Application-wide constants
 * Contains UI, feature flags, and general app configuration
 */
object AppConstants {
    // App Info
    const val APP_NAME = "AI Voice Keyboard"
    const val KEYBOARD_SERVICE_CLASS = "com.aikeyboard.presentation.keyboard.AiKeyboardService"
    
    // UI Constants
    const val KEYBOARD_ROW_HEIGHT_DP = 44
    const val KEYBOARD_KEY_MARGIN_DP = 2
    const val MIC_BUTTON_SIZE_DP = 70
    
    // Language Codes
    const val LANG_ENGLISH = "en"
    const val LANG_BENGALI = "bn"
    const val LANG_ENGLISH_FULL = "en-US"
    const val LANG_BENGALI_FULL = "bn-BD"
    
    // STT Engines
    const val STT_ENGINE_ANDROID = "android"
    const val STT_ENGINE_GROQ = "groq"
    const val STT_ENGINE_GEMINI = "gemini"
    
    // STT Engine Display Names
    const val STT_ENGINE_ANDROID_NAME = "Android (Offline)"
    const val STT_ENGINE_GROQ_NAME = "Groq (Online)"
    const val STT_ENGINE_GEMINI_NAME = "Gemini (Live)"
    
    // Panel Types
    const val PANEL_KEYBOARD = "keyboard"
    const val PANEL_VOICE = "voice"
    const val PANEL_TRANSLATE = "translate"
    const val PANEL_EMOJI = "emoji"
    
    // Audio Recording
    const val AUDIO_FILE_EXTENSION = ".3gp"
    const val MAX_TEXT_LENGTH_FOR_TRANSLATION = 5000
    
    // SharedPreferences Keys
    const val PREFS_NAME = "ai_keyboard_prefs"
    const val PREF_LANGUAGE = "pref_language"
    const val PREF_STT_ENGINE = "pref_stt_engine"
    const val PREF_LAST_PANEL = "pref_last_panel"
    
    // API Key SharedPreferences Keys
    const val PREF_GROQ_API_KEY = "pref_groq_api_key"
    const val PREF_GEMINI_API_KEY = "pref_gemini_api_key"
    
    // Animation Durations (ms)
    const val ANIMATION_DURATION_SHORT = 200
    const val ANIMATION_DURATION_MEDIUM = 300
    const val ANIMATION_DURATION_LONG = 500
    
    // Periodic Check Interval (ms)
    const val STATE_CHECK_INTERVAL = 500L
}
