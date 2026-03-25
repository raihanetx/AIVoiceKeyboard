package com.aikeyboard

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

class AiKeyboardApp : Application() {
    companion object {
        lateinit var instance: AiKeyboardApp
            private set

        const val GEMINI_API_KEY = "AIzaSyDGLu8GoxbJJi8CCRcj41EkTmYMODo-rrc"
        const val ZAI_API_KEY = "bc4af852307844eda61a4806a37521b0.ODmxaQPLG93RxmI6"
        const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        const val ZAI_ENDPOINT = "https://api.z.ai/api/paas/v4"
        const val GEMINI_AUDIO_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keyboard_settings")
    val keyboardDataStore: DataStore<Preferences> get() = dataStore

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
