package com.aikeyboard.voice

import android.util.Base64
import android.util.Log
import com.aikeyboard.AiKeyboardApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "GroqWhisperClient"

/**
 * Groq Whisper API Client - Fast, accurate speech-to-text
 * Free tier: ~10,000 requests/day
 * Get API key from: https://console.groq.com
 */
class GroqWhisperClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudio(audioFile: File, language: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate file
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file does not exist"))
            }
            if (audioFile.length() == 0L) {
                return@withContext Result.failure(Exception("Audio file is empty"))
            }

            val audioBytes = audioFile.readBytes()
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            // Determine mime type based on file extension
            val mimeType = when (audioFile.extension.lowercase()) {
                "3gp" -> "audio/3gpp"
                "mp4", "m4a" -> "audio/mp4"
                "webm" -> "audio/webm"
                "wav" -> "audio/wav"
                "mp3" -> "audio/mpeg"
                "aac" -> "audio/aac"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                else -> "audio/3gpp"
            }

            Log.d(TAG, "Transcribing with Groq Whisper: ${audioFile.name}, mimeType: $mimeType, size: ${audioFile.length()}")

            // Build multipart form request
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "whisper-large-v3")
                .addFormDataPart("language", if (language == "bn") "bn" else "en")
                .addFormDataPart("response_format", "json")
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioBytes.toRequestBody(mimeType.toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("${AiKeyboardApp.GROQ_ENDPOINT}/audio/transcriptions")
                .addHeader("Authorization", "Bearer ${AiKeyboardApp.GROQ_API_KEY}")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (bodyString.isNullOrBlank()) {
                        return@withContext Result.failure(Exception("Empty response body"))
                    }

                    try {
                        val jsonResponse = JSONObject(bodyString)
                        val text = jsonResponse.optString("text", "")

                        if (text.isBlank()) {
                            return@withContext Result.failure(Exception("Empty transcription"))
                        }

                        Log.d(TAG, "Groq transcription success: ${text.take(50)}...")
                        Result.success(text.trim())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        Result.failure(Exception("Failed to parse response: ${e.message}"))
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "API Error: ${response.code} - $errorBody")

                    // Parse error message
                    val errorMessage = try {
                        JSONObject(errorBody).optJSONObject("error")?.optString("message") ?: "API Error: ${response.code}"
                    } catch (e: Exception) {
                        "API Error: ${response.code}"
                    }

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

    companion object {
        val instance by lazy { GroqWhisperClient() }
    }
}
