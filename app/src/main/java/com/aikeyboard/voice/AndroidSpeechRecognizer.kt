package com.aikeyboard.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "AndroidSpeechRecognizer"

/**
 * Android's built-in SpeechRecognizer
 * - 100% FREE
 * - UNLIMITED requests
 * - Works OFFLINE on most devices
 * - No API key needed
 */
class AndroidSpeechRecognizer(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    sealed class RecognitionState {
        data class Ready(val message: String = "Listening...") : RecognitionState()
        data class PartialResult(val text: String) : RecognitionState()
        data class FinalResult(val text: String) : RecognitionState()
        data class Error(val message: String) : RecognitionState()
        data object Silent : RecognitionState()
    }

    /**
     * Start listening and emit results as a Flow
     */
    fun startListening(language: String): Flow<RecognitionState> = callbackFlow {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == "bn") "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                trySend(RecognitionState.Ready())
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - could be used for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected in time"
                    else -> "Unknown error: $error"
                }
                Log.e(TAG, "Recognition error: $errorMessage")
                trySend(RecognitionState.Error(errorMessage))
                close()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "Event: $eventType")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Log.d(TAG, "Partial result: $text")
                    trySend(RecognitionState.PartialResult(text))
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Log.d(TAG, "Final result: $text")
                    trySend(RecognitionState.FinalResult(text))
                } else {
                    trySend(RecognitionState.Error("No speech recognized"))
                }
                close()
            }
        }

        recognizer.setRecognitionListener(listener)
        recognizer.startListening(intent)

        Log.d(TAG, "Started listening for language: $language")

        awaitClose {
            Log.d(TAG, "Stopping speech recognizer")
            try {
                recognizer.stopListening()
                recognizer.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying recognizer", e)
            }
            speechRecognizer = null
        }
    }

    /**
     * Stop listening immediately
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d(TAG, "Speech recognizer stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer", e)
        }
    }

    /**
     * Check if SpeechRecognizer is available on this device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Destroy and cleanup
     */
    fun destroy() {
        stopListening()
    }

    companion object {
        /**
         * Check if speech recognition is available on device
         */
        fun isAvailable(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
    }
}
