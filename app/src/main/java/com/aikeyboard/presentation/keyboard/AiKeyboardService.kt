package com.aikeyboard.presentation.keyboard

import android.Manifest
import android.content.Intent
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
import com.aikeyboard.core.constants.ApiConstants
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
import java.io.File

/**
 * AI Voice Keyboard Input Method Service
 * 
 * Main keyboard service that handles text input, voice recognition,
 * and translation features.
 */
class AiKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "AiKeyboard"
    }

    // Controller for state management
    private val controller = KeyboardController()

    // Voice input view reference for updates
    private var voiceInputView: VoiceInputView? = null

    // Voice recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecorder: AudioRecorder? = null
    private var isRecording = false

    // Preferences
    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(this) }

    // API clients - lazy initialized with context
    private val groqWhisperApi: GroqWhisperApi by lazy { GroqWhisperApi.getInstance(this) }
    private val geminiLiveApi: GeminiLiveApi by lazy { GeminiLiveApi.getInstance(this) }
    private val zAiApi: ZAiApi by lazy { ZAiApi.instance }

    // Views
    private var mainLayout: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== KEYBOARD SERVICE CREATED ===")
        audioRecorder = AudioRecorder(this)
    }

    override fun onCreateInputView(): View {
        Log.d(TAG, "=== Creating keyboard view ===")
        // Load saved preferences
        loadPreferences()
        return try {
            mainLayout = createMainLayout()
            mainLayout!!
        } catch (e: Exception) {
            Log.e(TAG, "Error creating keyboard view", e)
            createErrorView("Error: ${e.message}")
        }
    }

    /**
     * Load saved preferences into controller
     */
    private fun loadPreferences() {
        val savedEngine = preferencesManager.getSttEngine()
        val savedLanguage = preferencesManager.getLanguage()
        controller.setSttEngine(savedEngine)
        controller.setLanguage(savedLanguage)
        Log.d(TAG, "Loaded preferences - Engine: $savedEngine, Language: $savedLanguage")
    }

    /**
     * Create the main keyboard layout
     */
    private fun createMainLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Add header
            addView(createHeader())

            // Add content area based on current panel
            addView(createPanelContent(controller.state.currentPanel))

            // Add language switch
            addView(createLanguageSwitch())
        }
    }

    /**
     * Create the header with panel switcher
     */
    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER
            setPadding(8, 10, 8, 10)

            val panels = listOf(
                "ABC" to AppConstants.PANEL_KEYBOARD,
                "🎤" to AppConstants.PANEL_VOICE,
                "🌐" to AppConstants.PANEL_TRANSLATE,
                "😀" to AppConstants.PANEL_EMOJI
            )

            panels.forEach { (label, panel) ->
                addView(Button(this@AiKeyboardService).apply {
                    text = label
                    setTextColor(
                        if (controller.state.currentPanel == panel) 
                            Color.parseColor("#4285F4") 
                        else 
                            Color.WHITE
                    )
                    setBackgroundColor(Color.TRANSPARENT)
                    textSize = 14f
                    setAllCaps(false)
                    setPadding(16, 8, 16, 8)
                    setOnClickListener { switchPanel(panel) }
                })
            }
        }
    }

    /**
     * Switch to a different panel
     */
    private fun switchPanel(panel: String) {
        controller.switchPanel(panel)
        setInputView(onCreateInputView())
        Log.d(TAG, "Switched to panel: $panel")
    }

    /**
     * Create content for the current panel
     */
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
            keyboardView.updateState(controller.state)
            addView(keyboardView)
        }
    }

    /**
     * Handle key press
     */
    private fun handleKeyPress(key: String) {
        when {
            key == "⌫" -> {
                controller.deleteLastChar()
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            key == "Space" -> {
                controller.appendText(" ")
                currentInputConnection?.commitText(" ", 1)
            }
            key == "↵" -> {
                currentInputConnection?.performEditorAction(
                    android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                )
            }
            key == "⇧" -> {
                controller.toggleCapsLock()
                Toast.makeText(
                    this,
                    "CAPS: ${if (controller.state.capsLock) "ON" else "OFF"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            key == "😊" -> switchPanel(AppConstants.PANEL_EMOJI)
            key == "?123" -> { /* Could switch to number/symbol keyboard */ }
            else -> {
                val char = if (controller.state.capsLock) key.uppercase() else key
                controller.appendText(char)
                currentInputConnection?.commitText(char, 1)
            }
        }
    }

    /**
     * Handle long key press
     */
    private fun handleLongKeyPress(key: String) {
        if (key == "⌫") {
            controller.clearText()
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

            voiceInputView = VoiceInputView.create(
                context = this@AiKeyboardService,
                onMicClick = { toggleVoiceRecording() },
                onEngineChange = { engine ->
                    // Save and update engine
                    preferencesManager.setSttEngine(engine)
                    controller.setSttEngine(engine)
                    // Refresh the view to show selection
                    voiceInputView?.updateState(controller.state)
                    Log.d(TAG, "Engine changed to: $engine")
                },
                onLanguageChange = { language ->
                    controller.setLanguage(language)
                    voiceInputView?.updateState(controller.state)
                },
                onInsertText = { text -> insertText(text) },
                onOpenSettings = {
                    // Open the Settings activity
                    val intent = Intent(this@AiKeyboardService, com.aikeyboard.presentation.settings.MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            )
            voiceInputView?.updateState(controller.state)
            addView(voiceInputView)
        }
    }

    /**
     * Toggle voice recording
     */
    private fun toggleVoiceRecording() {
        if (isRecording) {
            stopVoiceRecording()
        } else {
            startVoiceRecording()
        }
    }

    /**
     * Start voice recording
     */
    private fun startVoiceRecording() {
        // Check permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            controller.setRecordingStatus(false, "Microphone permission required")
            return
        }

        isRecording = true
        controller.startRecording()

        when {
            controller.state.isUsingAndroidStt -> startAndroidSpeechRecognizer()
            controller.state.isUsingGeminiStt -> startGeminiRecording()
            else -> startGroqRecording()
        }
    }

    /**
     * Start Android Speech Recognizer
     */
    private fun startAndroidSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            controller.setRecordingStatus(false, "Speech recognition not available")
            isRecording = false
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    controller.setRecordingStatus(true, "Listening... Speak now")
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    controller.setRecordingStatus(false, "Processing...")
                }

                override fun onError(error: Int) {
                    isRecording = false
                    controller.setTranscriptionResult(
                        TranscriptionResult.Error(getSpeechErrorText(error))
                    )
                }

                override fun onResults(results: Bundle?) {
                    isRecording = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        controller.setTranscriptionResult(
                            TranscriptionResult.Success(
                                text = matches[0],
                                language = controller.state.currentLanguage.code
                            )
                        )
                    } else {
                        controller.setRecordingStatus(false, "No speech detected")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        controller.setRecordingStatus(true, matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                controller.state.currentLanguage.localeCode
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
    }

    /**
     * Get error text for speech recognizer error codes
     */
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

    /**
     * Start Groq recording
     */
    private fun startGroqRecording() {
        if (!preferencesManager.isGroqApiKeyConfigured()) {
            controller.setRecordingStatus(false, "Groq API key not set. Go to Settings > API Keys")
            isRecording = false
            return
        }

        val audioFile = audioRecorder?.startRecording()
        if (audioFile == null) {
            controller.setRecordingStatus(false, "Recording failed")
            isRecording = false
            return
        }

        controller.setRecordingStatus(true, "Recording... Tap mic to stop")
    }

    /**
     * Start Gemini recording
     */
    private fun startGeminiRecording() {
        if (!preferencesManager.isGeminiApiKeyConfigured()) {
            controller.setRecordingStatus(false, "Gemini API key not set. Go to Settings > API Keys")
            isRecording = false
            return
        }

        val audioFile = audioRecorder?.startRecording()
        if (audioFile == null) {
            controller.setRecordingStatus(false, "Recording failed")
            isRecording = false
            return
        }

        controller.setRecordingStatus(true, "Recording with Gemini... Tap mic to stop")
    }

    /**
     * Stop voice recording
     */
    private fun stopVoiceRecording() {
        isRecording = false

        if (controller.state.isUsingAndroidStt) {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } else {
            val audioFile = audioRecorder?.stopRecording()
            
            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                controller.setRecordingStatus(false, "Transcribing...")
                
                CoroutineScope(Dispatchers.IO).launch {
                    // Choose API based on selected engine
                    val result = if (controller.state.isUsingGeminiStt) {
                        geminiLiveApi.transcribe(
                            audioFile,
                            controller.state.currentLanguage.code
                        )
                    } else {
                        groqWhisperApi.transcribe(
                            audioFile,
                            controller.state.currentLanguage.code
                        )
                    }
                    
                    withContext(Dispatchers.Main) {
                        result.fold(
                            onSuccess = { text ->
                                controller.setTranscriptionResult(
                                    TranscriptionResult.Success(
                                        text = text,
                                        language = controller.state.currentLanguage.code
                                    )
                                )
                            },
                            onFailure = { error ->
                                controller.setTranscriptionResult(
                                    TranscriptionResult.Error(error.message ?: "Transcription failed")
                                )
                            }
                        )
                    }
                    audioFile.delete()
                }
            }
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
                onLanguageChange = { language -> controller.setLanguage(language) },
                onInsertText = { text -> insertText(text) }
            )
            translateView.updateState(controller.state)
            addView(translateView)
        }
    }

    /**
     * Perform translation
     */
    private fun performTranslation(text: String, source: Language, target: Language) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = zAiApi.translate(text, source.code, target.code)
            
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { translatedText ->
                        controller.setTranslationResult(
                            TranslationResult.Success(
                                translatedText = translatedText,
                                sourceLanguage = source,
                                targetLanguage = target
                            )
                        )
                    },
                    onFailure = { error ->
                        controller.setTranslationResult(
                            TranslationResult.Error(error.message ?: "Translation failed")
                        )
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
                setTextColor(
                    if (controller.state.currentLanguage == Language.ENGLISH)
                        Color.parseColor("#4285F4")
                    else
                        Color.GRAY
                )
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 12f
                setOnClickListener {
                    controller.setLanguage(Language.ENGLISH)
                    switchPanel(controller.state.currentPanel)
                }
            })
            addView(Button(this@AiKeyboardService).apply {
                text = "বাং"
                setTextColor(
                    if (controller.state.currentLanguage == Language.BENGALI)
                        Color.parseColor("#4285F4")
                    else
                        Color.GRAY
                )
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 12f
                setOnClickListener {
                    controller.setLanguage(Language.BENGALI)
                    switchPanel(controller.state.currentPanel)
                }
            })
        }
    }

    // ==================== HELPERS ====================

    /**
     * Insert text into the current input field
     */
    private fun insertText(text: String) {
        currentInputConnection?.commitText(text, 1)
        controller.appendText(text)
    }

    /**
     * Create an error view
     */
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

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput")
    }

    override fun onStartInputView(editorInfo: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        Log.d(TAG, "=== KEYBOARD VISIBLE - READY TO TYPE ===")
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "onFinishInput")
    }

    override fun onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===")
        speechRecognizer?.destroy()
        audioRecorder?.release()
        super.onDestroy()
    }
}
