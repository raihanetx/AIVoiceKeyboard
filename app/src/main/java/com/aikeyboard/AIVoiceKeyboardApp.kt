package com.aikeyboard

import android.app.Application
import android.util.Log
import com.aikeyboard.core.security.Secrets
import com.aikeyboard.data.local.PreferencesManager

/**
 * AI Voice Keyboard Application
 * 
 * Main application class that handles initialization and provides
 * access to application-wide resources.
 */
class AIVoiceKeyboardApp : Application() {

    companion object {
        private const val TAG = "AIVoiceKeyboardApp"

        @Volatile
        private var instance: AIVoiceKeyboardApp? = null

        /**
         * Get the application instance
         */
        fun getInstance(): AIVoiceKeyboardApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }

        /**
         * Check if Groq API is configured
         */
        fun isGroqConfigured(): Boolean = Secrets.isGroqApiKeyConfigured()

        /**
         * Check if Z-AI API is configured
         */
        fun isZaiConfigured(): Boolean = Secrets.isZaiApiKeyConfigured()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.d(TAG, "=== AI Voice Keyboard App Started ===")
        Log.d(TAG, "Version: ${BuildConfig.VERSION_NAME}")
        
        // Initialize preferences
        PreferencesManager.getInstance(this)
        
        // Log API configuration status
        Log.d(TAG, "Groq API configured: ${isGroqConfigured()}")
        Log.d(TAG, "Z-AI API configured: ${isZaiConfigured()}")
    }

    override fun onTerminate() {
        Log.d(TAG, "=== AI Voice Keyboard App Terminated ===")
        super.onTerminate()
    }
}
