package com.aikeyboard.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.ViewGroup
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

    // --- ROOT VIEW ---
    private lateinit var rootView: View

    // --- TOP AREA VIEWS ---
    private lateinit var toolbar: LinearLayout
    private lateinit var suggestionBar: LinearLayout
    private lateinit var voiceBar: LinearLayout

    // --- TOOLBAR BUTTONS ---
    private lateinit var btnEmoji: ImageButton
    private lateinit var btnClipboard: ImageButton
    private lateinit var btnCredentials: ImageButton
    private lateinit var btnLanguage: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnVoice: ImageButton

    // --- SUGGESTION VIEWS ---
    private lateinit var tvSugg1: TextView
    private lateinit var tvSugg2: TextView
    private lateinit var tvSugg3: TextView

    // --- VOICE BAR VIEWS ---
    private lateinit var tvVoiceLang: TextView
    private lateinit var visualizerContainer: LinearLayout
    private lateinit var btnStopVoice: ImageButton

    // --- KEY GRID CONTAINER ---
    private lateinit var keyGridContainer: LinearLayout

    // --- VOICE ---
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isVoiceActive = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val client = OkHttpClient()

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
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    override fun onCreateInputView(): View {
        // Inflate the exact XML layout
        rootView = layoutInflater.inflate(R.layout.keyboard_view, null)

        // Initialize all views
        initViews()

        // Set up click listeners
        setupClickListeners()

        // Set up key listeners
        setupKeyListeners()

        return rootView
    }

    private fun initViews() {
        // Top area containers
        toolbar = rootView.findViewById(R.id.toolbar)
        suggestionBar = rootView.findViewById(R.id.suggestionBar)
        voiceBar = rootView.findViewById(R.id.voiceBar)

        // Toolbar buttons
        btnEmoji = rootView.findViewById(R.id.btnEmoji)
        btnClipboard = rootView.findViewById(R.id.btnClipboard)
        btnCredentials = rootView.findViewById(R.id.btnCredentials)
        btnLanguage = rootView.findViewById(R.id.btnLanguage)
        btnSettings = rootView.findViewById(R.id.btnSettings)
        btnVoice = rootView.findViewById(R.id.btnVoice)

        // Suggestion views
        tvSugg1 = rootView.findViewById(R.id.tvSugg1)
        tvSugg2 = rootView.findViewById(R.id.tvSugg2)
        tvSugg3 = rootView.findViewById(R.id.tvSugg3)

        // Voice bar views
        tvVoiceLang = rootView.findViewById(R.id.tvVoiceLang)
        visualizerContainer = rootView.findViewById(R.id.visualizerContainer)
        btnStopVoice = rootView.findViewById(R.id.btnStopVoice)

        // Key grid container
        keyGridContainer = rootView.findViewById(R.id.keyGridContainer)
    }

    private fun setupClickListeners() {
        // Toolbar buttons
        btnEmoji.setOnClickListener { show("Emoji coming soon") }
        btnClipboard.setOnClickListener { show("Clipboard coming soon") }
        btnCredentials.setOnClickListener { show("Credentials coming soon") }
        btnLanguage.setOnClickListener { swapLanguage() }
        btnSettings.setOnClickListener { show("Settings coming soon") }
        btnVoice.setOnClickListener { toggleVoiceBar() }

        // Suggestion clicks
        tvSugg1.setOnClickListener { usePrediction(tvSugg1.text.toString()) }
        tvSugg2.setOnClickListener { usePrediction(tvSugg2.text.toString()) }
        tvSugg3.setOnClickListener { usePrediction(tvSugg3.text.toString()) }

        // Voice bar
        tvVoiceLang.setOnClickListener { swapVoiceEngine() }
        btnStopVoice.setOnClickListener { toggleVoiceBar() }
    }

    private fun setupKeyListeners() {
        // Loop through all rows in keyGridContainer
        for (i in 0 until keyGridContainer.childCount) {
            val row = keyGridContainer.getChildAt(i) as? LinearLayout ?: continue

            for (j in 0 until row.childCount) {
                val btn = row.getChildAt(j) as? Button ?: continue
                val keyText = btn.text.toString()

                btn.setOnClickListener {
                    when (keyText) {
                        "⇧", "⇪" -> toggleCaps()
                        "⌫" -> handleDelete()
                        "↵" -> handleEnter()
                        "123", "?123" -> show("Numbers coming soon")
                        "English", "বাংলা" -> sendChar(' ')
                        else -> {
                            sendText(keyText)
                            if (isCaps && currentLang == "en") {
                                isCaps = false
                                rebuildKeys()
                            }
                        }
                    }
                }
            }
        }
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
            if (sel.isNullOrEmpty()) ic.deleteSurroundingText(1, 0)
            else ic.commitText("", 1)
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

    private fun usePrediction(word: String) {
        sendText("$word ")
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
        // Clear current key grid
        keyGridContainer.removeAllViews()

        if (currentLang == "en") {
            // Build English QWERTY layout
            addKeyRow(listOf("q","w","e","r","t","y","u","i","o","p"))
            addKeyRow(listOf("a","s","d","f","g","h","j","k","l"))
            addKeyRow(listOf("⇧","z","x","c","v","b","n","m","⌫"), hasSpecial = true)
            addKeyRow(listOf("123",",","SPACE",".","↵"), isBottom = true)
        } else {
            // Build Bangla layout
            banglaRows.forEachIndexed { i, row ->
                val special = (i == 2)
                val bottom = (i == banglaRows.lastIndex)
                addKeyRow(row, hasSpecial = special, isBottom = bottom)
            }
        }

        // Re-attach listeners
        setupKeyListeners()
    }

    private fun addKeyRow(keys: List<String>, hasSpecial: Boolean = false, isBottom: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
            gravity = android.view.Gravity.CENTER
        }

        keys.forEach { key ->
            val btn = Button(context)

            when (key) {
                "⇧" -> {
                    btn.text = if (isCaps) "⇪" else "⇧"
                    btn.setBackgroundResource(R.drawable.bg_key_special)
                    btn.textSize = 15f
                    btn.setTypeface(null, android.graphics.Typeface.BOLD)
                }
                "⌫" -> {
                    btn.text = "⌫"
                    btn.setBackgroundResource(R.drawable.bg_key_special)
                    btn.textSize = 15f
                    btn.setTypeface(null, android.graphics.Typeface.BOLD)
                }
                "SPACE" -> {
                    btn.text = if (currentLang == "en") "English" else "বাংলা"
                    btn.setBackgroundResource(R.drawable.bg_key_default)
                    btn.textSize = 15f
                    btn.setTextColor(Color.parseColor("#5f6368"))
                }
                "↵" -> {
                    btn.text = "↵"
                    btn.setBackgroundResource(R.drawable.bg_key_accent)
                    btn.setTextColor(Color.WHITE)
                }
                "123", "?123" -> {
                    btn.text = "123"
                    btn.setBackgroundResource(R.drawable.bg_key_special)
                    btn.textSize = 15f
                }
                else -> {
                    btn.text = if (isCaps && currentLang == "en") key.uppercase() else key
                    btn.setBackgroundResource(R.drawable.bg_key_default)
                    btn.textSize = 20f
                }
            }

            btn.setTextColor(if (key == "↵") Color.WHITE else colorKeyText)
            btn.textSize = when {
                key == "SPACE" -> 15f
                key.length > 2 -> 15f
                key in listOf("⇧", "⇪", "⌫", "123", "?123", "↵") -> 15f
                else -> 20f
            }
            btn.textAllCaps = false

            val weight = when {
                isBottom && key == "SPACE" -> 5f
                isBottom && (key == "123" || key == "?123") -> 1.3f
                isBottom && key == "↵" -> 1.6f
                isBottom && key in listOf(",", ".") -> 1f
                hasSpecial && key in listOf("⇧", "⇪", "⌫") -> 1.3f
                else -> 1f
            }

            btn.layoutParams = LinearLayout.LayoutParams(
                0, dp(42)
            ).apply {
                this.weight = weight
                marginStart = dp(3)
                marginEnd = dp(3)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                btn.stateListAnimator = null
            }

            row.addView(btn)
        }

        keyGridContainer.addView(row)
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
        voiceEngine = if (voiceEngine == "android") "groq" else "android"
        val langName = if (currentLang == "en") "English" else "বাংলা"
        tvVoiceLang.text = "$langName (${voiceEngine.replaceFirstChar { it.uppercase() }})"
        if (isVoiceActive) { stopVoice(); startVoice() }
    }

    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            show("Grant microphone permission")
            return
        }

        isVoiceActive = true

        when (voiceEngine) {
            "android" -> startAndroidVoice()
            "groq" -> startGroqVoice()
        }
    }

    private fun stopVoice() {
        isVoiceActive = false
        when (voiceEngine) {
            "android" -> speechRecognizer?.stopListening()
            "groq" -> stopGroqVoice()
        }
    }

    private fun startAndroidVoice() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() { if (isVoiceActive) startAndroidVoice() }
            override fun onError(p0: Int) { if (isVoiceActive && p0 != 7) startAndroidVoice() }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { sendText(it) }
                if (isVoiceActive) startAndroidVoice()
            }
            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        })

        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLang == "bn") "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        })
    }

    private fun startGroqVoice() {
        try {
            audioFile = File(cacheDir, "voice.m4a")
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
        } catch (e: Exception) { show("Recording failed") }
    }

    private fun stopGroqVoice() {
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
            audioFile?.let { if (it.exists() && it.length() > 0) sendToGroq(it) }
        } catch (e: Exception) {}
        audioFile = null
    }

    private fun sendToGroq(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-large-v3")
                    .addFormDataPart("file", "audio.m4a", file.asRequestBody("audio/mp4".toMediaType()))
                    .addFormDataPart("language", if (currentLang == "bn") "bn" else "en")
                    .build()

                val req = Request.Builder()
                    .url("https://api.groq.com/openai/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                    .post(body)
                    .build()

                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val txt = resp.body?.string()?.substringAfter("\"text\":\"")?.substringBefore("\"")
                        ?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: ""
                    withContext(Dispatchers.Main) { sendText(txt) }
                }
            } catch (e: Exception) {}
        }
    }

    // --- UTILS ---

    private fun show(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun vibrate() {
        val vib = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
        } else vib.vibrate(15)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        scope.cancel()
        mediaRecorder?.release()
    }
}
