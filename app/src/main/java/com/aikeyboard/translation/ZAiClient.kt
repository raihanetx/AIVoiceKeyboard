package com.aikeyboard.translation

import com.aikeyboard.AiKeyboardApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ZAiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val srcName = if (sourceLang == "en") "English" else "Bengali"
            val tgtName = if (targetLang == "en") "English" else "Bengali"
            
            val jsonBody = JSONObject().apply {
                put("model", "glm-4.7-flash")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are a translator. Only output the translation, nothing else.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Translate from $srcName to $tgtName: $text")
                    })
                })
                put("max_tokens", 2048)
                put("temperature", 0.3)
            }

            val request = Request.Builder()
                .url("${AiKeyboardApp.ZAI_ENDPOINT}/chat/completions")
                .addHeader("Authorization", "Bearer ${AiKeyboardApp.ZAI_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val content = jsonResponse
                    .optJSONArray("choices")
                    ?.getJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content", "") ?: ""
                Result.success(content.trim())
            } else {
                Result.failure(Exception("API Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Helper class for use in Compose
class ZAiClientHelper {
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        return ZAiClient.translate(text, sourceLang, targetLang)
    }
}
