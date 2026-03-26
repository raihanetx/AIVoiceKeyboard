package com.aikeyboard.data.remote.api

import android.content.Context
import android.util.Log
import com.aikeyboard.core.constants.ApiConstants
import com.aikeyboard.data.local.PreferencesManager
import com.aikeyboard.data.remote.dto.TranscriptionError
import com.aikeyboard.data.remote.dto.TranscriptionResponse
import com.aikeyboard.presentation.keyboard.ErrorType
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
                .addHeader("User-Agent", "AIVoiceKeyboard/3.8.0 (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})")
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
     * @return ApiTranscriptionResult with either success text or detailed error
     */
    suspend fun transcribe(
        audioFile: File,
        language: String
    ): ApiTranscriptionResult = withContext(Dispatchers.IO) {
        try {
            // Validate file
            if (!audioFile.exists()) {
                return@withContext ApiTranscriptionResult.error(
                    ErrorType.NO_AUDIO,
                    "Audio file not found",
                    "Path: ${audioFile.absolutePath}"
                )
            }
            if (audioFile.length() == 0L) {
                return@withContext ApiTranscriptionResult.error(
                    ErrorType.NO_AUDIO,
                    "Audio file is empty",
                    "No audio data was recorded. Please check microphone permissions."
                )
            }

            // Get API key from preferences
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                return@withContext ApiTranscriptionResult.error(
                    ErrorType.AUTH,
                    "No API key configured",
                    "Please enter your Groq API key in the voice settings."
                )
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
                        ApiTranscriptionResult.success(transcriptionResponse.text.trim())
                    } else {
                        Log.e(TAG, "Empty transcription result")
                        ApiTranscriptionResult.error(
                            ErrorType.EMPTY_RESULT,
                            "No speech detected",
                            "The audio was processed but no speech was detected. Please speak clearly and try again."
                        )
                    }
                } else {
                    val (errorType, errorMessage, errorDetails) = parseDetailedError(response.code, bodyString)
                    Log.e(TAG, "========== TRANSCRIBE FAILED ==========")
                    Log.e(TAG, "Error Type: $errorType")
                    Log.e(TAG, "Error Message: $errorMessage")
                    Log.e(TAG, "Full response: $bodyString")
                    ApiTranscriptionResult.error(errorType, errorMessage, errorDetails)
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timed out", e)
            ApiTranscriptionResult.error(
                ErrorType.TIMEOUT,
                "Request timed out",
                "The API request took too long. Please check your internet connection and try again.\n\nTechnical: ${e.message}"
            )
        } catch (e: UnknownHostException) {
            Log.e(TAG, "No internet connection", e)
            ApiTranscriptionResult.error(
                ErrorType.NETWORK,
                "No internet connection",
                "Unable to connect to Groq API. Please check your internet connection.\n\nTechnical: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            ApiTranscriptionResult.error(
                ErrorType.UNKNOWN,
                "Transcription failed",
                "An unexpected error occurred: ${e.message}\n\nException: ${e.javaClass.simpleName}"
            )
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
     * Parse detailed error from API response
     * Returns: Triple(errorType, userMessage, technicalDetails)
     */
    private fun parseDetailedError(code: Int, body: String?): Triple<String, String, String> {
        val technicalDetails = "HTTP $code\nResponse: ${body?.take(500) ?: "No response body"}"

        when (code) {
            401 -> {
                // Parse error details if available
                val parsedError = body?.let { parseErrorMessage(it) } ?: "Invalid API key"
                return Triple(
                    ErrorType.INVALID_KEY,
                    "Invalid API key",
                    "$parsedError\n\nYour API key was rejected. Please verify:\n1. The key is correct\n2. The key is active\n3. Get your key from: console.groq.com/keys\n\n$technicalDetails"
                )
            }
            403 -> {
                val parsedError = body?.let { parseErrorMessage(it) } ?: "Access forbidden"
                return Triple(
                    ErrorType.AUTH,
                    "Access denied",
                    "$parsedError\n\nYour API key may be:\n1. Expired\n2. Restricted\n3. Not authorized for this API\n\n$technicalDetails"
                )
            }
            429 -> {
                val parsedError = body?.let { parseErrorMessage(it) } ?: "Rate limit exceeded"
                return Triple(
                    ErrorType.RATE_LIMIT,
                    "Rate limit exceeded",
                    "$parsedError\n\nYou've reached the API rate limit. Please wait a moment and try again.\n\n$technicalDetails"
                )
            }
            500, 502, 503, 504 -> {
                return Triple(
                    ErrorType.SERVER,
                    "Server error",
                    "Groq API is experiencing issues (HTTP $code). Please try again in a few moments.\n\n$technicalDetails"
                )
            }
            400 -> {
                val parsedError = body?.let { parseErrorMessage(it) } ?: "Bad request"
                return Triple(
                    ErrorType.UNKNOWN,
                    "Invalid request",
                    "$parsedError\n\nThe audio format may not be supported.\n\n$technicalDetails"
                )
            }
            else -> {
                val parsedError = body?.let { parseErrorMessage(it) } ?: "Unknown error"
                return Triple(
                    ErrorType.UNKNOWN,
                    "API Error ($code)",
                    "$parsedError\n\n$technicalDetails"
                )
            }
        }
    }

    /**
     * Parse error message from JSON response
     */
    private fun parseErrorMessage(body: String): String? {
        return try {
            val error = TranscriptionError.fromJson(body)
            error.message
        } catch (e: Exception) {
            null
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
