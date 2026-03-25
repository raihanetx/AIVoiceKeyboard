package com.aikeyboard

import android.app.Application

class AiKeyboardApp : Application() {
    
    companion object {
        lateinit var instance: AiKeyboardApp
            private set
        
        // API Keys
        const val GEMINI_API_KEY = "AIzaSyDGLu8GoxbJJi8CCRcj41EkTmYMODo-rrc"
        const val ZAI_API_KEY = "bc4af852307844eda61a4806a37521b0.ODmxaQPLG93RxmI6"
        
        // Endpoints
        const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        const val ZAI_ENDPOINT = "https://api.z.ai/api/paas/v4"
        
        // Models
        const val GEMINI_AUDIO_MODEL = "gemini-1.5-flash-latest"
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
