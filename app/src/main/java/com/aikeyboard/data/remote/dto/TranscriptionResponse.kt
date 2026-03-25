package com.aikeyboard.data.remote.dto

import org.json.JSONObject

/**
 * Data Transfer Object for transcription API response
 * 
 * Matches the response format from Groq Whisper API
 */
data class TranscriptionResponse(
    val text: String,
    val language: String? = null,
    val duration: Float? = null,
    val words: List<WordTiming>? = null
) {
    /**
     * Check if response is valid (has non-empty text)
     */
    val isValid: Boolean
        get() = text.isNotBlank()

    /**
     * Word timing information for detailed transcription
     */
    data class WordTiming(
        val word: String,
        val start: Float,
        val end: Float
    )

    companion object {
        /**
         * Parse TranscriptionResponse from JSON string
         */
        fun fromJson(jsonString: String): TranscriptionResponse {
            return try {
                val json = JSONObject(jsonString)
                TranscriptionResponse(
                    text = json.optString("text", ""),
                    language = json.optString("language", null),
                    duration = json.optDouble("duration", 0.0).takeIf { it > 0 }?.toFloat(),
                    words = parseWords(json.optJSONArray("words"))
                )
            } catch (e: Exception) {
                TranscriptionResponse(text = "")
            }
        }

        private fun parseWords(wordsArray: org.json.JSONArray?): List<WordTiming>? {
            if (wordsArray == null || wordsArray.length() == 0) return null
            
            return (0 until wordsArray.length()).mapNotNull { i ->
                try {
                    val wordObj = wordsArray.getJSONObject(i)
                    WordTiming(
                        word = wordObj.optString("word", ""),
                        start = wordObj.optDouble("start", 0.0).toFloat(),
                        end = wordObj.optDouble("end", 0.0).toFloat()
                    )
                } catch (e: Exception) {
                    null
                }
            }.takeIf { it.isNotEmpty() }
        }
    }

    /**
     * Convert to JSON string
     */
    fun toJson(): String {
        return JSONObject().apply {
            put("text", text)
            language?.let { put("language", it) }
            duration?.let { put("duration", it) }
            words?.let { wordList ->
                put("words", org.json.JSONArray().apply {
                    wordList.forEach { word ->
                        put(JSONObject().apply {
                            put("word", word.word)
                            put("start", word.start)
                            put("end", word.end)
                        })
                    }
                })
            }
        }.toString()
    }
}

/**
 * DTO for transcription error response
 */
data class TranscriptionError(
    val message: String,
    val type: String? = null,
    val code: String? = null
) {
    companion object {
        fun fromJson(jsonString: String): TranscriptionError {
            return try {
                val json = JSONObject(jsonString)
                val errorObj = json.optJSONObject("error") ?: json
                TranscriptionError(
                    message = errorObj.optString("message", "Unknown error"),
                    type = errorObj.optString("type", null),
                    code = errorObj.optString("code", null)
                )
            } catch (e: Exception) {
                TranscriptionError(message = "Failed to parse error response")
            }
        }
    }
}
