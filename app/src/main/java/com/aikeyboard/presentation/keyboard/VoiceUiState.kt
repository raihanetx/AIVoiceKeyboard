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

    // Error fields for detailed error display
    val errorTitle: String? = null,
    val errorMessage: String? = null,
    val errorType: String? = null,  // "auth", "network", "timeout", "invalid_key", "rate_limit", "server", "no_audio", "empty_result"
    val errorDetails: String? = null,

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
     * Check if there's an error to display
     */
    val hasError: Boolean
        get() = errorMessage != null || errorType != null

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

    /**
     * Create a copy with error set
     */
    fun withError(
        title: String,
        message: String,
        type: String? = null,
        details: String? = null
    ): VoiceUiState {
        return copy(
            errorTitle = title,
            errorMessage = message,
            errorType = type,
            errorDetails = details,
            isRecording = false
        )
    }

    /**
     * Create a copy with error cleared
     */
    fun clearError(): VoiceUiState {
        return copy(
            errorTitle = null,
            errorMessage = null,
            errorType = null,
            errorDetails = null
        )
    }
}

/**
 * API Key Status enum
 */
enum class ApiKeyStatus {
    NONE,       // No key entered
    SAVED       // Key saved and ready to use
}

/**
 * Error types for categorizing errors
 */
object ErrorType {
    const val AUTH = "auth"              // Authentication failed
    const val NETWORK = "network"        // Network connection error
    const val TIMEOUT = "timeout"        // Request timed out
    const val INVALID_KEY = "invalid_key" // Invalid API key
    const val RATE_LIMIT = "rate_limit"  // Rate limit exceeded
    const val SERVER = "server"          // Server error
    const val NO_AUDIO = "no_audio"      // No audio recorded
    const val EMPTY_RESULT = "empty_result" // No speech detected
    const val PERMISSION = "permission"  // Permission denied
    const val UNKNOWN = "unknown"        // Unknown error
}
