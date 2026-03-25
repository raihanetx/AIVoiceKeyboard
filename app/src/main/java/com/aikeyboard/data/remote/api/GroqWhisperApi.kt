package com.aikeyboard.data.remote.api

import android.content.Context
import android.util.Log
import com.aikeyboard.core.constants.ApiConstants
import com.aikeyboard.data.local.PreferencesManager
import com.aikeyboard.data.remote.dto.TranscriptionError
import com.aikeyboard.data.remote.dto.TranscriptionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "GroqWhisperApi"

/**
 * Groq Whisper API client for speech-to-text transcription
 * 
 * Fast, accurate speech-to-text using Groq's Whisper Large V3 model.
 * Free tier: ~10,000 requests/day
 * Get API key from: https://console.groq.com
 */
class GroqWhisperApi(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConstants.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(context) }

    /**
     * Transcribe audio file to text
     * 
     * @param audioFile The audio file to transcribe
     * @param language The language code (e.g., "en", "bn")
     * @return Result containing the transcribed text or an error
     */
    suspend fun transcribe(
        audioFile: File,
        language: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate file
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file does not exist"))
            }
            if (audioFile.length() == 0L) {
                return@withContext Result.failure(Exception("Audio file is empty"))
            }

            // Check API key from PreferencesManager
            val apiKey = preferencesManager.getGroqApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Groq API key not configured. Go to Settings > API Keys to add your key. Get free key from https://console.groq.com"))
            }

            val audioBytes = audioFile.readBytes()
            val mimeType = getMimeType(audioFile)

            Log.d(TAG, "Transcribing: ${audioFile.name}, mimeType: $mimeType, size: ${audioFile.length()}")

            // Build multipart form request
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", ApiConstants.GROQ_WHISPER_MODEL)
                .addFormDataPart("language", if (language == "bn") "bn" else "en")
                .addFormDataPart("response_format", "json")
                .addFormDataPart("temperature", "0.0")
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioBytes.toRequestBody(mimeType.toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("${ApiConstants.GROQ_ENDPOINT}/audio/transcriptions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                
                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val transcriptionResponse = TranscriptionResponse.fromJson(bodyString)
                    
                    if (transcriptionResponse.isValid) {
                        Log.d(TAG, "Transcription success: ${transcriptionResponse.text.take(50)}...")
                        Result.success(transcriptionResponse.text.trim())
                    } else {
                        Result.failure(Exception("Empty transcription"))
                    }
                } else {
                    val errorMessage = parseError(response.code, bodyString)
                    Log.e(TAG, "API Error: ${response.code} - $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timed out", e)
            Result.failure(Exception("Request timed out. Please try again."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "No internet connection", e)
            Result.failure(Exception("No internet connection."))
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Result.failure(Exception("Transcription failed: ${e.message}"))
        }
    }

    /**
     * Check if the API is configured and available
     */
    fun isConfigured(): Boolean {
        return preferencesManager.isGroqApiKeyConfigured()
    }

    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<String> {
        return listOf("en", "bn")
    }

    /**
     * Get MIME type for audio file
     */
    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "3gp" -> "audio/3gpp"
            "mp4", "m4a" -> "audio/mp4"
            "webm" -> "audio/webm"
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            else -> "audio/3gpp"
        }
    }

    /**
     * Parse error response
     */
    private fun parseError(code: Int, body: String?): String {
        if (body.isNullOrBlank()) return "API Error: $code"
        
        return try {
            val error = TranscriptionError.fromJson(body)
            error.message
        } catch (e: Exception) {
            "API Error: $code"
        }
    }

    companion object {
        @Volatile
        private var instance: GroqWhisperApi? = null

        fun getInstance(context: Context): GroqWhisperApi {
            return instance ?: synchronized(this) {
                instance ?: GroqWhisperApi(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
}
