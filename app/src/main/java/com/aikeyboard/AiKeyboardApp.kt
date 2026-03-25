package com.aikeyboard

import android.app.Application

class AiKeyboardApp : Application() {
    
    companion object {
        lateinit var instance: AiKeyboardApp
            private set
        
        // API Keys - UPDATE GEMINI_KEY with your new key from https://aistudio.google.com/app/apikey
        // The previous key was blocked due to being leaked
        const val GEMINI_API_KEY = "YOUR_NEW_GEMINI_API_KEY_HERE"
        const val ZAI_API_KEY = "bc4af852307844eda61a4806a37521b0.ODmxaQPLG93RxmI6"
        
        // Endpoints
        const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        const val ZAI_ENDPOINT = "https://api.z.ai/api/paas/v4"
        
        // Models
        // For audio transcription, use gemini-1.5-flash or gemini-2.0-flash-exp
        // Both support audio input via generateContent API
        const val GEMINI_AUDIO_MODEL = "gemini-1.5-flash-latest"
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
