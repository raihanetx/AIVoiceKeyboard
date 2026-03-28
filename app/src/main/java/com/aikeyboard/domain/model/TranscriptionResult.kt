package com.aikeyboard.domain.model

sealed class TranscriptionResult {
    data class Success(
        val text: String,
        val language: String,
        val confidence: Float = 1.0f
    ) : TranscriptionResult()

    data class Error(
        val message: String,
        val code: Int? = null
    ) : TranscriptionResult()

    object Loading : TranscriptionResult()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getTextOrNull(): String? = (this as? Success)?.text
    fun getErrorMessageOrNull(): String? = (this as? Error)?.message
}
