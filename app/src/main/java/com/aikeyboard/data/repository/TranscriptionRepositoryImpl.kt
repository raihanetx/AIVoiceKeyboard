package com.aikeyboard.data.repository

import com.aikeyboard.data.remote.api.GroqWhisperApi
import com.aikeyboard.domain.model.AudioData
import com.aikeyboard.domain.model.TranscriptionResult
import com.aikeyboard.domain.repository.TranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementation of TranscriptionRepository using Groq Whisper API
 */
class TranscriptionRepositoryImpl(
    private val groqWhisperApi: GroqWhisperApi = GroqWhisperApi.instance
) : TranscriptionRepository {

    override suspend fun transcribe(audioData: AudioData): Flow<TranscriptionResult> = flow {
        emit(TranscriptionResult.Loading)
        
        if (!audioData.isValid) {
            emit(TranscriptionResult.Error("Invalid audio data"))
            return@flow
        }

        val result = groqWhisperApi.transcribe(
            audioFile = audioData.file,
            language = audioData.language
        )

        result.fold(
            onSuccess = { text ->
                emit(TranscriptionResult.Success(
                    text = text,
                    language = audioData.language
                ))
            },
            onFailure = { error ->
                emit(TranscriptionResult.Error(
                    message = error.message ?: "Transcription failed"
                ))
            }
        )
    }

    override suspend fun isServiceAvailable(): Boolean {
        return groqWhisperApi.isConfigured()
    }

    override fun getSupportedLanguages(): List<String> {
        return groqWhisperApi.getSupportedLanguages()
    }

    override suspend fun cancelTranscription() {
        // Currently no cancellation support in the API client
        // Could be implemented with OkHttp call cancellation
    }
}
