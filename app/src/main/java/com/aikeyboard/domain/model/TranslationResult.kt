package com.aikeyboard.domain.model

/**
 * Domain model for translation result
 */
sealed class TranslationResult {
    data class Success(
        val translatedText: String,
        val sourceLanguage: Language,
        val targetLanguage: Language,
        val confidence: Float = 1.0f
    ) : TranslationResult()

    data class Error(
        val message: String,
        val code: Int? = null
    ) : TranslationResult()

    object Loading : TranslationResult()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    /**
     * Get the translated text, or empty string if not successful
     */
    fun getTextOrNull(): String? {
        return when (this) {
            is Success -> translatedText
            else -> null
        }
    }

    /**
     * Get error message, or null if not an error
     */
    fun getErrorMessageOrNull(): String? {
        return when (this) {
            is Error -> message
            else -> null
        }
    }
}

/**
 * Translation request data
 */
data class TranslationRequest(
    val text: String,
    val sourceLanguage: Language,
    val targetLanguage: Language
) {
    val isValid: Boolean
        get() = text.isNotBlank() && text.length <= 5000
}
