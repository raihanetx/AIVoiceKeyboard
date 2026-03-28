package com.aikeyboard.presentation.keyboard

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.core.app.ActivityCompat
import com.aikeyboard.core.util.AudioRecorder
import com.aikeyboard.data.local.PreferencesManager
import com.aikeyboard.data.remote.api.GroqWhisperApi
import com.aikeyboard.domain.model.TranscriptionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AiKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "PixelProKeyboard"
    }

    // === DICTIONARIES ===
    private val DICT_ARR = listOf("the", "quick", "brown", "fox", "jumps", "over", "lazy", "dog", "hello", "world", "apple", "application", "android", "pixel", "premium", "good", "morning", "night", "how", "are", "you", "what", "where", "when", "why", "who", "awesome", "amazing", "keyboard", "i", "am", "have", "will", "best", "way", "only", "is", "a", "to", "it", "its", "on", "in", "at", "this", "that", "we", "us", "they", "them", "he", "she", "his", "her", "and", "or", "but", "so", "because", "yes", "no", "my", "mine", "car", "cat", "can", "boy", "girl", "time", "person", "year", "day", "thing", "man", "woman", "life", "child", "work", "new", "first", "last", "long", "great", "little", "own", "other", "old", "right", "big", "high", "different", "small", "large", "next", "early", "young", "important", "few", "public", "bad", "same", "able", "do", "say", "go", "get", "make", "know", "think", "take", "see", "come", "want", "look", "use", "find", "give", "tell", "work", "may", "should", "call", "try", "ask", "need", "feel", "become", "leave", "put", "mean", "keep", "let", "begin", "seem", "help", "talk", "turn", "start", "show", "hear", "play", "run", "move", "like", "live", "believe", "hold", "bring", "happen", "write", "provide", "sit", "stand", "lose", "pay", "meet", "include", "continue", "set", "learn", "change", "lead", "understand", "watch", "follow", "stop", "create", "speak", "read", "allow", "add", "spend", "grow", "open", "walk", "win", "offer", "remember", "love", "consider", "appear", "buy", "wait", "serve", "die", "send", "expect", "build", "stay", "fall", "cut", "reach", "kill", "remain")
    
    private val NEXT_WORD_MAP = mapOf(
        "i" to listOf("am", "have", "will"), "the" to listOf("best", "way", "only"), "hello" to listOf("world", "there", "friend"),
        "how" to listOf("are", "is", "to"), "good" to listOf("morning", "night", "luck"), "you" to listOf("are", "can", "will"),
        "we" to listOf("are", "will", "have"), "they" to listOf("are", "will", "have"), "what" to listOf("is", "are", "do")
    )

    private val L = mapOf(
        "en" to listOf(
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("⇧","z","x","c","v","b","n","m","⌫")
        ),
        "bn" to listOf(
            listOf("ৌ","ৈ","া","ী","ূ","ব","হ","গ","দ","জ"),
            listOf("ো","ে","্","ি","ু","প","র","ক","ত","চ"),
            listOf("⇧","ং","ম","ন","স","ল","শ","⌫")
        ),
        "bn_shift" to listOf(
            listOf("ঔ","ঐ","আ","ঈ","ঊ","ভ","ঙ","ঘ","ধ","ঝ"),
            listOf("ও","এ","অ","ই","উ","ফ","ড়","খ","থ","ছ"),
            listOf("⇧","ঃ","ণ","ঞ","ষ","য়","ঢ","⌫")
        ),
        "num" to listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            listOf("@","#","£","%","&","-","+","(",")"),
            listOf("=/*","\"","'",":",";","!","?","⌫")
        )
    )

    // === STATE ===
    private var bufferText = ""
    private var currentLang = "en"
    private var currentMode = "alpha"
    private var isCaps = false
    private var isVoiceMode = false
    private var selectedSttEngine = "android" // "android" or "groq"

    // === COLORS ===
    private val colorBg = Color.parseColor("#E8EAED")
    private val colorKeyBg = Color.parseColor("#FFFFFF")
    private val colorKeyText = Color.parseColor("#202124")
    private val colorSpecialBg = Color.parseColor("#DADCE0")
    private val colorAccent = Color.parseColor("#1A73E8")
    private val colorDivider = Color.parseColor("#1A000000")
    private val colorVoiceRed = Color.parseColor("#EA4335")
    private val colorGroq = Color.parseColor("#2196F3")

    // === VIEWS ===
    private lateinit var rootContainer: LinearLayout
    private lateinit var topArea: LinearLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var suggestionBar: LinearLayout
    private lateinit var voiceBar: LinearLayout
    private lateinit var keyGrid: LinearLayout
    private lateinit var engineSelector: LinearLayout
    
    private lateinit var sWord1: TextView
    private lateinit var sWord2: TextView
    private lateinit var sWord3: TextView
    private lateinit var voiceResultText: TextView
    private lateinit var visualizer: LinearLayout

    // Voice Animator
    private val animators = mutableListOf<ValueAnimator>()

    // Voice Recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecorder: AudioRecorder? = null
    private var isRecording = false

    // Preferences & API
    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(this) }
    private val groqWhisperApi: GroqWhisperApi by lazy { GroqWhisperApi.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== PixelPro Keyboard Created ===")
        audioRecorder = AudioRecorder(this)
    }

    override fun onCreateInputView(): View {
        rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
            setPadding(0, 0, 0, dpToPx(16))
        }

        setupTopArea()
        setupKeyGrid()
        
        return rootContainer
    }

    // === SETUP UI ===

    private fun setupTopArea() {
        topArea = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL 
        }

        // 1. Toolbar
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44))
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        // Left Buttons
        toolbar.addView(createToolbarButton(android.R.drawable.ic_menu_emotics) { /* Emoji */ })
        toolbar.addView(createToolbarButton(android.R.drawable.ic_menu_edit) { /* Clipboard */ })
        toolbar.addView(createToolbarButton(android.R.drawable.ic_lock_lock) { /* Credentials */ })
        
        // Spacer
        toolbar.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        
        // Right Buttons
        toolbar.addView(createToolbarButton(android.R.drawable.ic_menu_mapmode) { swapLang() })
        toolbar.addView(createToolbarButton(android.R.drawable.ic_menu_manage) { /* Settings */ })
        toolbar.addView(createToolbarButton(android.R.drawable.ic_btn_speak_now) { toggleVoiceMode() })

        topArea.addView(toolbar)

        // 2. Suggestion Bar
        suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48))
            weightSum = 3f
            visibility = View.GONE
        }

        sWord1 = createSuggestionTextView(Gravity.START)
        sWord2 = createSuggestionTextView(Gravity.CENTER)
        sWord3 = createSuggestionTextView(Gravity.END)

        suggestionBar.addView(sWord1)
        suggestionBar.addView(createVerticalDivider())
        suggestionBar.addView(sWord2)
        suggestionBar.addView(createVerticalDivider())
        suggestionBar.addView(sWord3)

        topArea.addView(suggestionBar)

        // 3. Voice Bar
        voiceBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            visibility = View.GONE
        }

        // Engine Selector Row
        engineSelector = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(8))
        }
        
        val androidBtn = createEngineButton("Android (Offline)", "android", colorAccent)
        val groqBtn = createEngineButton("Groq (Online)", "groq", colorGroq)
        
        engineSelector.addView(androidBtn)
        engineSelector.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(8), 1) })
        engineSelector.addView(groqBtn)
        
        voiceBar.addView(engineSelector)

        // Voice Row: Lang Chip | Visualizer | Stop Button
        val voiceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48))
            gravity = Gravity.CENTER_VERTICAL
        }

        val langChip = TextView(this).apply {
            text = if(currentLang == "en") "English" else "বাংলা"
            setTextColor(colorKeyText)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            background = createRoundedDrawable(colorSpecialBg, 20f)
        }
        
        visualizer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f)
            gravity = Gravity.CENTER
            tag = "visualizer"
        }
        
        repeat(5) { visualizer.addView(createVoiceBar()) }

        val stopBtn = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            background = createRoundedDrawable(colorVoiceRed, 18f)
            setOnClickListener { stopVoiceRecording() }
        }

        voiceRow.addView(langChip)
        voiceRow.addView(visualizer)
        voiceRow.addView(stopBtn)

        voiceBar.addView(voiceRow)

        // Voice Result Text
        voiceResultText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            textSize = 16f
            setTextColor(colorKeyText)
            gravity = Gravity.CENTER
            visibility = View.GONE
            background = createRoundedDrawable(Color.WHITE, 8f)
        }
        voiceBar.addView(voiceResultText)

        topArea.addView(voiceBar)
        rootContainer.addView(topArea)
    }

    private fun createEngineButton(text: String, engine: String, color: Int): TextView {
        val isSelected = selectedSttEngine == engine
        return TextView(this).apply {
            this.text = text
            setTextColor(if (isSelected) Color.WHITE else colorKeyText)
            textSize = 13f
            typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            background = createRoundedDrawable(if (isSelected) color else colorSpecialBg, 20f)
            setOnClickListener { 
                selectedSttEngine = engine
                rebuildVoiceUI()
            }
        }
    }

    private fun rebuildVoiceUI() {
        if (isVoiceMode) {
            voiceBar.removeAllViews()
            
            // Re-add engine selector
            engineSelector = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dpToPx(8))
            }
            
            val androidBtn = createEngineButton("Android (Offline)", "android", colorAccent)
            val groqBtn = createEngineButton("Groq (Online)", "groq", colorGroq)
            
            engineSelector.addView(androidBtn)
            engineSelector.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dpToPx(8), 1) })
            engineSelector.addView(groqBtn)
            
            voiceBar.addView(engineSelector)

            // Re-add voice row
            val voiceRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48))
                gravity = Gravity.CENTER_VERTICAL
            }

            val langChip = TextView(this@AiKeyboardService).apply {
                text = if(currentLang == "en") "English" else "বাংলা"
                setTextColor(colorKeyText)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                background = createRoundedDrawable(colorSpecialBg, 20f)
            }
            
            visualizer = LinearLayout(this@AiKeyboardService).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f)
                gravity = Gravity.CENTER
                tag = "visualizer"
            }
            
            repeat(5) { visualizer.addView(createVoiceBar()) }

            val stopBtn = ImageButton(this@AiKeyboardService).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
                scaleType = ImageView.ScaleType.CENTER
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.WHITE)
                background = createRoundedDrawable(colorVoiceRed, 18f)
                setOnClickListener { stopVoiceRecording() }
            }

            voiceRow.addView(langChip)
            voiceRow.addView(visualizer)
            voiceRow.addView(stopBtn)

            voiceBar.addView(voiceRow)

            // Re-add result text
            voiceResultText.visibility = View.GONE
            voiceBar.addView(voiceResultText)
        }
    }

    private fun setupKeyGrid() {
        keyGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), 0)
        }
        renderKeyboard()
        rootContainer.addView(keyGrid)
    }

    // === KEYBOARD RENDERER ===

    private fun renderKeyboard() {
        keyGrid.removeAllViews()
        
        val layoutMap = if (currentMode == "num") L["num"]!!
                        else if (currentLang == "bn") if (isCaps) L["bn_shift"]!! else L["bn"]!!
                        else L["en"]!!

        layoutMap.forEachIndexed { index, rowKeys ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
                if (index == 1) setPadding(dpToPx(10), 0, dpToPx(10), 0)
            }

            rowKeys.forEach { key ->
                val btn = Button(this)
                
                when (key) {
                    "⌫" -> {
                        btn.text = ""
                        btn.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_input_delete, 0, 0, 0)
                        btn.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) handleDelete()
                            false
                        }
                        setupKeyStyle(btn, isSpecial = true, flex = 1.3f)
                    }
                    "⇧" -> {
                        btn.text = "⇧"
                        btn.setOnClickListener { isCaps = !isCaps; renderKeyboard() }
                        setupKeyStyle(btn, isSpecial = true, flex = 1.3f)
                        if (isCaps) btn.setTextColor(colorAccent)
                    }
                    else -> {
                        val char = if (isCaps && currentLang == "en" && currentMode == "alpha") key.uppercase() else key
                        btn.text = char
                        btn.setOnClickListener { handleKey(char) }
                        setupKeyStyle(btn)
                    }
                }
                row.addView(btn)
            }
            keyGrid.addView(row)
        }

        // Bottom Row
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
        }

        val modeBtn = Button(this).apply {
            text = if (currentMode == "num") "ABC" else "?123"
            setOnClickListener { currentMode = if (currentMode == "num") "alpha" else "num"; renderKeyboard() }
        }
        setupKeyStyle(modeBtn, isSpecial = true, flex = 1.5f)

        val commaBtn = Button(this).apply { text = ","; setOnClickListener { handleKey(",") } }
        setupKeyStyle(commaBtn, isSpecial = true)

        val spaceBtn = Button(this).apply { 
            text = if(currentLang == "en") "English" else "বাংলা"
            setOnClickListener { handleKey(" ") } 
        }
        setupKeyStyle(spaceBtn, isSpace = true, flex = 5f)

        val dotBtn = Button(this).apply { text = "."; setOnClickListener { handleKey(".") } }
        setupKeyStyle(dotBtn, isSpecial = true)

        val enterBtn = Button(this).apply { 
            text = "↵"
            setOnClickListener { handleKey("\n") } 
        }
        setupKeyStyle(enterBtn, isEnter = true, flex = 1.5f)

        bottomRow.addView(modeBtn)
        bottomRow.addView(commaBtn)
        bottomRow.addView(spaceBtn)
        bottomRow.addView(dotBtn)
        bottomRow.addView(enterBtn)
        keyGrid.addView(bottomRow)
    }

    private fun setupKeyStyle(btn: Button, isSpecial: Boolean = false, isSpace: Boolean = false, isEnter: Boolean = false, flex: Float = 1.0f) {
        val params = LinearLayout.LayoutParams(0, dpToPx(42)).apply {
            weight = flex
            marginStart = dpToPx(3)
            marginEnd = dpToPx(3)
            topMargin = dpToPx(4)
        }
        btn.layoutParams = params
        
        btn.setTextColor(when {
            isEnter -> Color.WHITE
            else -> colorKeyText
        })

        btn.textSize = if (isSpace || isSpecial) 14f else 20f
        btn.typeface = Typeface.DEFAULT
        btn.setAllCaps(false)
        btn.background = createRoundedDrawable(when {
            isEnter -> colorAccent
            isSpecial || isSpace -> colorSpecialBg
            else -> colorKeyBg
        }, 6f)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) btn.stateListAnimator = null
    }

    // === INPUT LOGIC ===

    private fun handleKey(char: String) {
        vibrate()
        bufferText += char
        currentInputConnection?.commitText(char, 1)
        
        if (isCaps && currentLang == "en") { isCaps = false; renderKeyboard() }
        
        updateSuggestions()
    }

    private fun handleDelete() {
        vibrate()
        if (bufferText.isNotEmpty()) bufferText = bufferText.dropLast(1)
        currentInputConnection?.deleteSurroundingText(1, 0)
        updateSuggestions()
    }

    private fun updateSuggestions() {
        if (bufferText.trim().isEmpty()) {
            suggestionBar.visibility = View.GONE
            return
        } else {
            suggestionBar.visibility = View.VISIBLE
        }

        val isTrailingSpace = bufferText.endsWith(" ") || bufferText.endsWith("\n")
        val words = bufferText.trimEnd().split(Regex("[\\s\\n]+"))
        val currentWord = if (isTrailingSpace) "" else words.last()
        val prevWord = if (words.size > 1) words[words.size - 2].lowercase() else if (words.size == 1 && currentWord.isEmpty()) words[0].lowercase() else ""

        val suggestions = mutableListOf<String>()
        val generic = listOf("i", "the", "to")

        if (currentWord.isNotEmpty()) {
            suggestions.addAll(DICT_ARR.filter { it.startsWith(currentWord.lowercase()) && it != currentWord.lowercase() })
        } else {
            suggestions.addAll(NEXT_WORD_MAP[prevWord] ?: emptyList())
        }

        if (suggestions.isEmpty()) suggestions.addAll(generic)

        sWord1.text = suggestions.getOrNull(0) ?: ""
        sWord2.text = suggestions.getOrNull(1) ?: ""
        sWord3.text = suggestions.getOrNull(2) ?: ""

        sWord1.setOnClickListener { usePrediction(sWord1.text.toString()) }
        sWord2.setOnClickListener { usePrediction(sWord2.text.toString()) }
        sWord3.setOnClickListener { usePrediction(sWord3.text.toString()) }
    }

    private fun usePrediction(word: String) {
        if (word.isEmpty()) return
        val isCompletingWord = !bufferText.endsWith(" ") && !bufferText.endsWith("\n") && bufferText.isNotEmpty()
        
        if (isCompletingWord) {
            val match = Regex("[a-zA-Z]+\$").find(bufferText)
            if (match != null) {
                val len = match.value.length
                bufferText = bufferText.dropLast(len)
                currentInputConnection?.deleteSurroundingText(len, 0)
            }
        }
        handleKey("$word ")
    }

    private fun swapLang() {
        currentLang = if (currentLang == "en") "bn" else "en"
        currentMode = "alpha"
        renderKeyboard()
    }

    // === VOICE MODE ===

    private fun toggleVoiceMode() {
        isVoiceMode = !isVoiceMode
        if (isVoiceMode) {
            toolbar.visibility = View.GONE
            suggestionBar.visibility = View.GONE
            voiceBar.visibility = View.VISIBLE
            keyGrid.visibility = View.GONE
            startVoiceRecording()
        } else {
            stopVoiceRecording()
        }
    }

    private fun startVoiceRecording() {
        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showVoiceError("Microphone permission required")
            return
        }

        isRecording = true
        startVoiceAnimation()

        when (selectedSttEngine) {
            "android" -> startAndroidSpeechRecognizer()
            "groq" -> startGroqRecording()
        }
    }

    private fun startAndroidSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showVoiceError("Speech recognition not available")
            isRecording = false
            stopVoiceAnimation()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    showVoiceStatus("Listening...")
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    // Don't stop - keep listening
                }

                override fun onError(error: Int) {
                    showVoiceError(getSpeechErrorText(error))
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        showVoiceResult(matches[0])
                    }
                    // Restart listening for continuous mode
                    if (isRecording) {
                        startAndroidSpeechRecognizer()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        showVoiceStatus(matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLang == "en") "en-US" else "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.startListening(intent)
    }

    private fun startGroqRecording() {
        if (!preferencesManager.isGroqApiKeyConfigured()) {
            showVoiceError("Groq API key not set")
            isRecording = false
            stopVoiceAnimation()
            return
        }

        val audioFile = audioRecorder?.startRecording()
        if (audioFile == null) {
            showVoiceError("Recording failed")
            isRecording = false
            stopVoiceAnimation()
            return
        }

        showVoiceStatus("Recording... Tap stop when done")
    }

    private fun stopVoiceRecording() {
        isRecording = false
        stopVoiceAnimation()

        when (selectedSttEngine) {
            "android" -> {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
            "groq" -> {
                val audioFile = audioRecorder?.stopRecording()
                if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                    showVoiceStatus("Transcribing...")
                    transcribeWithGroq(audioFile)
                }
            }
        }

        // Return to keyboard
        voiceBar.visibility = View.GONE
        toolbar.visibility = View.VISIBLE
        keyGrid.visibility = View.VISIBLE
        isVoiceMode = false
        updateSuggestions()
    }

    private fun transcribeWithGroq(audioFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = groqWhisperApi.transcribe(audioFile, currentLang)
            
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { text ->
                        showVoiceResult(text)
                        // Insert text
                        bufferText += text + " "
                        currentInputConnection?.commitText(text + " ", 1)
                    },
                    onFailure = { error ->
                        showVoiceError(error.message ?: "Transcription failed")
                    }
                )
            }
            audioFile.delete()
        }
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

    private fun showVoiceStatus(text: String) {
        voiceResultText.text = text
        voiceResultText.visibility = View.VISIBLE
        voiceResultText.setTextColor(colorKeyText)
    }

    private fun showVoiceResult(text: String) {
        voiceResultText.text = text
        voiceResultText.visibility = View.VISIBLE
        voiceResultText.setTextColor(colorAccent)
        // Insert to input
        bufferText += text + " "
        currentInputConnection?.commitText(text + " ", 1)
    }

    private fun showVoiceError(error: String) {
        voiceResultText.text = "❌ $error"
        voiceResultText.visibility = View.VISIBLE
        voiceResultText.setTextColor(colorVoiceRed)
    }

    // === UTILS ===

    private fun createToolbarButton(iconRes: Int, action: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(40))
            setImageResource(iconRes)
            setColorFilter(colorKeyText)
            background = null
            setOnClickListener { action() }
        }
    }

    private fun createSuggestionTextView(gravity: Int): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            this.gravity = gravity or Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            textSize = 16f
            setTextColor(colorKeyText)
            typeface = Typeface.DEFAULT_BOLD
            background = null
        }
    }

    private fun createVerticalDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(1), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                topMargin = dpToPx(12)
                bottomMargin = dpToPx(12)
            }
            setBackgroundColor(colorDivider)
        }
    }

    private fun createVoiceBar(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(3), dpToPx(12)).apply {
                marginStart = dpToPx(2)
                marginEnd = dpToPx(2)
            }
            setBackgroundColor(colorAccent)
        }
    }

    private fun createRoundedDrawable(color: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(radiusDp).toFloat()
            setColor(color)
        }
    }

    private fun startVoiceAnimation() {
        for (i in 0 until visualizer.childCount) {
            val bar = visualizer.getChildAt(i)
            val anim = ValueAnimator.ofFloat(6f, 28f).apply {
                duration = (300..600).random().toLong()
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { value ->
                    val h = value.animatedValue as Float
                    bar.layoutParams.height = dpToPx(h.toInt())
                    bar.requestLayout()
                }
            }
            animators.add(anim)
            anim.start()
        }
    }

    private fun stopVoiceAnimation() {
        animators.forEach { it.cancel() }
        animators.clear()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(10)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "=== Service Destroyed ===")
        speechRecognizer?.destroy()
        audioRecorder?.release()
        stopVoiceAnimation()
        super.onDestroy()
    }
}
