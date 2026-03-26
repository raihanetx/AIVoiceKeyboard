package com.aikeyboard.core.constants

/**
 * API-related constants
 * Contains endpoints, timeouts, and other API configuration
 */
object ApiConstants {
    // Groq API Configuration
    const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1"
    const val GROQ_WHISPER_MODEL = "whisper-large-v3"
    
    // Gemini API Configuration  
    const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta"
    const val GEMINI_MODEL = "gemini-1.5-flash"
    
    // Z-AI API Configuration  
    const val ZAI_ENDPOINT = "https://api.z.ai/api/paas/v4"
    const val ZAI_MODEL = "glm-4.7-flash"
    
    // Timeouts (in seconds)
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 120L
    const val WRITE_TIMEOUT = 60L
    const val TRANSLATION_TIMEOUT = 60L
    
    // API Response Formats
    const val RESPONSE_FORMAT_JSON = "json"
    
    // Temperature settings
    const val TRANSCRIPTION_TEMPERATURE = 0.0
    const val TRANSLATION_TEMPERATURE = 0.3
    
    // Max tokens
    const val TRANSLATION_MAX_TOKENS = 2048
}
