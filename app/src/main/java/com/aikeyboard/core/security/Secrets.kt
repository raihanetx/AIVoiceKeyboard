package com.aikeyboard.core.security

/**
 * API keys and secrets
 * 
 * IMPORTANT: This file should NOT be committed to version control with real API keys!
 * Copy Secrets.kt.template to Secrets.kt and add your keys there.
 * 
 * Get your FREE Groq API key from: https://console.groq.com
 * Get your FREE Gemini API key from: https://aistudio.google.com/apikey
 */
object Secrets {
    // Groq API Key - Get from https://console.groq.com
    const val GROQ_API_KEY: String = ""
    
    // Gemini API Key - Get from https://aistudio.google.com/apikey
    // Free tier: 15 RPM, 1 million tokens/day
    const val GEMINI_API_KEY: String = ""
    
    // Z-AI API Key
    const val ZAI_API_KEY: String = "bc4af852307844eda61a4806a37521b0.ODmxaQPLG93RxmI6"
    
    /**
     * Check if Groq API key is configured
     */
    fun isGroqApiKeyConfigured(): Boolean = GROQ_API_KEY.isNotBlank()
    
    /**
     * Check if Gemini API key is configured
     */
    fun isGeminiApiKeyConfigured(): Boolean = GEMINI_API_KEY.isNotBlank()
    
    /**
     * Check if Z-AI API key is configured
     */
    fun isZaiApiKeyConfigured(): Boolean = ZAI_API_KEY.isNotBlank()
}
