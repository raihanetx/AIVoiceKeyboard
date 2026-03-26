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
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "AIVoiceKeyboard/3.7.0 (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})")
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Cache-Control", "no-cache")
                .build()
            chain.proceed(request)
        }
        .build()

    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(context) }

    /**
     * Get API key from preferences
     */
    private fun getApiKey(): String {
        val savedKey = preferencesManager.getGroqApiKey().trim()
        Log.d(TAG, "API key from preferences: length=${savedKey.length}, prefix=${if (savedKey.isNotEmpty()) savedKey.take(7) else "empty"}")
        return savedKey
    }

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
                Log.e(TAG, "Audio file does not exist: ${audioFile.absolutePath}")
                return@withContext Result.failure(Exception("Audio file does not exist"))
            }
            if (audioFile.length() == 0L) {
                Log.e(TAG, "Audio file is empty")
                return@withContext Result.failure(Exception("Audio file is empty"))
            }

            // Get API key from preferences
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                Log.e(TAG, "No API key configured!")
                return@withContext Result.failure(Exception("Please enter your Groq API key"))
            }
            
            Log.d(TAG, "========== TRANSCRIBE START ==========")
            Log.d(TAG, "Audio: ${audioFile.name}, size: ${audioFile.length()} bytes")
            Log.d(TAG, "Language: $language")
            Log.d(TAG, "API Key: length=${apiKey.length}, prefix=${apiKey.take(7)}...")

            val audioBytes = audioFile.readBytes()
            val mimeType = getMimeType(audioFile)

            Log.d(TAG, "MimeType: $mimeType, Bytes: ${audioBytes.size}")

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

            val url = "${ApiConstants.GROQ_ENDPOINT}/audio/transcriptions"
            Log.d(TAG, "Request URL: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            Log.d(TAG, "Sending request to Groq API...")
            
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response body: $bodyString")
                
                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val transcriptionResponse = TranscriptionResponse.fromJson(bodyString)
                    
                    if (transcriptionResponse.isValid) {
                        Log.d(TAG, "========== TRANSCRIBE SUCCESS ==========")
                        Log.d(TAG, "Result: ${transcriptionResponse.text}")
                        Result.success(transcriptionResponse.text.trim())
                    } else {
                        Log.e(TAG, "Empty transcription result")
                        Result.failure(Exception("Empty transcription"))
                    }
                } else {
                    val errorMessage = parseError(response.code, bodyString)
                    Log.e(TAG, "========== TRANSCRIBE FAILED ==========")
                    Log.e(TAG, "Error: $errorMessage")
                    Log.e(TAG, "Full response: $bodyString")
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
        return preferencesManager.hasGroqApiKey()
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
