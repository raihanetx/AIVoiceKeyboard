package com.aikeyboard.data.remote.api

import android.content.Context
import android.util.Log
import com.aikeyboard.core.constants.ApiConstants
import com.aikeyboard.data.local.PreferencesManager
import com.aikeyboard.presentation.keyboard.ErrorType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Base64
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiLiveApi"

/**
 * Gemini 2.5 Flash Live API client for speech-to-text transcription
 *
 * Uses Gemini's native audio input capability for real-time transcription.
 * Free tier: 15 RPM, 1 million tokens/day
 * Get API key from: https://aistudio.google.com/apikey
 */
class GeminiLiveApi(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConstants.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(context) }

    /**
     * Transcribe audio file to text using Gemini 2.5 Flash
     *
     * @param audioFile The audio file to transcribe
     * @param language The language code (e.g., "en", "bn")
     * @return TranscriptionResult with either success text or detailed error
     */
    suspend fun transcribe(
        audioFile: File,
        language: String
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        try {
            // Validate file
            if (!audioFile.exists()) {
                return@withContext TranscriptionResult.error(
                    ErrorType.NO_AUDIO,
                    "Audio file not found",
                    "Path: ${audioFile.absolutePath}"
                )
            }
            if (audioFile.length() == 0L) {
                return@withContext TranscriptionResult.error(
                    ErrorType.NO_AUDIO,
                    "Audio file is empty",
                    "No audio data was recorded. Please check microphone permissions."
                )
            }

            // Check API key from PreferencesManager
            val apiKey = preferencesManager.getGeminiApiKey().trim()
            if (apiKey.isEmpty()) {
                return@withContext TranscriptionResult.error(
                    ErrorType.AUTH,
                    "No API key configured",
                    "Please enter your Gemini API key in the voice settings.\n\nGet free key from: aistudio.google.com/apikey"
                )
            }

            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.getEncoder().encodeToString(audioBytes)
            val mimeType = getMimeType(audioFile)

            Log.d(TAG, "========== GEMINI TRANSCRIBE START ==========")
            Log.d(TAG, "File: ${audioFile.name}, mimeType: $mimeType, size: ${audioFile.length()}")
            Log.d(TAG, "API Key: length=${apiKey.length}, prefix=${apiKey.take(5)}...")
            Log.d(TAG, "Language: $language")

            val languageInstruction = if (language == "bn") {
                "Transcribe the following audio in Bengali language. Output only the transcribed text, nothing else."
            } else {
                "Transcribe the following audio in English language. Output only the transcribed text, nothing else."
            }

            // Build JSON request for Gemini
            val jsonBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", base64Audio)
                                })
                            })
                            put(JSONObject().apply {
                                put("text", languageInstruction)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 1024)
                })
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val url = "${ApiConstants.GEMINI_ENDPOINT}/models/${ApiConstants.GEMINI_MODEL}:generateContent?key=$apiKey"
            Log.d(TAG, "Request URL: ${url.take(80)}...")

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d(TAG, "Sending request to Gemini API...")

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response body: ${bodyString?.take(200)}")

                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val jsonResponse = JSONObject(bodyString)
                    val candidates = jsonResponse.optJSONArray("candidates")

                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0)
                            .optJSONObject("content")
                        val parts = content?.optJSONArray("parts")

                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text", "")
                            if (text.isNotBlank()) {
                                Log.d(TAG, "========== GEMINI SUCCESS ==========")
                                Log.d(TAG, "Result: ${text.take(100)}")
                                return@withContext TranscriptionResult.success(text.trim())
                            }
                        }
                    }

                    Log.e(TAG, "Empty transcription result")
                    TranscriptionResult.error(
                        ErrorType.EMPTY_RESULT,
                        "No speech detected",
                        "The audio was processed but no speech was detected. Please speak clearly and try again."
                    )
                } else {
                    val (errorType, errorMessage, errorDetails) = parseDetailedError(response.code, bodyString)
                    Log.e(TAG, "========== GEMINI FAILED ==========")
                    Log.e(TAG, "Error Type: $errorType")
                    Log.e(TAG, "Error Message: $errorMessage")
                    TranscriptionResult.error(errorType, errorMessage, errorDetails)
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timed out", e)
            TranscriptionResult.error(
                ErrorType.TIMEOUT,
                "Request timed out",
                "The API request took too long. Please check your internet connection and try again.\n\nTechnical: ${e.message}"
            )
        } catch (e: UnknownHostException) {
            Log.e(TAG, "No internet connection", e)
            TranscriptionResult.error(
                ErrorType.NETWORK,
                "No internet connection",
                "Unable to connect to Gemini API. Please check your internet connection.\n\nTechnical: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            TranscriptionResult.error(
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
        return preferencesManager.hasGeminiApiKey()
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
            "ogg" -> "audio/ogg"
            else -> "audio/3gpp"
        }
    }

    /**
     * Parse detailed error from API response
     * Returns: Triple(errorType, userMessage, technicalDetails)
     */
    private fun parseDetailedError(code: Int, body: String?): Triple<String, String, String> {
        val technicalDetails = "HTTP $code\nResponse: ${body?.take(500) ?: "No response body"}"

        // Parse Gemini error format
        val parsedError = try {
            if (!body.isNullOrBlank()) {
                val json = JSONObject(body)
                val error = json.optJSONObject("error")
                error?.optString("message", null)
            } else null
        } catch (e: Exception) { null }

        when (code) {
            400 -> {
                return Triple(
                    ErrorType.INVALID_KEY,
                    "Invalid request",
                    "${parsedError ?: "Bad request"}\n\nThe audio format may not be supported.\n\n$technicalDetails"
                )
            }
            401, 403 -> {
                return Triple(
                    ErrorType.INVALID_KEY,
                    "Invalid API key",
                    "${parsedError ?: "Authentication failed"}\n\nYour API key was rejected. Please verify:\n1. The key is correct\n2. The key is active\n3. Get your key from: aistudio.google.com/apikey\n\n$technicalDetails"
                )
            }
            429 -> {
                return Triple(
                    ErrorType.RATE_LIMIT,
                    "Rate limit exceeded",
                    "${parsedError ?: "Too many requests"}\n\nYou've reached the API rate limit. Please wait a moment and try again.\n\n$technicalDetails"
                )
            }
            500, 502, 503, 504 -> {
                return Triple(
                    ErrorType.SERVER,
                    "Server error",
                    "Gemini API is experiencing issues (HTTP $code). Please try again in a few moments.\n\n$technicalDetails"
                )
            }
            else -> {
                return Triple(
                    ErrorType.UNKNOWN,
                    "API Error ($code)",
                    "${parsedError ?: "Unknown error"}\n\n$technicalDetails"
                )
            }
        }
    }

    companion object {
        @Volatile
        private var instance: GeminiLiveApi? = null

        fun getInstance(context: Context): GeminiLiveApi {
            return instance ?: synchronized(this) {
                instance ?: GeminiLiveApi(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
