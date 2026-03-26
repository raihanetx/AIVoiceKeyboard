package com.aikeyboard.core.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.aikeyboard.core.constants.AppConstants
import java.io.File
import java.io.IOException

private const val TAG = "AudioRecorder"

/**
 * Audio recording utility class
 * Handles recording audio for speech-to-text processing
 */
class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    /**
     * Start recording audio
     * @return The file where audio is being recorded, or null if recording failed to start
     */
    fun startRecording(): File? {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress")
            return null
        }

        try {
            val fileName = "${context.cacheDir.absolutePath}/voice_${System.currentTimeMillis()}${AppConstants.AUDIO_FILE_EXTENSION}"
            audioFile = File(fileName)

            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(fileName)
                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started: $fileName")
            return audioFile

        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            releaseRecorder()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            releaseRecorder()
            return null
        }
    }

    /**
     * Stop recording and return the recorded audio file
     * @return The recorded audio file, or null if recording failed
     */
    fun stopRecording(): File? {
        if (!isRecording) {
            Log.w(TAG, "No recording in progress")
            return null
        }

        isRecording = false
        val recordedFile = audioFile

        try {
            mediaRecorder?.stop()
            Log.d(TAG, "Recording stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
            recordedFile?.delete()
            audioFile = null
        } finally {
            releaseRecorder()
        }

        return if (recordedFile?.exists() == true && recordedFile.length() > 0) {
            recordedFile
        } else {
            Log.w(TAG, "Recorded file is empty or doesn't exist")
            recordedFile?.delete()
            null
        }
    }

    /**
     * Cancel recording without returning the file
     */
    fun cancelRecording() {
        if (!isRecording) return

        isRecording = false
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder during cancel", e)
        } finally {
            releaseRecorder()
            audioFile?.delete()
            audioFile = null
        }
    }

    /**
     * Check if currently recording
     */
    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * Get current audio file
     */
    fun getCurrentAudioFile(): File? = audioFile

    /**
     * Release the media recorder resources
     */
    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing recorder", e)
        }
        mediaRecorder = null
    }

    /**
     * Create MediaRecorder instance based on Android version
     */
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    /**
     * Clean up resources
     */
    fun release() {
        cancelRecording()
    }

    companion object {
        /**
         * Get MIME type for audio file based on extension
         */
        fun getMimeType(file: File): String {
            return when (file.extension.lowercase()) {
                "3gp" -> "audio/3gpp"
                "mp4", "m4a" -> "audio/mp4"
                "webm" -> "audio/webm"
                "wav" -> "audio/wav"
                "mp3" -> "audio/mpeg"
                else -> "audio/3gpp"
            }
        }
    }
}
