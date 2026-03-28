package com.aikeyboard.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import com.aikeyboard.BuildConfig
import com.aikeyboard.R
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class PixelProKeyboard : android.inputmethodservice.InputMethodService() {

    // --- STATE ---
    private var isCaps = false
    private var currentLang = "en"
    private var voiceEngine = "android"

    // --- THEME COLORS (Light Mode) ---
    private val colorBg = Color.parseColor("#E8EAED")
    private val colorKeyBg = Color.parseColor("#FFFFFF")
    private val colorKeyText = Color.parseColor("#202124")
    private val colorSpecialBg = Color.parseColor("#DADCE0")
    private val colorAccent = Color.parseColor("#1A73E8")
    private val colorIcon = Color.parseColor("#5f6368")
    private val colorDivider = Color.parseColor("#1F000000")
    private val colorError = Color.parseColor("#EA4335")

    // --- VIEWS ---
    private lateinit var container: LinearLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var suggestionBar: LinearLayout
    private lateinit var voiceBar: LinearLayout
    private lateinit var keyGridContainer: LinearLayout
    private lateinit var tvSugg1: TextView
    private lateinit var tvSugg2: TextView
    private lateinit var tvSugg3: TextView
    private lateinit var tvVoiceLang: TextView
    private lateinit var tvVoiceStatus: TextView
    private lateinit var visualizerBars: MutableList<View>

    // --- VOICE ---
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isVoiceActive = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // Display metrics for dp conversion
    private lateinit var displayMetrics: DisplayMetrics
    private val mainHandler = Handler(Looper.getMainLooper())

    // Bangla layout
    private val banglaRows = listOf(
        listOf("ঔ","ঐ","আ","ঈ","ঊ","ঋ","এ","অ","ই","উ"),
        listOf("ও","্য","ড়","ঢ়","ৎ","ং","ঃ","ঁ","ক","খ"),
        listOf("⇧","গ","ঘ","ঙ","চ","ছ","জ","ঝ","ঞ","⌫"),
        listOf("123","ট","ঠ","ড","ঢ","ণ","ত","থ","দ","ধ"),
        listOf("ন","প","ফ","ব","ভ","ম","য","র","ল","শ"),
        listOf("123","ষ","স","হ","SPACE","।","↵")
    )

    override fun onCreate() {
        super.onCreate()
        try {
            displayMetrics = resources.displayMetrics
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        } catch (e: Exception) {
            android.util.Log.e("PixelProKeyboard", "onCreate error", e)
        }
    }

    override fun onCreateInputView(): View {
        return try {
            container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(colorBg)
                setPadding(0, dp(4), 0, dp(16))
            }
            visualizerBars = mutableListOf()
            setupTopArea()
            setupKeyboardGrid()
            container
        } catch (e: Exception) {
            android.util.Log.e("PixelProKeyboard", "onCreateInputView error", e)
            TextView(this).apply { text = "Keyboard error: ${e.message}" }
        }
    }

    private fun setupTopArea() {
        val topArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. TOOLBAR
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
            setPadding(dp(4), 0, dp(4), 0)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(colorBg)
        }

        // Left icons: Emoji, Clipboard, Credentials
        toolbar.addView(makeToolbarBtn(R.drawable.ic_smile) { show("Emoji coming soon") })
        toolbar.addView(makeToolbarBtn(R.drawable.ic_clipboard) { show("Clipboard coming soon") })
        toolbar.addView(makeToolbarBtn(R.drawable.ic_key) { show("Credentials coming soon") })

        // Spacer
        toolbar.addView(View(this).apply { 
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f) 
        })

        // Right icons: Language, Settings, Mic
        toolbar.addView(makeToolbarBtn(R.drawable.ic_globe) { swapLanguage() })
        toolbar.addView(makeToolbarBtn(R.drawable.ic_settings) { show("Settings coming soon") })
        toolbar.addView(makeToolbarBtn(R.drawable.ic_mic) { toggleVoiceBar() })

        topArea.addView(toolbar)

        // 2. SUGGESTION BAR
        suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            weightSum = 3f
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
        }

        tvSugg1 = makeSuggTv(Gravity.START)
        tvSugg2 = makeSuggTv(Gravity.CENTER)
        tvSugg3 = makeSuggTv(Gravity.END)

        suggestionBar.addView(tvSugg1)
        suggestionBar.addView(makeDivider())
        suggestionBar.addView(tvSugg2)
        suggestionBar.addView(makeDivider())
        suggestionBar.addView(tvSugg3)

        topArea.addView(suggestionBar)

        // 3. VOICE BAR
        voiceBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
        }

        // Language chip
        tvVoiceLang = TextView(this).apply {
            text = "English (Android)"
            setTextColor(colorKeyText)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedDrawable(colorSpecialBg, 16)
            isClickable = true
            isFocusable = true
            setOnClickListener { swapVoiceEngine() }
        }
        voiceBar.addView(tvVoiceLang)

        // Center container for status + visualizer
        val centerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
        }

        // Status text
        tvVoiceStatus = TextView(this).apply {
            text = "Tap mic to start"
            textSize = 12f
            setTextColor(colorIcon)
            gravity = Gravity.CENTER
        }
        centerContainer.addView(tvVoiceStatus)

        // Visualizer
        val visualizer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(20))
            gravity = Gravity.CENTER
        }

        visualizerBars.clear()
        repeat(5) {
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), dp(8)).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
                setBackgroundColor(colorAccent)
                alpha = 0.3f
            }
            visualizerBars.add(bar)
            visualizer.addView(bar)
        }
        centerContainer.addView(visualizer)
        voiceBar.addView(centerContainer)

        // Stop button
        val stopBtn = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            setImageResource(R.drawable.ic_stop)
            setColorFilter(Color.WHITE)
            background = roundedDrawable(colorError, 20)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            isClickable = true
            isFocusable = true
            setOnClickListener { toggleVoiceBar() }
        }
        voiceBar.addView(stopBtn)

        topArea.addView(voiceBar)
        container.addView(topArea)
    }

    private fun setupKeyboardGrid() {
        keyGridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), 0, dp(6), 0)
        }

        // English QWERTY layout
        addKeyRow(listOf("q","w","e","r","t","y","u","i","o","p"))
        addKeyRow(listOf("a","s","d","f","g","h","j","k","l"))
        addKeyRow(listOf("⇧","z","x","c","v","b","n","m","⌫"), hasSpecial = true)
        addKeyRow(listOf("123",",","SPACE",".","↵"), isBottom = true)

        container.addView(keyGridContainer)
    }

    private fun addKeyRow(keys: List<String>, hasSpecial: Boolean = false, isBottom: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(3) }
            gravity = Gravity.CENTER
        }

        keys.forEach { key ->
            val btn = Button(this)
            btn.isAllCaps = false
            btn.setAllCaps(false)
            btn.setTextColor(if (key == "↵") Color.WHITE else colorKeyText)

            when (key) {
                "⇧" -> {
                    btn.text = if (isCaps) "⇪" else "⇧"
                    btn.background = keyBg(colorSpecialBg)
                    btn.textSize = 15f
                    btn.setTypeface(null, Typeface.BOLD)
                    btn.setOnClickListener { toggleCaps() }
                }
                "⌫" -> {
                    btn.text = "⌫"
                    btn.background = keyBg(colorSpecialBg)
                    btn.textSize = 15f
                    btn.setTypeface(null, Typeface.BOLD)
                    btn.setOnClickListener { handleDelete() }
                }
                "SPACE" -> {
                    btn.text = if (currentLang == "en") "English" else "বাংলা"
                    btn.background = keyBg(colorKeyBg)
                    btn.textSize = 14f
                    btn.setTextColor(Color.parseColor("#5f6368"))
                    btn.setOnClickListener { sendChar(' ') }
                }
                "↵" -> {
                    btn.text = "↵"
                    btn.background = keyBg(colorAccent)
                    btn.setTextColor(Color.WHITE)
                    btn.setOnClickListener { handleEnter() }
                }
                "123", "?123" -> {
                    btn.text = "123"
                    btn.background = keyBg(colorSpecialBg)
                    btn.textSize = 14f
                    btn.setOnClickListener { show("Numbers coming soon") }
                }
                else -> {
                    btn.text = if (isCaps && currentLang == "en") key.uppercase() else key
                    btn.background = keyBg(colorKeyBg)
                    btn.textSize = 20f
                    btn.setOnClickListener { 
                        sendText(btn.text.toString())
                        if (isCaps && currentLang == "en") {
                            isCaps = false
                            rebuildKeys()
                        }
                    }
                }
            }

            // Set text size based on key type
            when {
                key == "SPACE" -> btn.textSize = 14f
                key.length > 2 -> btn.textSize = 14f
                key in listOf("⇧", "⇪", "⌫", "123", "?123", "↵") -> btn.textSize = 15f
                else -> btn.textSize = 20f
            }

            val weight = when {
                isBottom && key == "SPACE" -> 5f
                isBottom && (key == "123" || key == "?123") -> 1.3f
                isBottom && key == "↵" -> 1.6f
                isBottom && key in listOf(",", ".") -> 1f
                hasSpecial && key in listOf("⇧", "⇪", "⌫") -> 1.3f
                else -> 1f
            }

            btn.layoutParams = LinearLayout.LayoutParams(0, dp(44)).apply {
                this.weight = weight
                marginStart = dp(3)
                marginEnd = dp(3)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                btn.stateListAnimator = null
            }

            btn.isClickable = true
            btn.isFocusable = true

            row.addView(btn)
        }

        keyGridContainer.addView(row)
    }

    private fun makeToolbarBtn(iconRes: Int, action: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(40))
            setImageResource(iconRes)
            setColorFilter(colorIcon)
            background = null
            scaleType = android.widget.ImageView.ScaleType.CENTER
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
    }

    private fun makeSuggTv(gravity: Int): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            this.gravity = gravity or Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            textSize = 16f
            setTextColor(colorKeyText)
            typeface = Typeface.DEFAULT_BOLD
            isClickable = true
            isFocusable = true
            setOnClickListener { 
                val word = text.toString()
                if (word.isNotEmpty()) {
                    sendText("$word ")
                }
            }
        }
    }

    private fun makeDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
            setBackgroundColor(colorDivider)
        }
    }

    private fun keyBg(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(6).toFloat()
        setColor(color)
    }

    private fun roundedDrawable(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
    }

    // --- INPUT LOGIC ---

    private fun sendText(text: String) {
        vibrate()
        currentInputConnection?.commitText(text, 1)
        updateSuggestions(text)
    }

    private fun sendChar(c: Char) {
        vibrate()
        currentInputConnection?.commitText(c.toString(), 1)
    }

    private fun handleDelete() {
        vibrate()
        currentInputConnection?.let { ic ->
            val sel = ic.getSelectedText(0)
            if (sel.isNullOrEmpty()) {
                ic.deleteSurroundingText(1, 0)
            } else {
                ic.commitText("", 1)
            }
        }
        suggestionBar.visibility = View.GONE
    }

    private fun handleEnter() {
        vibrate()
        currentInputConnection?.let { ic ->
            val action = currentInputEditorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            when (action) {
                EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_SEND, EditorInfo.IME_ACTION_DONE -> ic.performEditorAction(action)
                else -> ic.commitText("\n", 1)
            }
        }
    }

    private fun updateSuggestions(typed: String) {
        suggestionBar.visibility = View.VISIBLE
        val last = typed.trim().split(" ").lastOrNull() ?: ""
        if (last.isNotEmpty() && currentLang == "en") {
            tvSugg1.text = "${last}ing"
            tvSugg2.text = "${last}ed"
            tvSugg3.text = "${last}ly"
        } else {
            tvSugg1.text = "the"
            tvSugg2.text = "to"
            tvSugg3.text = "and"
        }
    }

    private fun toggleCaps() {
        isCaps = !isCaps
        vibrate()
        rebuildKeys()
    }

    private fun swapLanguage() {
        currentLang = if (currentLang == "en") "bn" else "en"
        vibrate()
        show(if (currentLang == "en") "English" else "বাংলা")
        rebuildKeys()
    }

    private fun rebuildKeys() {
        keyGridContainer.removeAllViews()

        if (currentLang == "en") {
            addKeyRow(listOf("q","w","e","r","t","y","u","i","o","p"))
            addKeyRow(listOf("a","s","d","f","g","h","j","k","l"))
            addKeyRow(listOf("⇧","z","x","c","v","b","n","m","⌫"), hasSpecial = true)
            addKeyRow(listOf("123",",","SPACE",".","↵"), isBottom = true)
        } else {
            banglaRows.forEachIndexed { i, row ->
                val special = (i == 2)
                val bottom = (i == banglaRows.lastIndex)
                addKeyRow(row, hasSpecial = special, isBottom = bottom)
            }
        }
    }

    // --- VOICE LOGIC ---

    private fun toggleVoiceBar() {
        if (isVoiceActive) {
            stopVoice()
            voiceBar.visibility = View.GONE
            toolbar.visibility = View.VISIBLE
        } else {
            voiceBar.visibility = View.VISIBLE
            toolbar.visibility = View.GONE
            suggestionBar.visibility = View.GONE
            startVoice()
        }
    }

    private fun swapVoiceEngine() {
        vibrate()
        voiceEngine = if (voiceEngine == "android") "groq" else "android"
        val langName = if (currentLang == "en") "English" else "বাংলা"
        tvVoiceLang.text = "$langName (${voiceEngine.replaceFirstChar { it.uppercase() }})"
        if (isVoiceActive) { stopVoice(); startVoice() }
    }

    private fun startVoice() {
        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            show("Please grant microphone permission in Settings")
            tvVoiceStatus.text = "No permission"
            return
        }

        isVoiceActive = true
        animateVisualizer(true)

        when (voiceEngine) {
            "android" -> startAndroidVoice()
            "groq" -> startGroqVoice()
        }
    }

    private fun stopVoice() {
        isVoiceActive = false
        animateVisualizer(false)

        when (voiceEngine) {
            "android" -> {
                try {
                    speechRecognizer?.stopListening()
                } catch (e: Exception) {
                    android.util.Log.e("PixelProKeyboard", "stopListening error", e)
                }
            }
            "groq" -> stopGroqVoice()
        }
    }

    private fun startAndroidVoice() {
        tvVoiceStatus.text = "Listening..."

        try {
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p0: Bundle?) {
                    tvVoiceStatus.text = "Speak now..."
                }

                override fun onBeginningOfSpeech() {
                    tvVoiceStatus.text = "Listening..."
                }

                override fun onRmsChanged(rms: Float) {
                    // Animate visualizer based on volume
                    val normalizedRms = (rms / 10).coerceIn(0f, 1f)
                    visualizerBars.forEachIndexed { i, bar ->
                        val height = dp(8 + (normalizedRms * 12 * (i % 2 + 1)).toInt())
                        bar.layoutParams.height = height
                        bar.alpha = 0.5f + normalizedRms * 0.5f
                        bar.requestLayout()
                    }
                }

                override fun onBufferReceived(p0: ByteArray?) {}

                override fun onEndOfSpeech() {
                    if (isVoiceActive) {
                        tvVoiceStatus.text = "Processing..."
                        startAndroidVoice()
                    }
                }

                override fun onError(error: Int) {
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Error: $error"
                    }
                    tvVoiceStatus.text = errorMsg
                    
                    if (isVoiceActive && error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        mainHandler.postDelayed({ if (isVoiceActive) startAndroidVoice() }, 1000)
                    }
                }

                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { 
                        sendText(it)
                    }
                    if (isVoiceActive) startAndroidVoice()
                }

                override fun onPartialResults(partial: Bundle?) {
                    partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                        tvVoiceStatus.text = it
                    }
                }

                override fun onEvent(p0: Int, p1: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLang == "bn") "bn-BD" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)

        } catch (e: Exception) {
            android.util.Log.e("PixelProKeyboard", "startAndroidVoice error", e)
            tvVoiceStatus.text = "Voice error: ${e.message}"
            show("Voice recognition failed")
        }
    }

    private fun startGroqVoice() {
        tvVoiceStatus.text = "Recording..."

        try {
            audioFile = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }

            // Simulate visualizer animation for Groq
            groqVisualizerJob = scope.launch {
                var amplitude = 0
                while (isVoiceActive) {
                    amplitude = (amplitude + 1) % 10
                    withContext(Dispatchers.Main) {
                        visualizerBars.forEachIndexed { i, bar ->
                            val height = dp(8 + ((Math.sin((amplitude + i).toDouble()) + 1) * 6).toInt())
                            bar.layoutParams.height = height
                            bar.alpha = 0.5f + (Math.sin((amplitude + i).toDouble()).toFloat() * 0.25f) + 0.25f
                            bar.requestLayout()
                        }
                    }
                    delay(100)
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("PixelProKeyboard", "startGroqVoice error", e)
            tvVoiceStatus.text = "Recording failed"
            show("Failed to start recording: ${e.message}")
        }
    }

    private var groqVisualizerJob: Job? = null

    private fun stopGroqVoice() {
        groqVisualizerJob?.cancel()
        groqVisualizerJob = null

        try {
            mediaRecorder?.apply { 
                stop()
                release() 
            }
            mediaRecorder = null

            audioFile?.let { file ->
                if (file.exists() && file.length() > 1000) {
                    tvVoiceStatus.text = "Transcribing..."
                    sendToGroq(file)
                } else {
                    tvVoiceStatus.text = "Recording too short"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PixelProKeyboard", "stopGroqVoice error", e)
            tvVoiceStatus.text = "Error processing audio"
        }
        audioFile = null
    }

    private fun sendToGroq(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-large-v3")
                    .addFormDataPart("file", "audio.m4a", file.asRequestBody("audio/mp4".toMediaType()))
                    .addFormDataPart("language", if (currentLang == "bn") "bn" else "en")
                    .addFormDataPart("response_format", "json")
                    .build()

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val text = responseBody?.substringAfter("\"text\":\"")?.substringBefore("\"")
                            ?.replace("\\n", "\n")
                            ?.replace("\\\"", "\"")
                            ?.replace("\\'", "'")
                            ?: ""
                        
                        if (text.isNotEmpty()) {
                            sendText(text)
                            tvVoiceStatus.text = "Done!"
                        } else {
                            tvVoiceStatus.text = "No speech detected"
                        }
                    } else {
                        val errorCode = response.code
                        tvVoiceStatus.text = "API Error: $errorCode"
                        when (errorCode) {
                            401 -> show("Invalid API key")
                            429 -> show("Rate limit exceeded")
                            500, 502, 503 -> show("Server error, try again")
                            else -> show("API Error: $errorCode")
                        }
                    }
                    response.body?.close()
                }

            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    tvVoiceStatus.text = "Network error"
                    show("Network error: ${e.message}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvVoiceStatus.text = "Error"
                    show("Error: ${e.message}")
                }
            } finally {
                // Clean up audio file
                try {
                    file.delete()
                } catch (e: Exception) {}
            }
        }
    }

    private fun animateVisualizer(active: Boolean) {
        visualizerBars.forEach { bar ->
            bar.alpha = if (active) 0.5f else 0.3f
            bar.layoutParams.height = dp(if (active) 12 else 8)
            bar.requestLayout()
        }
    }

    // --- UTILS ---

    private fun show(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        android.util.Log.d("PixelProKeyboard", msg)
    }

    private fun vibrate() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    private fun dp(value: Int): Int {
        return if (::displayMetrics.isInitialized) {
            (value * displayMetrics.density).toInt()
        } else {
            value
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            speechRecognizer?.destroy()
            scope.cancel()
            mediaRecorder?.release()
            groqVisualizerJob?.cancel()
        } catch (e: Exception) {
            android.util.Log.e("PixelProKeyboard", "onDestroy error", e)
        }
    }
}
