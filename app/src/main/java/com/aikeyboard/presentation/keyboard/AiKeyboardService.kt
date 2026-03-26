package com.aikeyboard.presentation.keyboard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import com.aikeyboard.R
import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.core.extension.dpToPx
import com.aikeyboard.core.util.AudioRecorder
import com.aikeyboard.data.local.PreferencesManager
import com.aikeyboard.data.remote.api.ApiTranscriptionResult
import com.aikeyboard.data.remote.api.GeminiLiveApi
import com.aikeyboard.data.remote.api.GroqWhisperApi
import com.aikeyboard.data.remote.api.ZAiApi
import com.aikeyboard.domain.model.Language
import com.aikeyboard.presentation.keyboard.view.EmojiView
import com.aikeyboard.presentation.keyboard.view.KeyboardView
import com.aikeyboard.presentation.keyboard.view.TranslateView
import com.aikeyboard.presentation.keyboard.view.VoiceInputView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI Voice Keyboard Input Method Service
 */
class AiKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "AiKeyboard"
    }

    // Preferences
    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(this) }

    // API clients
    private val groqWhisperApi: GroqWhisperApi by lazy { GroqWhisperApi.getInstance(this) }
    private val geminiLiveApi: GeminiLiveApi by lazy { GeminiLiveApi.getInstance(this) }
    private val zAiApi: ZAiApi by lazy { ZAiApi.instance }

    // Voice recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecorder: AudioRecorder? = null
    private var isRecording = false

    // Views
    private var mainLayout: LinearLayout? = null
    private var voiceInputView: VoiceInputView? = null

    // Current voice state
    private var voiceState = VoiceUiState()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== KEYBOARD SERVICE CREATED ===")
        audioRecorder = AudioRecorder(this)
    }

    override fun onCreateInputView(): View {
        Log.d(TAG, "=== Creating keyboard view ===")
        return try {
            loadPreferences()
            mainLayout = createMainLayout()
            mainLayout!!
        } catch (e: Exception) {
            Log.e(TAG, "Error creating keyboard view", e)
            createErrorView("Error: ${e.message}")
        }
    }

    /**
     * Load saved preferences into state
     */
    private fun loadPreferences() {
        val savedEngine = preferencesManager.getSttEngine()
        val savedLanguage = preferencesManager.getLanguage()

        voiceState = voiceState.copy(
            selectedEngine = savedEngine,
            currentLanguage = if (savedLanguage == "bn") Language.BENGALI else Language.ENGLISH,
            groqApiKeyStatus = if (preferencesManager.hasGroqApiKey()) ApiKeyStatus.SAVED else ApiKeyStatus.NONE,
            geminiApiKeyStatus = if (preferencesManager.hasGeminiApiKey()) ApiKeyStatus.SAVED else ApiKeyStatus.NONE,
            statusMessage = getStatusMessage(savedEngine)
        )
        Log.d(TAG, "Loaded preferences - Engine: $savedEngine, GroqKey: ${voiceState.groqApiKeyStatus}, GeminiKey: ${voiceState.geminiApiKeyStatus}")
    }

    /**
     * Get status message based on engine and API key status
     */
    private fun getStatusMessage(engine: String): String {
        return when (engine) {
            "android" -> "🟢 Android ready - Tap mic to speak"
            "groq" -> if (preferencesManager.hasGroqApiKey()) "🔵 Groq ready - Tap mic to speak" else "⚠️ Enter Groq API Key first"
            "gemini" -> if (preferencesManager.hasGeminiApiKey()) "🟣 Gemini ready - Tap mic to speak" else "⚠️ Enter Gemini API Key first"
            else -> "Select an engine"
        }
    }

    private fun createMainLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )

            addView(createHeader())
            addView(createPanelContent(AppConstants.PANEL_KEYBOARD))
            addView(createLanguageSwitch())
        }
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER
            setPadding(8, 10, 8, 10)

            listOf(
                "ABC" to AppConstants.PANEL_KEYBOARD,
                "🎤" to AppConstants.PANEL_VOICE,
                "🌐" to AppConstants.PANEL_TRANSLATE,
                "😀" to AppConstants.PANEL_EMOJI
            ).forEach { (label, panel) ->
                addView(Button(this@AiKeyboardService).apply {
                    text = label
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.TRANSPARENT)
                    textSize = 14f
                    setAllCaps(false)
                    setPadding(16, 8, 16, 8)
                    setOnClickListener { switchPanel(panel) }
                })
            }
        }
    }

    private fun switchPanel(panel: String) {
        mainLayout?.removeViewAt(1)
        mainLayout?.addView(createPanelContent(panel), 1)
        Log.d(TAG, "Switched to panel: $panel")
    }

    private fun createPanelContent(panel: String): View {
        return when (panel) {
            AppConstants.PANEL_VOICE -> createVoicePanel()
            AppConstants.PANEL_TRANSLATE -> createTranslatePanel()
            AppConstants.PANEL_EMOJI -> createEmojiPanel()
            else -> createKeyboardPanel()
        }
    }

    // ==================== KEYBOARD PANEL ====================

    private fun createKeyboardPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(4), 0, dpToPx(4))

            val keyboardView = KeyboardView.create(
                context = this@AiKeyboardService,
                onKeyPress = { key -> handleKeyPress(key) },
                onLongKeyPress = { key -> handleLongKeyPress(key) }
            )
            addView(keyboardView)
        }
    }

    private fun handleKeyPress(key: String) {
        when {
            key == "⌫" -> currentInputConnection?.deleteSurroundingText(1, 0)
            key == "Space" -> currentInputConnection?.commitText(" ", 1)
            key == "↵" -> currentInputConnection?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            key == "😊" -> switchPanel(AppConstants.PANEL_EMOJI)
            else -> currentInputConnection?.commitText(key, 1)
        }
    }

    private fun handleLongKeyPress(key: String) {
        if (key == "⌫") {
            currentInputConnection?.deleteSurroundingText(10000, 0)
        }
    }

    // ==================== VOICE PANEL ====================

    private fun createVoicePanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER_HORIZONTAL

            voiceInputView = VoiceInputView.create(this@AiKeyboardService).apply {
                onEngineSelected = { engine -> handleEngineSelection(engine) }
                onApiKeySaved = { engine, key -> handleApiKeyEntered(engine, key) }
                onMicClicked = { handleMicClick() }
                onLanguageChanged = { language -> handleLanguageChange(language) }
                onInsertText = { text -> insertText(text) }
            }
            voiceInputView?.updateState(voiceState)
            addView(voiceInputView)
        }
    }

    /**
     * Handle engine selection
     */
    private fun handleEngineSelection(engine: String) {
        Log.d(TAG, "=== Engine selected: $engine ===")

        when (engine) {
            "android" -> {
                // Android doesn't need API key
                selectEngine(engine)
            }
            "groq" -> {
                if (preferencesManager.hasGroqApiKey()) {
                    Log.d(TAG, "Groq has API key, selecting")
                    voiceState = voiceState.copy(groqApiKeyStatus = ApiKeyStatus.SAVED)
                    selectEngine(engine)
                } else {
                    // Show API key input
                    Log.d(TAG, "Groq needs API key, showing input")
                    voiceState = voiceState.copy(
                        showApiKeyInputFor = "groq"
                    ).clearError()
                    voiceInputView?.updateState(voiceState)
                }
            }
            "gemini" -> {
                if (preferencesManager.hasGeminiApiKey()) {
                    Log.d(TAG, "Gemini has API key, selecting")
                    voiceState = voiceState.copy(geminiApiKeyStatus = ApiKeyStatus.SAVED)
                    selectEngine(engine)
                } else {
                    // Show API key input
                    Log.d(TAG, "Gemini needs API key, showing input")
                    voiceState = voiceState.copy(
                        showApiKeyInputFor = "gemini"
                    ).clearError()
                    voiceInputView?.updateState(voiceState)
                }
            }
        }
    }

    /**
     * Handle API key entered
     */
    private fun handleApiKeyEntered(engine: String, apiKey: String) {
        Log.d(TAG, "=== API key entered for: $engine ===")

        if (apiKey.isBlank()) {
            Log.e(TAG, "API key is blank!")
            voiceState = voiceState.withError(
                title = "Invalid Input",
                message = "API Key cannot be empty",
                type = ErrorType.INVALID_KEY
            )
            voiceInputView?.updateState(voiceState)
            return
        }

        // Save the key synchronously
        val saved = when (engine) {
            "groq" -> {
                val success = preferencesManager.setGroqApiKey(apiKey)
                Log.d(TAG, "Groq API key saved: $success, hasKey=${preferencesManager.hasGroqApiKey()}")
                success
            }
            "gemini" -> {
                val success = preferencesManager.setGeminiApiKey(apiKey)
                Log.d(TAG, "Gemini API key saved: $success, hasKey=${preferencesManager.hasGeminiApiKey()}")
                success
            }
            else -> false
        }

        if (!saved) {
            Log.e(TAG, "Failed to save API key!")
            voiceState = voiceState.withError(
                title = "Save Failed",
                message = "Failed to save API key",
                type = ErrorType.UNKNOWN
            )
            voiceInputView?.updateState(voiceState)
            return
        }

        // Select the engine with proper status
        selectEngineWithStatus(engine)

        Toast.makeText(this, "$engine API Key saved!", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "=== API key flow complete for: $engine ===")
    }

    /**
     * Select an engine (simple version - just saves and updates state)
     */
    private fun selectEngine(engine: String) {
        preferencesManager.setSttEngine(engine)
        voiceState = voiceState.copy(
            selectedEngine = engine,
            showApiKeyInputFor = null,
            statusMessage = getReadyMessage(engine)
        ).clearError()
        voiceInputView?.updateState(voiceState)
        Log.d(TAG, "Engine selected and saved: $engine")
    }

    /**
     * Select engine after API key is saved (updates API key status too)
     */
    private fun selectEngineWithStatus(engine: String) {
        preferencesManager.setSttEngine(engine)

        voiceState = voiceState.copy(
            selectedEngine = engine,
            showApiKeyInputFor = null,
            groqApiKeyStatus = if (engine == "groq" || preferencesManager.hasGroqApiKey()) ApiKeyStatus.SAVED else voiceState.groqApiKeyStatus,
            geminiApiKeyStatus = if (engine == "gemini" || preferencesManager.hasGeminiApiKey()) ApiKeyStatus.SAVED else voiceState.geminiApiKeyStatus,
            statusMessage = getReadyMessage(engine)
        ).clearError()
        voiceInputView?.updateState(voiceState)
        Log.d(TAG, "Engine selected with status: $engine, groqStatus=${voiceState.groqApiKeyStatus}, geminiStatus=${voiceState.geminiApiKeyStatus}")
    }

    /**
     * Get ready message for an engine (engine is ready to use)
     */
    private fun getReadyMessage(engine: String): String {
        return when (engine) {
            "android" -> "🟢 Android ready - Tap mic to speak"
            "groq" -> "🔵 Groq ready - Tap mic to speak"
            "gemini" -> "🟣 Gemini ready - Tap mic to speak"
            else -> "Select an engine"
        }
    }

    /**
     * Handle mic button click
     */
    private fun handleMicClick() {
        Log.d(TAG, "=== handleMicClick called, isRecording=$isRecording ===")
        if (isRecording) {
            Log.d(TAG, "Stopping recording...")
            stopRecording()
        } else {
            Log.d(TAG, "Starting recording...")
            startRecording()
        }
    }

    /**
     * Handle language change
     */
    private fun handleLanguageChange(language: Language) {
        preferencesManager.setLanguage(language.code)
        voiceState = voiceState.copy(currentLanguage = language)
        voiceInputView?.updateState(voiceState)
    }

    /**
     * Start recording
     */
    private fun startRecording() {
        val engine = voiceState.selectedEngine
        Log.d(TAG, "=== startRecording called, engine=$engine ===")
        Log.d(TAG, "groqApiKeyStatus=${voiceState.groqApiKeyStatus}, geminiApiKeyStatus=${voiceState.geminiApiKeyStatus}")
        Log.d(TAG, "hasGroqApiKey=${preferencesManager.hasGroqApiKey()}, hasGeminiApiKey=${preferencesManager.hasGeminiApiKey()}")

        // Validate API key for Groq/Gemini
        when (engine) {
            "groq" -> {
                if (!preferencesManager.hasGroqApiKey()) {
                    Log.e(TAG, "Groq API key NOT found!")
                    voiceState = voiceState.copy(
                        showApiKeyInputFor = "groq"
                    ).withError(
                        title = "API Key Required",
                        message = "Please enter your Groq API key first",
                        type = ErrorType.AUTH
                    )
                    voiceInputView?.updateState(voiceState)
                    return
                }
                Log.d(TAG, "Groq API key found, proceeding")
            }
            "gemini" -> {
                if (!preferencesManager.hasGeminiApiKey()) {
                    Log.e(TAG, "Gemini API key NOT found!")
                    voiceState = voiceState.copy(
                        showApiKeyInputFor = "gemini"
                    ).withError(
                        title = "API Key Required",
                        message = "Please enter your Gemini API key first",
                        type = ErrorType.AUTH
                    )
                    voiceInputView?.updateState(voiceState)
                    return
                }
                Log.d(TAG, "Gemini API key found, proceeding")
            }
        }

        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission NOT granted!")
            voiceState = voiceState.withError(
                title = "Permission Required",
                message = "Microphone permission is required for voice typing",
                type = ErrorType.PERMISSION,
                details = "Please grant microphone permission in Settings > Apps > AI Voice Keyboard > Permissions"
            )
            voiceInputView?.updateState(voiceState)
            return
        }

        Log.d(TAG, "All checks passed, starting recording for $engine")
        isRecording = true
        voiceState = voiceState.copy(
            isRecording = true,
            statusMessage = "🔴 Recording with ${voiceState.getEngineDisplayName()}..."
        ).clearError()
        voiceInputView?.updateState(voiceState)

        when (engine) {
            "android" -> startAndroidSpeechRecognizer()
            "groq" -> startGroqRecording()
            "gemini" -> startGeminiRecording()
        }
    }

    /**
     * Start Android Speech Recognizer
     */
    private fun startAndroidSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopRecordingWithError(
                title = "Not Available",
                message = "Speech recognition is not available on this device",
                type = ErrorType.UNKNOWN
            )
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    voiceState = voiceState.copy(statusMessage = "Listening... Speak now")
                    voiceInputView?.updateState(voiceState)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    voiceState = voiceState.copy(statusMessage = "Processing...")
                    voiceInputView?.updateState(voiceState)
                }

                override fun onError(error: Int) {
                    val (errorType, errorMessage, errorDetails) = getSpeechErrorInfo(error)
                    stopRecordingWithError(
                        title = "Speech Error",
                        message = errorMessage,
                        type = errorType,
                        details = errorDetails
                    )
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        showResult(matches[0])
                    } else {
                        stopRecordingWithError(
                            title = "No Speech",
                            message = "No speech was detected",
                            type = ErrorType.EMPTY_RESULT
                        )
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        voiceState = voiceState.copy(statusMessage = matches[0])
                        voiceInputView?.updateState(voiceState)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceState.currentLanguage.localeCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun startGroqRecording() {
        val audioFile = audioRecorder?.startRecording()
        if (audioFile == null) {
            stopRecordingWithError(
                title = "Recording Failed",
                message = "Could not start audio recording",
                type = ErrorType.NO_AUDIO,
                details = "The audio recorder failed to initialize. Please check microphone permissions."
            )
            return
        }
        voiceState = voiceState.copy(statusMessage = "🔵 Recording... Tap mic to stop")
        voiceInputView?.updateState(voiceState)
    }

    private fun startGeminiRecording() {
        val audioFile = audioRecorder?.startRecording()
        if (audioFile == null) {
            stopRecordingWithError(
                title = "Recording Failed",
                message = "Could not start audio recording",
                type = ErrorType.NO_AUDIO,
                details = "The audio recorder failed to initialize. Please check microphone permissions."
            )
            return
        }
        voiceState = voiceState.copy(statusMessage = "🟣 Recording... Tap mic to stop")
        voiceInputView?.updateState(voiceState)
    }

    /**
     * Stop recording
     */
    private fun stopRecording() {
        isRecording = false

        if (voiceState.selectedEngine == "android") {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            voiceState = voiceState.copy(
                isRecording = false,
                statusMessage = getReadyMessage(voiceState.selectedEngine)
            )
            voiceInputView?.updateState(voiceState)
        } else {
            val audioFile = audioRecorder?.stopRecording()

            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                voiceState = voiceState.copy(
                    statusMessage = "Transcribing...",
                    isRecording = false
                )
                voiceInputView?.updateState(voiceState)

                CoroutineScope(Dispatchers.IO).launch {
                    val result = if (voiceState.selectedEngine == "gemini") {
                        geminiLiveApi.transcribe(audioFile, voiceState.currentLanguage.code)
                    } else {
                        groqWhisperApi.transcribe(audioFile, voiceState.currentLanguage.code)
                    }

                    withContext(Dispatchers.Main) {
                        handleTranscriptionResult(result)
                    }
                    audioFile.delete()
                }
            } else {
                stopRecordingWithError(
                    title = "No Audio",
                    message = "No audio was recorded",
                    type = ErrorType.NO_AUDIO,
                    details = "The recording file is empty or missing. Please try again."
                )
            }
        }
    }

    /**
     * Handle transcription result with detailed error handling
     */
    private fun handleTranscriptionResult(result: ApiTranscriptionResult) {
        if (result.isSuccess && result.text != null) {
            showResult(result.text)
        } else {
            // Show detailed error
            val engineName = voiceState.getEngineDisplayName()
            stopRecordingWithError(
                title = "$engineName Error",
                message = result.errorMessage ?: "Transcription failed",
                type = result.errorType ?: ErrorType.UNKNOWN,
                details = result.errorDetails
            )
        }
    }

    private fun stopRecordingWithError(
        title: String,
        message: String,
        type: String = ErrorType.UNKNOWN,
        details: String? = null
    ) {
        isRecording = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        voiceState = voiceState.withError(
            title = title,
            message = message,
            type = type,
            details = details
        ).copy(
            statusMessage = getStatusMessage(voiceState.selectedEngine)
        )
        voiceInputView?.updateState(voiceState)
    }

    private fun showResult(text: String) {
        isRecording = false
        voiceState = voiceState.copy(
            isRecording = false,
            showResult = true,
            resultText = text,
            statusMessage = "✓ Transcription complete"
        ).clearError()
        voiceInputView?.updateState(voiceState)
    }

    /**
     * Get speech error info: Triple(errorType, message, details)
     */
    private fun getSpeechErrorInfo(error: Int): Triple<String, String, String?> {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> Triple(
                ErrorType.NO_AUDIO,
                "Audio recording error",
                "Error code: ERROR_AUDIO ($error)\nThe microphone may be in use by another app."
            )
            SpeechRecognizer.ERROR_CLIENT -> Triple(
                ErrorType.UNKNOWN,
                "Client error",
                "Error code: ERROR_CLIENT ($error)\nA client-side error occurred."
            )
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Triple(
                ErrorType.PERMISSION,
                "Permission denied",
                "Error code: ERROR_INSUFFICIENT_PERMISSIONS ($error)\nPlease grant microphone permission."
            )
            SpeechRecognizer.ERROR_NETWORK -> Triple(
                ErrorType.NETWORK,
                "Network error",
                "Error code: ERROR_NETWORK ($error)\nPlease check your internet connection."
            )
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Triple(
                ErrorType.TIMEOUT,
                "Network timeout",
                "Error code: ERROR_NETWORK_TIMEOUT ($error)\nThe network request timed out."
            )
            SpeechRecognizer.ERROR_NO_MATCH -> Triple(
                ErrorType.EMPTY_RESULT,
                "No speech detected",
                "Error code: ERROR_NO_MATCH ($error)\nPlease speak clearly and try again."
            )
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Triple(
                ErrorType.EMPTY_RESULT,
                "No speech detected",
                "Error code: ERROR_SPEECH_TIMEOUT ($error)\nNo speech was detected within the timeout period."
            )
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Triple(
                ErrorType.UNKNOWN,
                "Recognizer busy",
                "Error code: ERROR_RECOGNIZER_BUSY ($error)\nThe speech recognizer is busy. Please try again."
            )
            SpeechRecognizer.ERROR_SERVER -> Triple(
                ErrorType.SERVER,
                "Server error",
                "Error code: ERROR_SERVER ($error)\nThe speech recognition server encountered an error."
            )
            else -> Triple(
                ErrorType.UNKNOWN,
                "Unknown error",
                "Error code: $error"
            )
        }
    }

    // ==================== TRANSLATE PANEL ====================

    private fun createTranslatePanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)

            val translateView = TranslateView.create(
                context = this@AiKeyboardService,
                onTranslate = { text, source, target -> performTranslation(text, source, target) },
                onLanguageChange = { },
                onInsertText = { text -> insertText(text) }
            )
            addView(translateView)
        }
    }

    private fun performTranslation(text: String, source: Language, target: Language) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = zAiApi.translate(text, source.code, target.code)

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { translatedText ->
                        Toast.makeText(this@AiKeyboardService, translatedText, Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AiKeyboardService, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // ==================== EMOJI PANEL ====================

    private fun createEmojiPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            gravity = Gravity.CENTER

            val emojiView = EmojiView.create(
                context = this@AiKeyboardService,
                onEmojiClick = { emoji -> insertText(emoji) }
            )
            addView(emojiView)
        }
    }

    // ==================== LANGUAGE SWITCH ====================

    private fun createLanguageSwitch(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER
            setPadding(8, 6, 8, 6)

            addView(Button(this@AiKeyboardService).apply {
                text = "EN"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 12f
            })
            addView(Button(this@AiKeyboardService).apply {
                text = "বাং"
                setTextColor(Color.GRAY)
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 12f
            })
        }
    }

    // ==================== HELPERS ====================

    private fun insertText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun createErrorView(message: String): View {
        return TextView(this).apply {
            text = "AI Voice Keyboard\n$message"
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===")
        speechRecognizer?.destroy()
        audioRecorder?.release()
        super.onDestroy()
    }
}
