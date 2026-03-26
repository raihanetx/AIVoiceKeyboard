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
import com.aikeyboard.data.remote.api.GeminiLiveApi
import com.aikeyboard.data.remote.api.GroqWhisperApi
import com.aikeyboard.data.remote.api.ZAiApi
import com.aikeyboard.domain.model.Language
import com.aikeyboard.domain.model.TranscriptionResult
import com.aikeyboard.domain.model.TranslationResult
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
     * Get status message based on engine
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
        Log.d(TAG, "Engine selected: $engine")

        when (engine) {
            "android" -> {
                // Android doesn't need API key
                selectEngine(engine)
            }
            "groq" -> {
                if (preferencesManager.hasGroqApiKey()) {
                    selectEngine(engine)
                } else {
                    // Show API key input
                    voiceState = voiceState.copy(showApiKeyInputFor = "groq")
                    voiceInputView?.updateState(voiceState)
                }
            }
            "gemini" -> {
                if (preferencesManager.hasGeminiApiKey()) {
                    selectEngine(engine)
                } else {
                    // Show API key input
                    voiceState = voiceState.copy(showApiKeyInputFor = "gemini")
                    voiceInputView?.updateState(voiceState)
                }
            }
        }
    }

    /**
     * Handle API key entered
     */
    private fun handleApiKeyEntered(engine: String, apiKey: String) {
        Log.d(TAG, "API key entered for: $engine")

        if (apiKey.isBlank()) {
            voiceState = voiceState.copy(errorMessage = "API Key cannot be empty")
            voiceInputView?.updateState(voiceState)
            return
        }

        // Save the key
        when (engine) {
            "groq" -> preferencesManager.setGroqApiKey(apiKey)
            "gemini" -> preferencesManager.setGeminiApiKey(apiKey)
        }

        // Update state
        voiceState = voiceState.copy(
            showApiKeyInputFor = null,
            errorMessage = null
        )
        if (engine == "groq") {
            voiceState = voiceState.copy(groqApiKeyStatus = ApiKeyStatus.SAVED)
        } else if (engine == "gemini") {
            voiceState = voiceState.copy(geminiApiKeyStatus = ApiKeyStatus.SAVED)
        }

        // Select the engine
        selectEngine(engine)

        Toast.makeText(this, "$engine API Key saved!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Select an engine
     */
    private fun selectEngine(engine: String) {
        preferencesManager.setSttEngine(engine)
        voiceState = voiceState.copy(
            selectedEngine = engine,
            showApiKeyInputFor = null,
            errorMessage = null,
            statusMessage = getStatusMessage(engine)
        )
        voiceInputView?.updateState(voiceState)
        Log.d(TAG, "Engine selected and saved: $engine")
    }

    /**
     * Handle mic button click
     */
    private fun handleMicClick() {
        if (isRecording) {
            stopRecording()
        } else {
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

        // Validate API key for Groq/Gemini
        when (engine) {
            "groq" -> {
                if (!preferencesManager.hasGroqApiKey()) {
                    voiceState = voiceState.copy(
                        errorMessage = "Please enter Groq API Key first",
                        showApiKeyInputFor = "groq"
                    )
                    voiceInputView?.updateState(voiceState)
                    return
                }
            }
            "gemini" -> {
                if (!preferencesManager.hasGeminiApiKey()) {
                    voiceState = voiceState.copy(
                        errorMessage = "Please enter Gemini API Key first",
                        showApiKeyInputFor = "gemini"
                    )
                    voiceInputView?.updateState(voiceState)
                    return
                }
            }
        }

        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            voiceState = voiceState.copy(errorMessage = "Microphone permission required")
            voiceInputView?.updateState(voiceState)
            return
        }

        isRecording = true
        voiceState = voiceState.copy(
            isRecording = true,
            errorMessage = null,
            statusMessage = "🔴 Recording with ${voiceState.getEngineDisplayName()}..."
        )
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
            stopRecordingWithError("Speech recognition not available")
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
                    stopRecordingWithError(getSpeechErrorText(error))
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        showResult(matches[0])
                    } else {
                        stopRecordingWithError("No speech detected")
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
            stopRecordingWithError("Failed to start recording")
            return
        }
        voiceState = voiceState.copy(statusMessage = "🔵 Recording... Tap mic to stop")
        voiceInputView?.updateState(voiceState)
    }

    private fun startGeminiRecording() {
        val audioFile = audioRecorder?.startRecording()
        if (audioFile == null) {
            stopRecordingWithError("Failed to start recording")
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
        } else {
            val audioFile = audioRecorder?.stopRecording()

            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                voiceState = voiceState.copy(statusMessage = "Transcribing...")
                voiceInputView?.updateState(voiceState)

                CoroutineScope(Dispatchers.IO).launch {
                    val result = if (voiceState.selectedEngine == "gemini") {
                        geminiLiveApi.transcribe(audioFile, voiceState.currentLanguage.code)
                    } else {
                        groqWhisperApi.transcribe(audioFile, voiceState.currentLanguage.code)
                    }

                    withContext(Dispatchers.Main) {
                        result.fold(
                            onSuccess = { text -> showResult(text) },
                            onFailure = { error -> stopRecordingWithError(error.message ?: "Transcription failed") }
                        )
                    }
                    audioFile.delete()
                }
            } else {
                stopRecordingWithError("No audio recorded")
            }
        }

        voiceState = voiceState.copy(isRecording = false)
        voiceInputView?.updateState(voiceState)
    }

    private fun stopRecordingWithError(message: String) {
        isRecording = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        voiceState = voiceState.copy(
            isRecording = false,
            errorMessage = message,
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
        )
        voiceInputView?.updateState(voiceState)
    }

    private fun getSpeechErrorText(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Error: $error"
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
