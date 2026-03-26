package com.aikeyboard.data.repository

import com.aikeyboard.data.remote.api.ApiTranscriptionResult
import com.aikeyboard.data.remote.api.GeminiLiveApi
import com.aikeyboard.data.remote.api.GroqWhisperApi
import com.aikeyboard.domain.model.AudioData
import com.aikeyboard.domain.model.TranscriptionResult
import com.aikeyboard.domain.repository.TranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementation of TranscriptionRepository using Groq Whisper API and Gemini Live API
 */
class TranscriptionRepositoryImpl(
    private val groqWhisperApi: GroqWhisperApi,
    private val geminiLiveApi: GeminiLiveApi
) : TranscriptionRepository {

    override suspend fun transcribe(audioData: AudioData): Flow<TranscriptionResult> = flow {
        emit(TranscriptionResult.Loading)

        if (!audioData.isValid) {
            emit(TranscriptionResult.Error("Invalid audio data"))
            return@flow
        }

        val result = groqWhisperApi.transcribe(audioData.file, audioData.language)

        if (result.isSuccess && result.text != null) {
            emit(TranscriptionResult.Success(text = result.text, language = audioData.language))
        } else {
            emit(TranscriptionResult.Error(message = result.errorMessage ?: "Transcription failed"))
        }
    }

    override suspend fun isServiceAvailable(): Boolean = groqWhisperApi.isConfigured()

    override fun getSupportedLanguages(): List<String> = groqWhisperApi.getSupportedLanguages()

    override suspend fun cancelTranscription() {}
}
