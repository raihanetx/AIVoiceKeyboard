package com.aikeyboard

import android.app.Application
import android.util.Log
import com.aikeyboard.data.local.PreferencesManager

class AIVoiceKeyboardApp : Application() {

    companion object {
        private const val TAG = "AIVoiceKeyboardApp"

        @Volatile
        private var instance: AIVoiceKeyboardApp? = null

        fun getInstance(): AIVoiceKeyboardApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.d(TAG, "=== PixelPro Keyboard Started ===")
        Log.d(TAG, "Version: ${BuildConfig.VERSION_NAME}")
        
        PreferencesManager.getInstance(this)
    }
}
