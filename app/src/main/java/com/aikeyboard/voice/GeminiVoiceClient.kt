package com.aikeyboard.voice

import android.util.Base64
import com.aikeyboard.AiKeyboardApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiVoiceClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudio(audioFile: File, language: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val audioBase64 = Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Transcribe this audio to ${if(language=="bn") "Bengali" else "English"}. Only output text."))
                            put(JSONObject().put("inline_data", JSONObject().put("mime_type", "audio/wav").put("data", audioBase64)))
                        })
                    })
                })
            }
            val url = "${AiKeyboardApp.GEMINI_ENDPOINT}/${AiKeyboardApp.GEMINI_AUDIO_MODEL}:generateContent?key=${AiKeyboardApp.GEMINI_API_KEY}"
            val request = Request.Builder().url(url).post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val text = JSONObject(response.body?.string()).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                Result.success(text.trim())
            } else Result.failure(Exception("API Error: ${response.code}"))
        } catch (e: Exception) { Result.failure(e) }
    }
    companion object { val instance by lazy { GeminiVoiceClient() } }
}
