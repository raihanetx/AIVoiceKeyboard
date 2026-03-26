package com.aikeyboard.presentation.keyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aikeyboard.R
import com.aikeyboard.core.util.AudioRecorder
import com.aikeyboard.data.local.PreferencesManager
import com.aikeyboard.data.remote.api.ApiTranscriptionResult
import com.aikeyboard.data.remote.api.GeminiLiveApi
import com.aikeyboard.data.remote.api.GroqWhisperApi
import com.aikeyboard.data.remote.api.ZAiApi
import com.aikeyboard.domain.model.Language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI Voice Keyboard - Simplified with XML layouts
 */
class AiKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "AiKeyboard"
    }

    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(this) }
    private val groqWhisperApi: GroqWhisperApi by lazy { GroqWhisperApi.getInstance(this) }
    private val geminiLiveApi: GeminiLiveApi by lazy { GeminiLiveApi.getInstance(this) }
    private val zAiApi: ZAiApi by lazy { ZAiApi.instance }

    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecorder: AudioRecorder? = null
    private var isRecording = false

    private var mainLayout: LinearLayout? = null
    private var voicePanel: View? = null

    // Voice panel views
    private var cardAndroid: View? = null
    private var cardGroq: View? = null
    private var cardGemini: View? = null
    private var radioAndroid: View? = null
    private var radioGroq: View? = null
    private var radioGemini: View? = null
    private var badgeGroq: TextView? = null
    private var badgeGemini: TextView? = null
    private var apiKeyGroqContainer: View? = null
    private var apiKeyGeminiContainer: View? = null
    private var apiKeyGroqInput: EditText? = null
    private var apiKeyGeminiInput: EditText? = null
    private var btnSaveGroqKey: Button? = null
    private var btnSaveGeminiKey: Button? = null
    private var errorContainer: View? = null
    private var errorTitle: TextView? = null
    private var errorMessage: TextView? = null
    private var errorDetails: TextView? = null
    private var btnToggleDetails: Button? = null
    private var btnCopyError: Button? = null
    private var btnEnglish: Button? = null
    private var btnBengali: Button? = null
    private var micButton: View? = null
    private var micButtonBg: View? = null
    private var statusText: TextView? = null
    private var resultCard: View? = null
    private var resultText: TextView? = null
    private var btnInsertText: Button? = null

    private var selectedEngine: String = "android"
    private var currentLanguage: Language = Language.ENGLISH

    override fun onCreate() {
        super.onCreate()
        audioRecorder = AudioRecorder(this)
    }

    override fun onCreateInputView(): View {
        return try {
            loadPreferences()
            mainLayout = createMainLayout()
            mainLayout!!
        } catch (e: Exception) {
            createErrorView("Error: ${e.message}")
        }
    }

    private fun loadPreferences() {
        selectedEngine = preferencesManager.getSttEngine()
        val savedLanguage = preferencesManager.getLanguage()
        currentLanguage = if (savedLanguage == "bn") Language.BENGALI else Language.ENGLISH
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
            addView(createPanelContent("keyboard"))
            addView(createLanguageSwitch())
        }
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER
            setPadding(8, 10, 8, 10)

            listOf("ABC" to "keyboard", "🎤" to "voice", "🌐" to "translate", "😀" to "emoji").forEach { (label, panel) ->
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
    }

    private fun createPanelContent(panel: String): View {
        return when (panel) {
            "voice" -> createVoicePanel()
            "translate" -> createTranslatePanel()
            "emoji" -> createEmojiPanel()
            else -> createKeyboardPanel()
        }
    }

    private fun createKeyboardPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 4)

            val inflater = LayoutInflater.from(context)
            val keyboardView = inflater.inflate(R.layout.keyboard_panel, this, false)

            keyboardView.findViewById<Button>(R.id.btnBackspace)?.setOnClickListener {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            keyboardView.findViewById<Button>(R.id.btnSpace)?.setOnClickListener {
                currentInputConnection?.commitText(" ", 1)
            }
            keyboardView.findViewById<Button>(R.id.btnReturn)?.setOnClickListener {
                currentInputConnection?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            }
            keyboardView.findViewById<Button>(R.id.btnEmoji)?.setOnClickListener {
                switchPanel("emoji")
            }

            for (i in 0 until (keyboardView as ViewGroup).childCount) {
                val row = keyboardView.getChildAt(i) as? ViewGroup ?: continue
                for (j in 0 until row.childCount) {
                    val btn = row.getChildAt(j) as? Button ?: continue
                    val key = btn.text.toString()
                    if (key.length == 1 && key[0].isLetter()) {
                        btn.setOnClickListener {
                            currentInputConnection?.commitText(key.lowercase(), 1)
                        }
                    }
                }
            }
            addView(keyboardView)
        }
    }

    private fun createVoicePanel(): View {
        val inflater = LayoutInflater.from(this)
        voicePanel = inflater.inflate(R.layout.voice_panel, null)

        cardAndroid = voicePanel?.findViewById(R.id.cardAndroid)
        cardGroq = voicePanel?.findViewById(R.id.cardGroq)
        cardGemini = voicePanel?.findViewById(R.id.cardGemini)
        radioAndroid = voicePanel?.findViewById(R.id.radioAndroid)
        radioGroq = voicePanel?.findViewById(R.id.radioGroq)
        radioGemini = voicePanel?.findViewById(R.id.radioGemini)
        badgeGroq = voicePanel?.findViewById(R.id.badgeGroq)
        badgeGemini = voicePanel?.findViewById(R.id.badgeGemini)
        apiKeyGroqContainer = voicePanel?.findViewById(R.id.apiKeyGroqContainer)
        apiKeyGeminiContainer = voicePanel?.findViewById(R.id.apiKeyGeminiContainer)
        apiKeyGroqInput = voicePanel?.findViewById(R.id.apiKeyGroqInput)
        apiKeyGeminiInput = voicePanel?.findViewById(R.id.apiKeyGeminiInput)
        btnSaveGroqKey = voicePanel?.findViewById(R.id.btnSaveGroqKey)
        btnSaveGeminiKey = voicePanel?.findViewById(R.id.btnSaveGeminiKey)
        errorContainer = voicePanel?.findViewById(R.id.errorContainer)
        errorTitle = voicePanel?.findViewById(R.id.errorTitle)
        errorMessage = voicePanel?.findViewById(R.id.errorMessage)
        errorDetails = voicePanel?.findViewById(R.id.errorDetails)
        btnToggleDetails = voicePanel?.findViewById(R.id.btnToggleDetails)
        btnCopyError = voicePanel?.findViewById(R.id.btnCopyError)
        btnEnglish = voicePanel?.findViewById(R.id.btnEnglish)
        btnBengali = voicePanel?.findViewById(R.id.btnBengali)
        micButton = voicePanel?.findViewById(R.id.micButton)
        micButtonBg = voicePanel?.findViewById(R.id.micButtonBg)
        statusText = voicePanel?.findViewById(R.id.statusText)
        resultCard = voicePanel?.findViewById(R.id.resultCard)
        resultText = voicePanel?.findViewById(R.id.resultText)
        btnInsertText = voicePanel?.findViewById(R.id.btnInsertText)

        // Card clicks
        cardAndroid?.setOnClickListener { selectEngine("android") }
        cardGroq?.setOnClickListener { handleGroqClick() }
        cardGemini?.setOnClickListener { handleGeminiClick() }

        // Save buttons
        btnSaveGroqKey?.setOnClickListener { saveGroqApiKey() }
        btnSaveGeminiKey?.setOnClickListener { saveGeminiApiKey() }

        // Error buttons
        btnToggleDetails?.setOnClickListener {
            errorDetails?.visibility = if (errorDetails?.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            btnToggleDetails?.text = if (errorDetails?.visibility == View.VISIBLE) "Hide" else "Details"
        }
        btnCopyError?.setOnClickListener { copyErrorToClipboard() }

        // Language buttons
        btnEnglish?.setOnClickListener { selectLanguage(Language.ENGLISH) }
        btnBengali?.setOnClickListener { selectLanguage(Language.BENGALI) }

        // Mic button
        micButton?.setOnClickListener { handleMicClick() }

        // Insert text
        btnInsertText?.setOnClickListener {
            resultText?.text?.toString()?.let { text ->
                if (text.isNotBlank()) {
                    currentInputConnection?.commitText(text, 1)
                    resultCard?.visibility = View.GONE
                }
            }
        }

        updateEngineUI()
        updateLanguageUI()
        updateBadges()

        return voicePanel!!
    }

    private fun selectEngine(engine: String) {
        selectedEngine = engine
        preferencesManager.setSttEngine(engine)
        updateEngineUI()
        hideError()
        hideApiKeyInputs()
        updateStatus()
        Log.d(TAG, "Selected: $engine")
    }

    private fun handleGroqClick() {
        hideApiKeyInputs()
        if (preferencesManager.hasGroqApiKey()) {
            selectEngine("groq")
        } else {
            apiKeyGroqContainer?.visibility = View.VISIBLE
            apiKeyGroqInput?.requestFocus()
        }
    }

    private fun handleGeminiClick() {
        hideApiKeyInputs()
        if (preferencesManager.hasGeminiApiKey()) {
            selectEngine("gemini")
        } else {
            apiKeyGeminiContainer?.visibility = View.VISIBLE
            apiKeyGeminiInput?.requestFocus()
        }
    }

    private fun hideApiKeyInputs() {
        apiKeyGroqContainer?.visibility = View.GONE
        apiKeyGeminiContainer?.visibility = View.GONE
    }

    private fun saveGroqApiKey() {
        val key = apiKeyGroqInput?.text?.toString()?.trim() ?: ""
        if (key.isEmpty()) {
            showError("Invalid", "API Key cannot be empty", null)
            return
        }
        preferencesManager.setGroqApiKey(key)
        apiKeyGroqContainer?.visibility = View.GONE
        apiKeyGroqInput?.setText("")
        updateBadges()
        selectEngine("groq")
        Toast.makeText(this, "Groq API Key saved!", Toast.LENGTH_SHORT).show()
    }

    private fun saveGeminiApiKey() {
        val key = apiKeyGeminiInput?.text?.toString()?.trim() ?: ""
        if (key.isEmpty()) {
            showError("Invalid", "API Key cannot be empty", null)
            return
        }
        preferencesManager.setGeminiApiKey(key)
        apiKeyGeminiContainer?.visibility = View.GONE
        apiKeyGeminiInput?.setText("")
        updateBadges()
        selectEngine("gemini")
        Toast.makeText(this, "Gemini API Key saved!", Toast.LENGTH_SHORT).show()
    }

    private fun updateEngineUI() {
        cardAndroid?.setBackgroundResource(R.drawable.engine_card_bg)
        cardGroq?.setBackgroundResource(R.drawable.engine_card_bg)
        cardGemini?.setBackgroundResource(R.drawable.engine_card_bg)
        radioAndroid?.setBackgroundResource(R.drawable.radio_unselected)
        radioGroq?.setBackgroundResource(R.drawable.radio_unselected)
        radioGemini?.setBackgroundResource(R.drawable.radio_unselected)

        when (selectedEngine) {
            "android" -> {
                cardAndroid?.setBackgroundResource(R.drawable.engine_card_selected_bg)
                radioAndroid?.setBackgroundResource(R.drawable.radio_selected_android)
                micButtonBg?.setBackgroundResource(R.drawable.mic_btn_bg)
            }
            "groq" -> {
                cardGroq?.setBackgroundResource(R.drawable.engine_card_selected_groq)
                radioGroq?.setBackgroundResource(R.drawable.radio_selected_groq)
                micButtonBg?.setBackgroundResource(R.drawable.mic_btn_groq_bg)
            }
            "gemini" -> {
                cardGemini?.setBackgroundResource(R.drawable.engine_card_selected_gemini)
                radioGemini?.setBackgroundResource(R.drawable.radio_selected_gemini)
                micButtonBg?.setBackgroundResource(R.drawable.mic_btn_gemini_bg)
            }
        }
    }

    private fun updateBadges() {
        if (preferencesManager.hasGroqApiKey()) {
            badgeGroq?.text = "✓ Ready"
            badgeGroq?.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            badgeGroq?.text = "Key Required"
            badgeGroq?.setTextColor(Color.parseColor("#888888"))
        }

        if (preferencesManager.hasGeminiApiKey()) {
            badgeGemini?.text = "✓ Ready"
            badgeGemini?.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            badgeGemini?.text = "Key Required"
            badgeGemini?.setTextColor(Color.parseColor("#888888"))
        }
    }

    private fun updateLanguageUI() {
        if (currentLanguage == Language.ENGLISH) {
            btnEnglish?.setBackgroundResource(R.drawable.lang_btn_selected_bg)
            btnEnglish?.setTextColor(Color.WHITE)
            btnBengali?.setBackgroundResource(R.drawable.lang_btn_bg)
            btnBengali?.setTextColor(Color.parseColor("#888888"))
        } else {
            btnBengali?.setBackgroundResource(R.drawable.lang_btn_selected_bg)
            btnBengali?.setTextColor(Color.WHITE)
            btnEnglish?.setBackgroundResource(R.drawable.lang_btn_bg)
            btnEnglish?.setTextColor(Color.parseColor("#888888"))
        }
    }

    private fun selectLanguage(language: Language) {
        currentLanguage = language
        preferencesManager.setLanguage(language.code)
        updateLanguageUI()
    }

    private fun updateStatus() {
        val msg = when (selectedEngine) {
            "android" -> "🟢 Tap mic to speak"
            "groq" -> if (preferencesManager.hasGroqApiKey()) "🔵 Tap mic to speak" else "⚠️ Enter Groq API Key"
            "gemini" -> if (preferencesManager.hasGeminiApiKey()) "🟣 Tap mic to speak" else "⚠️ Enter Gemini API Key"
            else -> "Select an engine"
        }
        statusText?.text = msg
    }

    private fun handleMicClick() {
        if (isRecording) stopRecording() else startRecording()
    }

    private fun startRecording() {
        when (selectedEngine) {
            "groq" -> {
                if (!preferencesManager.hasGroqApiKey()) {
                    showError("API Key Required", "Please enter Groq API key", null)
                    apiKeyGroqContainer?.visibility = View.VISIBLE
                    return
                }
            }
            "gemini" -> {
                if (!preferencesManager.hasGeminiApiKey()) {
                    showError("API Key Required", "Please enter Gemini API key", null)
                    apiKeyGeminiContainer?.visibility = View.VISIBLE
                    return
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showError("Permission", "Microphone permission required", null)
            return
        }

        isRecording = true
        micButtonBg?.setBackgroundResource(R.drawable.mic_btn_recording_bg)
        statusText?.text = "🔴 Recording... Tap to stop"
        hideError()
        resultCard?.visibility = View.GONE

        when (selectedEngine) {
            "android" -> startAndroidSpeechRecognizer()
            "groq" -> startGroqRecording()
            "gemini" -> startGeminiRecording()
        }
    }

    private fun startAndroidSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopRecordingWithError("Error", "Speech recognition not available", null)
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { statusText?.text = "Listening..." }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { statusText?.text = "Processing..." }
                override fun onError(error: Int) { stopRecordingWithError("Error", getSpeechErrorText(error), null) }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) showResult(matches[0])
                    else stopRecordingWithError("No Speech", "Nothing detected", null)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) statusText?.text = matches[0]
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.localeCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun startGroqRecording() {
        val audioFile = audioRecorder?.startRecording()
        if (audioFile == null) {
            stopRecordingWithError("Error", "Could not start recording", null)
        }
    }

    private fun startGeminiRecording() {
        val audioFile = audioRecorder?.startRecording()
        if (audioFile == null) {
            stopRecordingWithError("Error", "Could not start recording", null)
        }
    }

    private fun stopRecording() {
        isRecording = false
        updateEngineUI()

        if (selectedEngine == "android") {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            updateStatus()
        } else {
            val audioFile = audioRecorder?.stopRecording()
            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                statusText?.text = "Transcribing..."
                CoroutineScope(Dispatchers.IO).launch {
                    val result = if (selectedEngine == "gemini") {
                        geminiLiveApi.transcribe(audioFile, currentLanguage.code)
                    } else {
                        groqWhisperApi.transcribe(audioFile, currentLanguage.code)
                    }
                    withContext(Dispatchers.Main) { handleTranscriptionResult(result) }
                    audioFile.delete()
                }
            } else {
                stopRecordingWithError("Error", "No audio recorded", null)
            }
        }
    }

    private fun handleTranscriptionResult(result: ApiTranscriptionResult) {
        if (result.isSuccess && result.text != null) {
            showResult(result.text)
        } else {
            stopRecordingWithError("Error", result.errorMessage ?: "Failed", result.errorDetails)
        }
    }

    private fun stopRecordingWithError(title: String, message: String?, details: String?) {
        isRecording = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        updateEngineUI()
        showError(title, message ?: "Unknown error", details)
        updateStatus()
    }

    private fun showResult(text: String) {
        isRecording = false
        updateEngineUI()
        resultText?.text = text
        resultCard?.visibility = View.VISIBLE
        statusText?.text = "✓ Done"
        hideError()
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

    private fun showError(title: String, message: String, details: String?) {
        errorTitle?.text = "❌ $title"
        errorMessage?.text = message
        errorDetails?.text = details ?: ""
        errorContainer?.visibility = View.VISIBLE
        errorDetails?.visibility = View.GONE
        btnToggleDetails?.text = "Details"
    }

    private fun hideError() {
        errorContainer?.visibility = View.GONE
    }

    private fun copyErrorToClipboard() {
        val text = "${errorTitle?.text}\n${errorMessage?.text}\n${errorDetails?.text}"
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Error", text))
        Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
    }

    private fun createTranslatePanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            addView(TextView(context).apply {
                text = "🌐 Translate"
                setTextColor(Color.WHITE)
                textSize = 18f
                gravity = Gravity.CENTER
            })
        }
    }

    private fun createEmojiPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            gravity = Gravity.CENTER
            val emojis = listOf("😀", "😂", "😍", "🥰", "😎", "🤔", "👍", "👎", "❤️", "🔥")
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                emojis.forEach { emoji ->
                    addView(Button(context).apply {
                        text = emoji
                        textSize = 24f
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnClickListener { currentInputConnection?.commitText(emoji, 1) }
                    })
                }
            })
        }
    }

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
        speechRecognizer?.destroy()
        audioRecorder?.release()
        super.onDestroy()
    }
}
