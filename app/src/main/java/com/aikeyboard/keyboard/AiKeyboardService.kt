package com.aikeyboard.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import com.aikeyboard.AiKeyboardApp
import com.aikeyboard.voice.GroqWhisperClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AiKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "AiKeyboard"
    }

    private var currentText = StringBuilder()
    private var capsLock = false
    private var currentPanel = "keyboard" // keyboard, voice, translate, emoji
    private var currentLanguage = "en" // en, bn
    private var sttEngine = "android" // android, groq

    // Voice recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    // Views
    private var mainLayout: LinearLayout? = null
    private var voiceStatusText: TextView? = null
    private var voiceResultText: TextView? = null
    private var voiceResultCard: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== KEYBOARD SERVICE CREATED ===")
    }

    override fun onCreateInputView(): View {
        Log.d(TAG, "=== Creating keyboard view ===")
        return try {
            mainLayout = createMainLayout()
            mainLayout!!
        } catch (e: Exception) {
            Log.e(TAG, "Error creating keyboard view", e)
            createErrorView("Error: ${e.message}")
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

            // Add header
            addView(createHeader())

            // Add content area (will be switched between panels)
            when (currentPanel) {
                "voice" -> addView(createVoicePanel())
                "translate" -> addView(createTranslatePanel())
                "emoji" -> addView(createEmojiPanel())
                else -> addView(createKeyboardPanel())
            }

            // Add language switch
            addView(createLanguageSwitch())
        }
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER
            setPadding(8, 10, 8, 10)

            val panels = listOf("ABC" to "keyboard", "🎤" to "voice", "🌐" to "translate", "😀" to "emoji")
            panels.forEach { (label, panel) ->
                addView(Button(this@AiKeyboardService).apply {
                    text = label
                    setTextColor(if (currentPanel == panel) Color.parseColor("#4285F4") else Color.WHITE)
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
        currentPanel = panel
        setInputView(onCreateInputView())
        Log.d(TAG, "Switched to panel: $panel")
    }

    // ==================== KEYBOARD PANEL ====================
    private fun createKeyboardPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 4)

            val rows = if (currentLanguage == "en") {
                listOf(
                    listOf("q","w","e","r","t","y","u","i","o","p"),
                    listOf("a","s","d","f","g","h","j","k","l"),
                    listOf("⇧","z","x","c","v","b","n","m","⌫"),
                    listOf("?123","😊","Space",".","↵")
                )
            } else {
                listOf(
                    listOf("১","২","৩","৪","৫","৬","৭","৮","৯","০"),
                    listOf("ৌ","ৈ","া","ী","ূ","ব","হ","গ","দ","জ"),
                    listOf("ো","ে","্","ি","ু","প","র","ক","ত","চ"),
                    listOf("ং","ম","ন","ণ","স","ও","য","শ","খ","ঃ")
                )
            }

            rows.forEach { row ->
                addView(LinearLayout(this@AiKeyboardService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    row.forEach { key ->
                        addView(createKeyButton(key))
                    }
                })
            }
        }
    }

    private fun createKeyButton(key: String): Button {
        return Button(this).apply {
            text = key
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2D2D2D"))
            textSize = 16f
            setAllCaps(false)

            val width = when {
                key == "Space" -> 150
                key == "⇧" || key == "⌫" -> 50
                key == "?123" || key == "😊" || key == "." -> 45
                else -> 35
            }

            layoutParams = LinearLayout.LayoutParams(dpToPx(width), dpToPx(44)).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }

            setOnClickListener { handleKeyPress(key) }
            setOnLongClickListener {
                if (key == "⌫") {
                    currentText.clear()
                    currentInputConnection?.deleteSurroundingText(10000, 0)
                    true
                } else false
            }
        }
    }

    private fun handleKeyPress(key: String) {
        when {
            key == "⌫" -> {
                if (currentText.isNotEmpty()) currentText.deleteCharAt(currentText.length - 1)
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            key == "Space" -> {
                currentText.append(" ")
                currentInputConnection?.commitText(" ", 1)
            }
            key == "↵" -> currentInputConnection?.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            key == "⇧" -> {
                capsLock = !capsLock
                Toast.makeText(this, "CAPS: ${if (capsLock) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            }
            key == "😊" -> switchPanel("emoji")
            else -> {
                val char = if (capsLock) key.uppercase() else key
                currentText.append(char)
                currentInputConnection?.commitText(char, 1)
            }
        }
    }

    // ==================== VOICE PANEL ====================
    private fun createVoicePanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER_HORIZONTAL

            // Engine switcher
            addView(LinearLayout(this@AiKeyboardService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 8)

                addView(createChip("Android (Offline)", sttEngine == "android") { 
                    sttEngine = "android"
                    switchPanel("voice")
                })
                addView(View(this@AiKeyboardService).apply { layoutParams = LinearLayout.LayoutParams(16, 1) })
                addView(createChip("Groq (Online)", sttEngine == "groq") { 
                    sttEngine = "groq"
                    switchPanel("voice")
                })
            })

            // Language switcher
            addView(LinearLayout(this@AiKeyboardService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 12)

                addView(createChip("English", currentLanguage == "en") { 
                    currentLanguage = "en"
                    switchPanel("voice")
                })
                addView(View(this@AiKeyboardService).apply { layoutParams = LinearLayout.LayoutParams(16, 1) })
                addView(createChip("বাংলা", currentLanguage == "bn") { 
                    currentLanguage = "bn"
                    switchPanel("voice")
                })
            })

            // Mic button
            addView(Button(this@AiKeyboardService).apply {
                text = "🎤"
                textSize = 32f
                setBackgroundColor(Color.parseColor("#4285F4"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(dpToPx(70), dpToPx(70)).apply {
                    setMargins(0, 8, 0, 8)
                }
                setOnClickListener { toggleVoiceRecording() }
            })

            // Status text
            voiceStatusText = TextView(this@AiKeyboardService).apply {
                text = "Tap mic to speak"
                setTextColor(Color.GRAY)
                textSize = 12f
                gravity = Gravity.CENTER
            }
            addView(voiceStatusText)

            // Result card (hidden initially)
            voiceResultCard = LinearLayout(this@AiKeyboardService).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#1B5E20"))
                setPadding(12, 12, 12, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 16, 0, 0) }
                visibility = View.GONE

                voiceResultText = TextView(this@AiKeyboardService).apply {
                    text = ""
                    setTextColor(Color.WHITE)
                    textSize = 14f
                }
                addView(voiceResultText)

                addView(Button(this@AiKeyboardService).apply {
                    text = "Insert Text"
                    setTextColor(Color.parseColor("#1B5E20"))
                    setBackgroundColor(Color.WHITE)
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(36)
                    ).apply { setMargins(0, 8, 0, 0) }
                    setOnClickListener {
                        voiceResultText?.text?.toString()?.let { text ->
                            currentInputConnection?.commitText(text, 1)
                            currentText.append(text)
                        }
                        voiceResultCard?.visibility = View.GONE
                    }
                })
            }
            addView(voiceResultCard)
        }
    }

    private fun createChip(text: String, selected: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 11f
            setTextColor(if (selected) Color.WHITE else Color.GRAY)
            setBackgroundColor(if (selected) Color.parseColor("#4285F4") else Color.TRANSPARENT)
            setPadding(12, 6, 12, 6)
            setAllCaps(false)
            setOnClickListener { onClick() }
        }
    }

    private fun toggleVoiceRecording() {
        if (isRecording) {
            stopVoiceRecording()
        } else {
            startVoiceRecording()
        }
    }

    private fun startVoiceRecording() {
        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            voiceStatusText?.text = "Microphone permission required"
            return
        }

        isRecording = true
        voiceStatusText?.text = "Listening..."
        voiceResultCard?.visibility = View.GONE

        if (sttEngine == "android") {
            startAndroidSpeechRecognizer()
        } else {
            startGroqRecording()
        }
    }

    private fun startAndroidSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            voiceStatusText?.text = "Speech recognition not available"
            isRecording = false
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    voiceStatusText?.text = "Listening... Speak now"
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    voiceStatusText?.text = "Processing..."
                }
                override fun onError(error: Int) {
                    isRecording = false
                    voiceStatusText?.text = "Error: ${getErrorText(error)}"
                }
                override fun onResults(results: Bundle?) {
                    isRecording = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        showResult(matches[0])
                    } else {
                        voiceStatusText?.text = "No speech detected"
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        voiceStatusText?.text = matches[0]
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLanguage == "bn") "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun getErrorText(error: Int): String {
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

    private fun startGroqRecording() {
        if (AiKeyboardApp.GROQ_API_KEY.isBlank()) {
            voiceStatusText?.text = "Groq API key not set"
            isRecording = false
            return
        }

        try {
            val fileName = "${cacheDir.absolutePath}/voice_${System.currentTimeMillis()}.3gp"
            audioFile = File(fileName)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(fileName)
                prepare()
                start()
            }

            voiceStatusText?.text = "Recording... Tap mic to stop"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            voiceStatusText?.text = "Recording failed"
            isRecording = false
        }
    }

    private fun stopVoiceRecording() {
        isRecording = false

        if (sttEngine == "android") {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } else {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recorder", e)
            }
            mediaRecorder = null

            // Transcribe with Groq
            audioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    voiceStatusText?.text = "Transcribing..."
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = GroqWhisperClient.instance.transcribeAudio(file, currentLanguage)
                        withContext(Dispatchers.Main) {
                            result.onSuccess { text -> showResult(text) }
                            result.onFailure { e -> 
                                voiceStatusText?.text = "Error: ${e.message}"
                            }
                        }
                        file.delete()
                    }
                }
            }
            audioFile = null
        }
    }

    private fun showResult(text: String) {
        voiceStatusText?.text = "Done!"
        voiceResultText?.text = text
        voiceResultCard?.visibility = View.VISIBLE
    }

    // ==================== TRANSLATE PANEL ====================
    private fun createTranslatePanel(): LinearLayout {
        var inputEditText: EditText? = null
        var resultTextView: TextView? = null
        var resultCard: LinearLayout? = null

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)

            // Language direction
            addView(LinearLayout(this@AiKeyboardService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                addView(createChip("EN → বাং", currentLanguage == "en") { currentLanguage = "en"; switchPanel("translate") })
                addView(View(this@AiKeyboardService).apply { layoutParams = LinearLayout.LayoutParams(16, 1) })
                addView(createChip("বাং → EN", currentLanguage == "bn") { currentLanguage = "bn"; switchPanel("translate") })
            })

            // Input field
            inputEditText = EditText(this@AiKeyboardService).apply {
                hint = "Enter text to translate"
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
                textSize = 14f
                setBackgroundColor(Color.parseColor("#2D2D2D"))
                setPadding(12, 12, 12, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 12, 0, 8) }
            }
            addView(inputEditText)

            // Translate button
            addView(Button(this@AiKeyboardService).apply {
                text = "Translate"
                setBackgroundColor(Color.parseColor("#4285F4"))
                setTextColor(Color.WHITE)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(40)
                )
                setOnClickListener {
                    val input = inputEditText?.text?.toString() ?: ""
                    if (input.isNotBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val targetLang = if (currentLanguage == "en") "bn" else "en"
                            val result = com.aikeyboard.translation.ZAiClient.translate(input, currentLanguage, targetLang)
                            withContext(Dispatchers.Main) {
                                result.onSuccess { text ->
                                    resultTextView?.text = text
                                    resultCard?.visibility = View.VISIBLE
                                }
                                result.onFailure { e ->
                                    Toast.makeText(this@AiKeyboardService, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            })

            // Result card
            resultCard = LinearLayout(this@AiKeyboardService).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#2D2D2D"))
                setPadding(12, 12, 12, 12)
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 12, 0, 0) }

                resultTextView = TextView(this@AiKeyboardService).apply {
                    setTextColor(Color.WHITE)
                    textSize = 14f
                }
                addView(resultTextView)

                addView(Button(this@AiKeyboardService).apply {
                    text = "Insert"
                    setTextColor(Color.parseColor("#4285F4"))
                    setBackgroundColor(Color.TRANSPARENT)
                    textSize = 12f
                    setOnClickListener {
                        resultTextView?.text?.toString()?.let { text ->
                            currentInputConnection?.commitText(text, 1)
                        }
                    }
                })
            }
            addView(resultCard)
        }
    }

    // ==================== EMOJI PANEL ====================
    private fun createEmojiPanel(): LinearLayout {
        val emojis = listOf(
            listOf("😀","😃","😄","😁","😆","😅","🤣","😂","🙂","😊"),
            listOf("❤️","🔥","✨","👍","👎","👏","🎉","💯","🙌","💪"),
            listOf("🥰","😍","🤩","😘","😎","🤔","😢","😭","😤","🤗"),
            listOf("🙏","👋","🤝","✌️","🤞","👌","✋","👏","🤲","👐")
        )

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            gravity = Gravity.CENTER

            emojis.forEach { row ->
                addView(LinearLayout(this@AiKeyboardService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    row.forEach { emoji ->
                        addView(TextView(this@AiKeyboardService).apply {
                            text = emoji
                            textSize = 24f
                            setPadding(8, 8, 8, 8)
                            setOnClickListener {
                                currentInputConnection?.commitText(emoji, 1)
                            }
                        })
                    }
                })
            }
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
                setTextColor(if (currentLanguage == "en") Color.parseColor("#4285F4") else Color.GRAY)
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 12f
                setOnClickListener { currentLanguage = "en"; switchPanel(currentPanel) }
            })
            addView(Button(this@AiKeyboardService).apply {
                text = "বাং"
                setTextColor(if (currentLanguage == "bn") Color.parseColor("#4285F4") else Color.GRAY)
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 12f
                setOnClickListener { currentLanguage = "bn"; switchPanel(currentPanel) }
            })
        }
    }

    // ==================== HELPERS ====================
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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
        try {
            mediaRecorder?.release()
        } catch (e: Exception) { }
        super.onDestroy()
    }
}
