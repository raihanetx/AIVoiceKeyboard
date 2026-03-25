package com.aikeyboard

import android.app.Application

class AiKeyboardApp : Application() {

    companion object {
        lateinit var instance: AiKeyboardApp
            private set

        // ==================== API KEYS ====================
        // Get your Groq API key from: https://console.groq.com (FREE, ~10,000 requests/day)
        const val GROQ_API_KEY = ""  // <-- ADD YOUR GROQ API KEY HERE

        // z-ai API key for translation (already provided)
        const val ZAI_API_KEY = "bc4af852307844eda61a4806a37521b0.ODmxaQPLG93RxmI6"

        // ==================== ENDPOINTS ====================
        const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1"
        const val ZAI_ENDPOINT = "https://api.z.ai/api/paas/v4"

        // ==================== SETTINGS ====================
        // Default STT engine: "groq" or "android"
        var defaultSTTEngine = "groq"

        // Default language: "en" or "bn"
        var defaultLanguage = "en"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
