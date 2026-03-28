package com.aikeyboard.data.remote.api

import android.content.Context
import android.util.Log
import com.aikeyboard.data.local.PreferencesManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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

class GroqWhisperApi(private val context: Context) {

    companion object {
        private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1"
        private const val GROQ_WHISPER_MODEL = "whisper-large-v3"
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 60L

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

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(context) }

    suspend fun transcribe(
        audioFile: File,
        language: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file does not exist"))
            }
            if (audioFile.length() == 0L) {
                return@withContext Result.failure(Exception("Audio file is empty"))
            }

            val apiKey = preferencesManager.getGroqApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Groq API key not configured. Get free key from https://console.groq.com"))
            }

            val audioBytes = audioFile.readBytes()
            val mimeType = getMimeType(audioFile)

            Log.d(TAG, "Transcribing: ${audioFile.name}, mimeType: $mimeType, size: ${audioFile.length()}")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", GROQ_WHISPER_MODEL)
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
                .url("$GROQ_ENDPOINT/audio/transcriptions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                
                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val json = JsonParser.parseString(bodyString).asJsonObject
                    val text = json.get("text")?.asString ?: ""
                    
                    if (text.isNotBlank()) {
                        Log.d(TAG, "Transcription success: ${text.take(50)}...")
                        Result.success(text.trim())
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

    fun isConfigured(): Boolean {
        return preferencesManager.isGroqApiKeyConfigured()
    }

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

    private fun parseError(code: Int, body: String?): String {
        if (body.isNullOrBlank()) return "API Error: $code"
        
        return try {
            val json = JsonParser.parseString(body).asJsonObject
            val errorObj = json.getAsJsonObject("error")
            errorObj?.get("message")?.asString ?: "API Error: $code"
        } catch (e: Exception) {
            "API Error: $code"
        }
    }
}
