package com.aikeyboard

import android.app.Application

class AiKeyboardApp : Application() {

    companion object {
        lateinit var instance: AiKeyboardApp
            private set

        // ==================== API KEYS ====================
        val GROQ_API_KEY: String = Secrets.GROQ_API_KEY
        const val ZAI_API_KEY = "bc4af852307844eda61a4806a37521b0.ODmxaQPLG93RxmI6"

        // ==================== ENDPOINTS ====================
        const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1"
        const val ZAI_ENDPOINT = "https://api.z.ai/api/paas/v4"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
