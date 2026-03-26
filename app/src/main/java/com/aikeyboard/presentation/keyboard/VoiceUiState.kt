package com.aikeyboard.presentation.keyboard

import com.aikeyboard.domain.model.Language

/**
 * UI State for the Voice Input Panel
 *
 * Represents all the state needed to render the voice input UI.
 */
data class VoiceUiState(
    // Selected engine: "android", "groq", or "gemini"
    val selectedEngine: String = "android",

    // API Key status for each engine that requires it
    val groqApiKeyStatus: ApiKeyStatus = ApiKeyStatus.NONE,
    val geminiApiKeyStatus: ApiKeyStatus = ApiKeyStatus.NONE,

    // Recording state
    val isRecording: Boolean = false,

    // Status message to show below mic button
    val statusMessage: String = "Ready to record",

    // Result text after transcription
    val resultText: String? = null,

    // Whether result card should be visible
    val showResult: Boolean = false,

    // Current language for recognition
    val currentLanguage: Language = Language.ENGLISH,

    // Error message if any
    val errorMessage: String? = null,

    // Which engine is showing API key input
    val showApiKeyInputFor: String? = null
) {
    /**
     * Check if Android engine is selected
     */
    val isAndroidSelected: Boolean
        get() = selectedEngine == "android"

    /**
     * Check if Groq engine is selected
     */
    val isGroqSelected: Boolean
        get() = selectedEngine == "groq"

    /**
     * Check if Gemini engine is selected
     */
    val isGeminiSelected: Boolean
        get() = selectedEngine == "gemini"

    /**
     * Check if Groq can be used (has API key or showing input)
     */
    val canUseGroq: Boolean
        get() = groqApiKeyStatus == ApiKeyStatus.SAVED

    /**
     * Check if Gemini can be used (has API key or showing input)
     */
    val canUseGemini: Boolean
        get() = geminiApiKeyStatus == ApiKeyStatus.SAVED

    /**
     * Get engine display name
     */
    fun getEngineDisplayName(): String {
        return when (selectedEngine) {
            "android" -> "Android"
            "groq" -> "Groq"
            "gemini" -> "Gemini"
            else -> "Unknown"
        }
    }

    /**
     * Get engine emoji
     */
    fun getEngineEmoji(): String {
        return when (selectedEngine) {
            "android" -> "🟢"
            "groq" -> "🔵"
            "gemini" -> "🟣"
            else -> "⚪"
        }
    }
}

/**
 * API Key Status enum
 */
enum class ApiKeyStatus {
    NONE,       // No key entered
    SAVED       // Key saved and ready to use
}
