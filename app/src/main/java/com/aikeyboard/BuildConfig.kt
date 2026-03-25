package com.aikeyboard

/**
 * Build configuration constants
 * 
 * Contains version and build information.
 */
object BuildConfig {
    const val VERSION_NAME = "3.0.0"
    const val VERSION_CODE = 30
    
    // Build types
    const val DEBUG = true
    const val RELEASE = false
    
    // Feature flags
    const val FEATURE_VOICE_INPUT = true
    const val FEATURE_TRANSLATION = true
    const val FEATURE_EMOJI_PANEL = true
    
    // API Configuration
    const val GROQ_ENABLED = true
    const val ZAI_ENABLED = true
}
