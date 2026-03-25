package com.aikeyboard.domain.usecase

import android.util.Log
import com.aikeyboard.core.util.AudioRecorder
import com.aikeyboard.domain.model.AudioData
import com.aikeyboard.domain.model.TranscriptionResult
import com.aikeyboard.domain.repository.TranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

private const val TAG = "TranscribeAudioUseCase"

/**
 * Use case for transcribing audio to text
 * 
 * This use case encapsulates the business logic for audio transcription,
 * including audio validation and service selection.
 */
class TranscribeAudioUseCase(
    private val transcriptionRepository: TranscriptionRepository,
    private val audioRecorder: AudioRecorder
) {
    /**
     * Execute transcription from an existing audio file
     * 
     * @param audioFile The audio file to transcribe
     * @param language The language code (e.g., "en", "bn")
     * @return Flow of TranscriptionResult
     */
    suspend operator fun invoke(
        audioFile: File,
        language: String
    ): Flow<TranscriptionResult> = flow {
        emit(TranscriptionResult.Loading)
        
        // Validate audio file
        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file does not exist: ${audioFile.path}")
            emit(TranscriptionResult.Error("Audio file does not exist"))
            return@flow
        }
        
        if (audioFile.length() == 0L) {
            Log.e(TAG, "Audio file is empty: ${audioFile.path}")
            emit(TranscriptionResult.Error("Audio file is empty"))
            return@flow
        }
        
        // Create audio data
        val audioData = AudioData(
            file = audioFile,
            mimeType = AudioRecorder.getMimeType(audioFile),
            language = language
        )
        
        // Delegate to repository
        transcriptionRepository.transcribe(audioData).collect { result ->
            emit(result)
        }
    }

    /**
     * Start recording audio for transcription
     * 
     * @return The file being recorded, or null if recording failed
     */
    fun startRecording(): File? {
        return audioRecorder.startRecording()
    }

    /**
     * Stop recording and transcribe the recorded audio
     * 
     * @param language The language code for transcription
     * @return Flow of TranscriptionResult
     */
    suspend fun stopRecordingAndTranscribe(language: String): Flow<TranscriptionResult> = flow {
        emit(TranscriptionResult.Loading)
        
        val audioFile = audioRecorder.stopRecording()
        
        if (audioFile == null) {
            Log.e(TAG, "Failed to stop recording or no audio captured")
            emit(TranscriptionResult.Error("Failed to capture audio"))
            return@flow
        }
        
        // Now transcribe the recorded audio
        invoke(audioFile, language).collect { result ->
            emit(result)
        }
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = audioRecorder.isCurrentlyRecording()

    /**
     * Cancel current recording
     */
    fun cancelRecording() {
        audioRecorder.cancelRecording()
    }

    /**
     * Check if transcription service is available
     */
    suspend fun isAvailable(): Boolean {
        return transcriptionRepository.isServiceAvailable()
    }
}
