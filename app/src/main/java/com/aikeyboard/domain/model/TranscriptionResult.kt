package com.aikeyboard.domain.model

/**
 * Domain model for transcription result
 */
sealed class TranscriptionResult {
    data class Success(
        val text: String,
        val language: String,
        val confidence: Float = 1.0f,
        val durationMs: Long = 0L
    ) : TranscriptionResult()

    data class Error(
        val message: String,
        val code: Int? = null
    ) : TranscriptionResult()

    object Loading : TranscriptionResult()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    /**
     * Get the transcribed text, or empty string if not successful
     */
    fun getTextOrNull(): String? {
        return when (this) {
            is Success -> text
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
 * Audio data for transcription
 */
data class AudioData(
    val file: java.io.File,
    val mimeType: String,
    val language: String,
    val durationMs: Long = 0L
) {
    val exists: Boolean
        get() = file.exists()

    val size: Long
        get() = file.length()

    val isValid: Boolean
        get() = exists && size > 0
}
