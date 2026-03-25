package com.aikeyboard.domain.repository

import com.aikeyboard.domain.model.AudioData
import com.aikeyboard.domain.model.TranscriptionResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for transcription operations
 * 
 * This interface defines the contract for transcription implementations.
 * The domain layer depends on this abstraction, not concrete implementations.
 */
interface TranscriptionRepository {
    
    /**
     * Transcribe audio file to text
     * 
     * @param audioData The audio data to transcribe
     * @return Flow of TranscriptionResult to track progress
     */
    suspend fun transcribe(audioData: AudioData): Flow<TranscriptionResult>
    
    /**
     * Check if transcription service is available
     */
    suspend fun isServiceAvailable(): Boolean
    
    /**
     * Get supported languages for transcription
     */
    fun getSupportedLanguages(): List<String>
    
    /**
     * Cancel any ongoing transcription
     */
    suspend fun cancelTranscription()
}
