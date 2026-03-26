package com.aikeyboard.data.remote.api

/**
 * Error types for API transcription
 */
object ApiErrorType {
    const val AUTH = "auth"
    const val NETWORK = "network"
    const val TIMEOUT = "timeout"
    const val INVALID_KEY = "invalid_key"
    const val RATE_LIMIT = "rate_limit"
    const val SERVER = "server"
    const val NO_AUDIO = "no_audio"
    const val EMPTY_RESULT = "empty_result"
    const val UNKNOWN = "unknown"
}

/**
 * Result wrapper for transcription API with detailed error info
 */
data class ApiTranscriptionResult(
    val text: String? = null,
    val errorType: String? = null,
    val errorMessage: String? = null,
    val errorDetails: String? = null
) {
    val isSuccess: Boolean get() = text != null
    val isFailure: Boolean get() = errorType != null

    companion object {
        fun success(text: String) = ApiTranscriptionResult(text = text)
        fun error(type: String, message: String, details: String? = null) =
            ApiTranscriptionResult(errorType = type, errorMessage = message, errorDetails = details)
    }
}
