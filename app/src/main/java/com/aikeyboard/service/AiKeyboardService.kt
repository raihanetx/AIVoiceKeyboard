package com.aikeyboard.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.aikeyboard.R
import com.aikeyboard.BuildConfig
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AiKeyboardService : android.inputmethodservice.InputMethodService() {

    private enum class KeyboardMode { ENGLISH, BANGLA, NUMBERS }
    private enum class VoiceEngine { ANDROID, GROQ }
    private enum class KeyModifier { NORMAL, SHIFT, CAPS_LOCK }

    private var currentMode = KeyboardMode.ENGLISH
    private var currentEngine = VoiceEngine.ANDROID
    private var keyModifier = KeyModifier.NORMAL
    private var isVoiceActive = false
    private var isRecording = false

    // Views
    private lateinit var keyboardContainer: FrameLayout
    private lateinit var voiceBar: LinearLayout
    private lateinit var suggestionBar: LinearLayout
    private lateinit var btnEngineAndroid: TextView
    private lateinit var btnEngineGroq: TextView
    private lateinit var visualizer: LinearLayout
    private lateinit var visualizerBars: List<View>

    // Voice recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val client = OkHttpClient()

    // Keyboard layouts
    private val englishKeys = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("⇧", "Z", "X", "C", "V", "B", "N", "M", "⌫"),
        listOf("?123", ",", "Space", ".", "↵")
    )

    private val banglaKeys = listOf(
        listOf("ঔ", "ঐ", "আ", "ঈ", "ঊ", "ঋ", "এ", "অ", "ই", "উ"),
        listOf("ও", "্য", "ড়", "ঢ়", "ৎ", "ং", "ঃ", "ঁ", "ক", "খ"),
        listOf("⇧", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ", "ঞ", "⌫"),
        listOf("?123", "ট", "ঠ", "ড", "ঢ", "ণ", "ত", "থ", "দ", "ধ"),
        listOf("ন", "প", "ফ", "ব", "ভ", "ম", "য", "র", "ল", "শ"),
        listOf("?123", "ষ", "স", "হ", "Space", "।", "↵")
    )

    private val numberKeys = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\""),
        listOf("⇧", "#+=", ".", ",", "?", "!", "'", "⌫"),
        listOf("ABC", ", ", "Space", ".", "↵")
    )

    private val symbolKeys = listOf(
        listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "="),
        listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•"),
        listOf("⇧", "1/2", ".", ",", "?", "!", "'", "⌫"),
        listOf("ABC", ", ", "Space", ".", "↵")
    )

    private var showingSymbols = false

    override fun onCreate() {
        super.onCreate()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)
        
        keyboardContainer = root.findViewById(R.id.keyboardContainer)
        voiceBar = root.findViewById(R.id.voiceBar)
        suggestionBar = root.findViewById(R.id.suggestionBar)
        btnEngineAndroid = root.findViewById(R.id.btnEngineAndroid)
        btnEngineGroq = root.findViewById(R.id.btnEngineGroq)
        visualizer = root.findViewById(R.id.visualizer)
        
        visualizerBars = listOf(
            root.findViewById(R.id.bar1),
            root.findViewById(R.id.bar2),
            root.findViewById(R.id.bar3),
            root.findViewById(R.id.bar4),
            root.findViewById(R.id.bar5)
        )

        root.findViewById<ImageButton>(R.id.btnVoice).setOnClickListener {
            toggleVoiceMode()
        }

        root.findViewById<ImageButton>(R.id.btnLanguage).setOnClickListener {
            cycleLanguage()
        }

        root.findViewById<ImageButton>(R.id.btnEmoji).setOnClickListener {
            Toast.makeText(this, "Emoji keyboard coming soon!", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<ImageButton>(R.id.btnClipboard).setOnClickListener {
            Toast.makeText(this, "Clipboard coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnEngineAndroid.setOnClickListener { selectEngine(VoiceEngine.ANDROID) }
        btnEngineGroq.setOnClickListener { selectEngine(VoiceEngine.GROQ) }

        root.findViewById<ImageButton>(R.id.btnStopVoice).setOnClickListener {
            stopVoice()
        }

        buildKeyboard()
        
        return root
    }

    private fun buildKeyboard() {
        keyboardContainer.removeAllViews()
        
        val keys = when (currentMode) {
            KeyboardMode.ENGLISH -> englishKeys
            KeyboardMode.BANGLA -> banglaKeys
            KeyboardMode.NUMBERS -> if (showingSymbols) symbolKeys else numberKeys
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 4, 8, 8)
        }

        for (row in keys) {
            val rowView = createKeyboardRow(row)
            container.addView(rowView)
        }

        keyboardContainer.addView(container)
    }

    private fun createKeyboardRow(keys: List<String>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = android.view.Gravity.CENTER
            weightSum = keys.size.toFloat()

            for (key in keys) {
                val keyView = createKeyView(key)
                addView(keyView)
            }
        }
    }

    private fun createKeyView(key: String): View {
        val isSpecial = key in listOf("⇧", "⌫", "?123", "ABC", "#+=", "1/2", "Space", "↵")
        val isAction = key == "↵"
        val isSpace = key == "Space"

        val displayText = when (key) {
            "Space" -> " "
            "⌫" -> "⌫"
            "⇧" -> if (keyModifier == KeyModifier.CAPS_LOCK) "⇪" else "⇧"
            "?123" -> "?123"
            "ABC" -> "ABC"
            "#+=" -> "#+="
            "1/2" -> "1/2"
            "↵" -> "↵"
            else -> {
                if (keyModifier == KeyModifier.NORMAL && key.isNotEmpty() && key[0].isLetter()) {
                    key.lowercase()
                } else {
                    key
                }
            }
        }

        return TextView(this).apply {
            text = displayText
            textSize = if (isSpace) 14f else if (key.length > 2) 12f else 18f
            gravity = android.view.Gravity.CENTER
            setTextColor(if (isAction) Color.WHITE else Color.parseColor("#1F1F1F"))
            
            layoutParams = LinearLayout.LayoutParams(
                0,
                if (isSpace) (48 * resources.displayMetrics.density).toInt() 
                else (44 * resources.displayMetrics.density).toInt(),
                when {
                    isSpace -> 3f
                    key in listOf("⇧", "⌫") -> 1.5f
                    key in listOf("?123", "ABC", "#+=", "1/2") -> 1.5f
                    key == "↵" -> 1.5f
                    else -> 1f
                }
            ).apply {
                setMargins(2, 3, 2, 3)
            }

            background = when {
                isAction -> AppCompatResources.getDrawable(context, R.drawable.key_action_bg)
                isSpecial -> AppCompatResources.getDrawable(context, R.drawable.key_special_bg)
                else -> AppCompatResources.getDrawable(context, R.drawable.key_bg)
            }

            setOnClickListener { handleKeyPress(key) }
            
            if (!isSpecial && !isSpace) {
                setOnLongClickListener {
                    handleKeyLongPress(key)
                    true
                }
            }
        }
    }

    private fun handleKeyPress(key: String) {
        when (key) {
            "⌫" -> handleBackspace()
            "⇧" -> handleShift()
            "?123" -> {
                currentMode = KeyboardMode.NUMBERS
                showingSymbols = false
                buildKeyboard()
            }
            "ABC" -> {
                currentMode = KeyboardMode.ENGLISH
                showingSymbols = false
                buildKeyboard()
            }
            "#+=" -> {
                showingSymbols = true
                buildKeyboard()
            }
            "1/2" -> {
                showingSymbols = false
                buildKeyboard()
            }
            "↵" -> handleEnter()
            "Space" -> commitText(" ")
            else -> {
                val text = when (keyModifier) {
                    KeyModifier.NORMAL -> key.lowercase()
                    KeyModifier.SHIFT -> key.uppercase()
                    KeyModifier.CAPS_LOCK -> key.uppercase()
                }
                commitText(text)
                if (keyModifier == KeyModifier.SHIFT) {
                    keyModifier = KeyModifier.NORMAL
                    buildKeyboard()
                }
            }
        }
    }

    private fun handleKeyLongPress(key: String) {
        val alternatives = when (key.lowercase()) {
            "a" -> "àáâäæãåā"
            "e" -> "èéêëēėę"
            "i" -> "ìíîïī"
            "o" -> "òóôöøõō"
            "u" -> "ùúûüū"
            "c" -> "çćč"
            "n" -> "ñń"
            "s" -> "ßśš"
            "z" -> "źżž"
            else -> return
        }
        if (alternatives.isNotEmpty()) {
            commitText(alternatives[0].toString())
        }
    }

    private fun handleBackspace() {
        currentInputConnection?.let { ic ->
            val selectedText = ic.getSelectedText(0)
            if (selectedText.isNullOrEmpty()) {
                ic.deleteSurroundingText(1, 0)
            } else {
                ic.commitText("", 1)
            }
        }
    }

    private fun handleShift() {
        keyModifier = when (keyModifier) {
            KeyModifier.NORMAL -> KeyModifier.SHIFT
            KeyModifier.SHIFT -> KeyModifier.CAPS_LOCK
            KeyModifier.CAPS_LOCK -> KeyModifier.NORMAL
        }
        buildKeyboard()
    }

    private fun handleEnter() {
        currentInputConnection?.let { ic ->
            val action = currentInputEditorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            when (action) {
                EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_SEND, EditorInfo.IME_ACTION_DONE -> {
                    ic.performEditorAction(action)
                }
                else -> {
                    ic.commitText("\n", 1)
                }
            }
        }
    }

    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun cycleLanguage() {
        currentMode = when (currentMode) {
            KeyboardMode.ENGLISH -> KeyboardMode.BANGLA
            KeyboardMode.BANGLA -> KeyboardMode.NUMBERS
            KeyboardMode.NUMBERS -> KeyboardMode.ENGLISH
        }
        keyModifier = KeyModifier.NORMAL
        showingSymbols = false
        buildKeyboard()
        
        Toast.makeText(this, 
            when (currentMode) {
                KeyboardMode.ENGLISH -> "English Keyboard"
                KeyboardMode.BANGLA -> "Bangla Keyboard"
                KeyboardMode.NUMBERS -> "Numbers & Symbols"
            }, 
            Toast.LENGTH_SHORT
        ).show()
    }

    // Voice Recognition
    private fun toggleVoiceMode() {
        if (isVoiceActive) {
            stopVoice()
        } else {
            startVoice()
        }
    }

    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please grant microphone permission", Toast.LENGTH_LONG).show()
            return
        }

        isVoiceActive = true
        voiceBar.visibility = View.VISIBLE
        isRecording = true

        when (currentEngine) {
            VoiceEngine.ANDROID -> startAndroidRecognition()
            VoiceEngine.GROQ -> startGroqRecording()
        }

        startVisualizerAnimation()
    }

    private fun stopVoice() {
        isVoiceActive = false
        isRecording = false
        voiceBar.visibility = View.GONE
        stopVisualizerAnimation()

        when (currentEngine) {
            VoiceEngine.ANDROID -> stopAndroidRecognition()
            VoiceEngine.GROQ -> stopGroqRecording()
        }
    }

    private fun selectEngine(engine: VoiceEngine) {
        currentEngine = engine
        updateEngineUI()
        
        if (isVoiceActive) {
            stopVoice()
            startVoice()
        }
    }

    private fun updateEngineUI() {
        when (currentEngine) {
            VoiceEngine.ANDROID -> {
                btnEngineAndroid.setBackgroundResource(R.drawable.engine_active_bg)
                btnEngineAndroid.setTextColor(Color.WHITE)
                btnEngineGroq.setBackgroundColor(Color.TRANSPARENT)
                btnEngineGroq.setTextColor(Color.parseColor("#5F6368"))
            }
            VoiceEngine.GROQ -> {
                btnEngineGroq.setBackgroundResource(R.drawable.engine_active_bg)
                btnEngineGroq.setTextColor(Color.WHITE)
                btnEngineAndroid.setBackgroundColor(Color.TRANSPARENT)
                btnEngineAndroid.setTextColor(Color.parseColor("#5F6368"))
            }
        }
    }

    // Android Offline Recognition
    private fun startAndroidRecognition() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(this@AiKeyboardService, "Listening...", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (isRecording) {
                    startAndroidRecognition()
                }
            }

            override fun onError(error: Int) {
                if (isRecording && error != SpeechRecognizer.ERROR_NO_MATCH) {
                    startAndroidRecognition()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    commitText(text)
                }
                if (isRecording) {
                    startAndroidRecognition()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentMode == KeyboardMode.BANGLA) "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun stopAndroidRecognition() {
        speechRecognizer?.stopListening()
    }

    // Groq Online Recognition
    private fun startGroqRecording() {
        try {
            audioFile = File(cacheDir, "voice_recording.m4a")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
            Toast.makeText(this, "Recording for Groq...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_LONG).show()
            stopVoice()
        }
    }

    private fun stopGroqRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            audioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    sendToGroq(file)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        audioFile = null
    }

    private fun sendToGroq(audioFile: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-large-v3")
                    .addFormDataPart(
                        "file",
                        "audio.m4a",
                        audioFile.asRequestBody("audio/mp4".toMediaType())
                    )
                    .addFormDataPart("language", if (currentMode == KeyboardMode.BANGLA) "bn" else "en")
                    .build()

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val text = parseGroqResponse(responseBody)
                    withContext(Dispatchers.Main) {
                        commitText(text)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AiKeyboardService,
                            "Groq error: ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AiKeyboardService,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun parseGroqResponse(json: String): String {
        val textKey = "\"text\":\""
        val start = json.indexOf(textKey)
        if (start == -1) return ""
        
        val textStart = start + textKey.length
        val textEnd = json.indexOf("\"", textStart)
        if (textEnd == -1) return ""
        
        return json.substring(textStart, textEnd)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    // Visualizer
    private var visualizerJob: Job? = null

    private fun startVisualizerAnimation() {
        visualizerJob = scope.launch {
            while (isRecording) {
                for (i in visualizerBars.indices) {
                    launch(Dispatchers.Main) {
                        val height = (20 + (Math.random() * 30)).toInt()
                        visualizerBars[i].layoutParams.height = height * resources.displayMetrics.density.toInt()
                        visualizerBars[i].requestLayout()
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopVisualizerAnimation() {
        visualizerJob?.cancel()
        visualizerJob = null
        
        visualizerBars.forEach { bar ->
            bar.layoutParams.height = (20 * resources.displayMetrics.density).toInt()
            bar.requestLayout()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        scope.cancel()
        mediaRecorder?.release()
    }
}
