package com.aikeyboard.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import com.aikeyboard.BuildConfig
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AiKeyboardService : android.inputmethodservice.InputMethodService() {

    // --- STATE ---
    private var isCaps = false
    private var currentLang = "en" // "en" or "bn"
    private var voiceEngine = "android" // "android" or "groq"
    
    // --- THEME COLORS (Light Mode) ---
    private var colorBg: Int = Color.parseColor("#E8EAED")
    private var colorKeyBg: Int = Color.parseColor("#FFFFFF")
    private var colorKeyText: Int = Color.parseColor("#202124")
    private var colorSpecialBg: Int = Color.parseColor("#DADCE0")
    private var colorAccent: Int = Color.parseColor("#1A73E8")
    private var colorDivider: Int = Color.parseColor("#1F000000")

    // --- VIEWS ---
    private lateinit var container: LinearLayout
    private lateinit var topAreaContainer: LinearLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var suggestionBar: LinearLayout
    private lateinit var voiceBar: LinearLayout
    private lateinit var keyGridContainer: LinearLayout
    
    private lateinit var tvSugg1: TextView
    private lateinit var tvSugg2: TextView
    private lateinit var tvSugg3: TextView
    
    private lateinit var langChip: TextView
    private lateinit var visualizerBars: List<View>
    private lateinit var voiceStatus: TextView

    // --- VOICE RECOGNITION ---
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isVoiceActive = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val client = OkHttpClient()
    
    // Bangla keyboard layout (Probhat)
    private val banglaRows = listOf(
        listOf("ঔ", "ঐ", "আ", "ঈ", "ঊ", "ঋ", "এ", "অ", "ই", "উ"),
        listOf("ও", "্য", "ড়", "ঢ়", "ৎ", "ং", "ঃ", "ঁ", "ক", "খ"),
        listOf("⇧", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ", "ঞ", "⌫"),
        listOf("?123", "ট", "ঠ", "ড", "ঢ", "ণ", "ত", "থ", "দ", "ধ"),
        listOf("ন", "প", "ফ", "ব", "ভ", "ম", "য", "র", "ল", "শ"),
        listOf("?123", "ষ", "স", "হ", "SPACE", "।", "↵")
    )

    override fun onCreate() {
        super.onCreate()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    override fun onCreateInputView(): View {
        container = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
            setPadding(0, 4, 0, 16)
        }
        
        setupTopArea()
        setupKeyboardGrid()

        return container
    }

    // --- UI BUILDERS ---

    private fun setupTopArea() {
        topAreaContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. TOOLBAR
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            )
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        // Left side buttons
        toolbar.addView(createToolbarButton(android.R.drawable.ic_menu_emoticons) { showText("Emoji coming soon!") })
        toolbar.addView(createToolbarButton(android.R.drawable.ic_menu_edit) { showText("Clipboard coming soon!") })
        toolbar.addView(createToolbarButton(android.R.drawable.ic_lock_lock) { showText("Credentials coming soon!") })

        // Spacer
        toolbar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })

        // Right side buttons
        toolbar.addView(createToolbarButton(android.R.drawable.ic_menu_sort_by_size) { swapLanguage() })
        toolbar.addView(createToolbarButton(android.R.drawable.ic_menu_manage) { showText("Settings coming soon!") })
        toolbar.addView(createToolbarButton(android.R.drawable.ic_btn_speak_now) { toggleVoiceBar() })

        topAreaContainer.addView(toolbar)

        // 2. SUGGESTION BAR
        suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            weightSum = 3f
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
        }

        tvSugg1 = createSuggestionTextView(Gravity.START)
        tvSugg2 = createSuggestionTextView(Gravity.CENTER)
        tvSugg3 = createSuggestionTextView(Gravity.END)

        suggestionBar.addView(tvSugg1)
        suggestionBar.addView(createVerticalDivider())
        suggestionBar.addView(tvSugg2)
        suggestionBar.addView(createVerticalDivider())
        suggestionBar.addView(tvSugg3)

        topAreaContainer.addView(suggestionBar)

        // 3. VOICE BAR
        voiceBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(56)
            )
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
        }

        // Engine selector
        val engineContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(36))
            background = createRoundedDrawable(colorBg, 18f)
        }

        langChip = TextView(this).apply {
            text = "Android"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
            background = createRoundedDrawable(colorAccent, 18f)
            setOnClickListener { 
                voiceEngine = "android"
                updateEngineUI()
                if (isVoiceActive) { stopVoice(); startVoice() }
            }
        }
        
        val groqChip = TextView(this).apply {
            text = "Groq"
            setTextColor(colorKeyText)
            textSize = 13f
            setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
            setOnClickListener { 
                voiceEngine = "groq"
                updateEngineUI()
                if (isVoiceActive) { stopVoice(); startVoice() }
            }
        }
        
        engineContainer.addView(langChip)
        engineContainer.addView(groqChip)

        // Status/Visualizer
        voiceStatus = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f)
            gravity = Gravity.CENTER
            text = "🎤 Tap mic to start"
            textSize = 14f
            setTextColor(colorAccent)
        }

        // Visualizer bars (hidden initially)
        val visualizer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        
        visualizerBars = (1..5).map {
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(4), dpToPx(16)).apply {
                    marginStart = dpToPx(2)
                    marginEnd = dpToPx(2)
                }
                setBackgroundColor(colorAccent)
            }.also { visualizer.addView(it) }
        }

        val stopBtn = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            background = createRoundedDrawable(Color.parseColor("#EA4335"), 20f)
            setOnClickListener { toggleVoiceBar() }
        }

        voiceBar.addView(engineContainer)
        voiceBar.addView(voiceStatus)
        voiceBar.addView(visualizer)
        voiceBar.addView(stopBtn)

        topAreaContainer.addView(voiceBar)
        container.addView(topAreaContainer)
    }

    private fun updateEngineUI() {
        if (voiceEngine == "android") {
            langChip.text = "Android"
            langChip.setTextColor(Color.WHITE)
            langChip.background = createRoundedDrawable(colorAccent, 18f)
        } else {
            langChip.text = "Groq"
            langChip.setTextColor(Color.WHITE)
            langChip.background = createRoundedDrawable(colorAccent, 18f)
        }
    }

    private fun setupKeyboardGrid() {
        keyGridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
        }

        if (currentLang == "en") {
            // English QWERTY
            val row1 = listOf("q","w","e","r","t","y","u","i","o","p")
            val row2 = listOf("a","s","d","f","g","h","j","k","l")
            val row3 = listOf("⇧", "z","x","c","v","b","n","m", "⌫")
            val row4 = listOf("?123", ",", "SPACE", ".", "↵")

            keyGridContainer.addView(createKeyRow(row1))
            keyGridContainer.addView(createKeyRow(row2))
            keyGridContainer.addView(createKeyRow(row3, hasSpecial = true))
            keyGridContainer.addView(createKeyRow(row4, isBottom = true))
        } else {
            // Bangla Probhat
            banglaRows.forEachIndexed { index, row ->
                if (index == banglaRows.lastIndex) {
                    keyGridContainer.addView(createKeyRow(row, isBottom = true))
                } else if (index == 2) {
                    keyGridContainer.addView(createKeyRow(row, hasSpecial = true))
                } else {
                    keyGridContainer.addView(createKeyRow(row))
                }
            }
        }

        container.addView(keyGridContainer)
    }

    private fun createKeyRow(keys: List<String>, hasSpecial: Boolean = false, isBottom: Boolean = false): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(3) }
            gravity = Gravity.CENTER
            
            keys.forEach { key ->
                val btn = Button(context)
                
                when (key) {
                    "⇧" -> {
                        btn.text = if (isCaps) "⇪" else "⇧"
                        btn.setOnClickListener { toggleCaps() }
                        btn.background = createKeyBackground(colorSpecialBg)
                    }
                    "⌫" -> {
                        btn.text = "⌫"
                        btn.setOnClickListener { handleDelete(); vibrate() }
                        btn.background = createKeyBackground(colorSpecialBg)
                    }
                    "SPACE" -> {
                        btn.text = if (currentLang == "en") "English" else "বাংলা"
                        btn.setOnClickListener { sendKeyChar(' ') }
                        btn.background = createKeyBackground(colorKeyBg)
                    }
                    "↵" -> {
                        btn.text = "↵"
                        btn.background = createKeyBackground(colorAccent, isEnter = true)
                        btn.setTextColor(Color.WHITE)
                        btn.setOnClickListener { handleEnter() }
                    }
                    "?123" -> {
                        btn.text = "?123"
                        btn.background = createKeyBackground(colorSpecialBg)
                        btn.setOnClickListener { showText("Numbers coming soon!") }
                    }
                    else -> {
                        btn.text = if (isCaps && currentLang == "en" && key.isNotEmpty() && key[0].isLetter()) key.uppercase() else key
                        btn.setOnClickListener { 
                            sendText(btn.text.toString())
                            if (isCaps && currentLang == "en") toggleCaps()
                        }
                        btn.background = createKeyBackground(colorKeyBg)
                    }
                }

                btn.setTextColor(if (key == "↵") Color.WHITE else colorKeyText)
                btn.textSize = if (key.length > 2 || key == "SPACE") 12f else 18f
                btn.typeface = if (currentLang == "bn") Typeface.DEFAULT else Typeface.DEFAULT
                
                val weightVal = when {
                    isBottom && key == "SPACE" -> 3f
                    isBottom && (key == "?123" || key == "↵") -> 1.5f
                    hasSpecial && (key == "⇧" || key == "⌫") -> 1.4f
                    else -> 1f
                }

                val widthPx = if (isBottom || hasSpecial) 0 else LinearLayout.LayoutParams.WRAP_CONTENT
                
                btn.layoutParams = LinearLayout.LayoutParams(widthPx, dpToPx(44)).apply {
                    if (weightVal > 0 && (isBottom || hasSpecial)) this.weight = weightVal
                    marginStart = dpToPx(2)
                    marginEnd = dpToPx(2)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    btn.stateListAnimator = null
                }

                addView(btn)
            }
        }
    }

    private fun createToolbarButton(iconRes: Int, action: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                marginStart = dpToPx(2)
                marginEnd = dpToPx(2)
            }
            setImageResource(iconRes)
            setColorFilter(colorKeyText)
            background = null
            setOnClickListener { action() }
        }
    }

    private fun createSuggestionTextView(gravity: Int): TextView {
        return TextView(this).apply {
            this.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            this.gravity = gravity or Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            textSize = 15f
            setTextColor(colorKeyText)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { usePrediction(text.toString()) }
        }
    }

    private fun createVerticalDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                topMargin = dpToPx(12)
                bottomMargin = dpToPx(12)
            }
            setBackgroundColor(colorDivider)
        }
    }

    private fun createKeyBackground(color: Int, isEnter: Boolean = false): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(6).toFloat()
            setColor(color)
        }
    }

    private fun createRoundedDrawable(color: Int, radiusDp: Float): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(radiusDp).toFloat()
            setColor(color)
        }
    }

    // --- INPUT LOGIC ---

    private fun sendText(text: String) {
        vibrate()
        currentInputConnection?.commitText(text, 1)
        updateSuggestions(text)
    }

    private fun sendKeyChar(c: Char) {
        vibrate()
        currentInputConnection?.commitText(c.toString(), 1)
    }

    private fun handleDelete() {
        currentInputConnection?.let { ic ->
            val selectedText = ic.getSelectedText(0)
            if (selectedText.isNullOrEmpty()) {
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
                EditorInfo.IME_ACTION_SEND, EditorInfo.IME_ACTION_DONE -> {
                    ic.performEditorAction(action)
                }
                else -> ic.commitText("\n", 1)
            }
        }
    }

    private fun usePrediction(word: String) {
        sendText(word + " ")
    }

    private fun updateSuggestions(typed: String) {
        suggestionBar.visibility = View.VISIBLE
        val last = typed.trim().split(" ").lastOrNull() ?: ""
        if (last.isNotEmpty() && currentLang == "en") {
            tvSugg1.text = last + "ing"
            tvSugg2.text = last + "ed"
            tvSugg3.text = last + "ly"
        } else {
            tvSugg1.text = "the"
            tvSugg2.text = "to"
            tvSugg3.text = "and"
        }
    }

    private fun toggleCaps() {
        isCaps = !isCaps
        vibrate()
        container.removeView(keyGridContainer)
        setupKeyboardGrid()
    }

    private fun swapLanguage() {
        currentLang = if (currentLang == "en") "bn" else "en"
        vibrate()
        showText(if (currentLang == "en") "English Keyboard" else "বাংলা কীবোর্ড")
        container.removeView(keyGridContainer)
        setupKeyboardGrid()
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

    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            showText("Please grant microphone permission")
            return
        }

        isVoiceActive = true
        voiceStatus.text = "🎤 Listening..."
        voiceStatus.visibility = View.VISIBLE
        (visualizerBars.firstOrNull()?.parent as? View)?.visibility = View.GONE

        when (voiceEngine) {
            "android" -> startAndroidRecognition()
            "groq" -> startGroqRecording()
        }
    }

    private fun stopVoice() {
        isVoiceActive = false
        when (voiceEngine) {
            "android" -> stopAndroidRecognition()
            "groq" -> stopGroqRecording()
        }
    }

    private fun startAndroidRecognition() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                voiceStatus.text = "🎤 Listening..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (isVoiceActive) startAndroidRecognition()
            }
            override fun onError(error: Int) {
                if (isVoiceActive && error != SpeechRecognizer.ERROR_NO_MATCH) {
                    startAndroidRecognition()
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { 
                    sendText(it)
                }
                if (isVoiceActive) startAndroidRecognition()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLang == "bn") "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopAndroidRecognition() {
        speechRecognizer?.stopListening()
    }

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
            voiceStatus.text = "🎤 Recording... Tap stop"
        } catch (e: Exception) {
            showText("Recording failed")
        }
    }

    private fun stopGroqRecording() {
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
            audioFile?.let { file ->
                if (file.exists() && file.length() > 0) sendToGroq(file)
            }
        } catch (e: Exception) {}
        audioFile = null
    }

    private fun sendToGroq(audioFile: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-large-v3")
                    .addFormDataPart("file", "audio.m4a", audioFile.asRequestBody("audio/mp4".toMediaType()))
                    .addFormDataPart("language", if (currentLang == "bn") "bn" else "en")
                    .build()

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val text = body.substringAfter("\"text\":\"").substringBefore("\"")
                        .replace("\\n", "\n").replace("\\\"", "\"")
                    withContext(Dispatchers.Main) { sendText(text) }
                }
            } catch (e: Exception) {}
        }
    }

    // --- UTILS ---

    private fun showText(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(15)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        scope.cancel()
        mediaRecorder?.release()
    }
}
