package com.aikeyboard.service

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * PixelProKeyboard - Production-Ready AI Voice Keyboard
 * 
 * @version 1.1.0
 */
class PixelProKeyboard : android.inputmethodservice.InputMethodService() {

    companion object {
        private const val TAG = "PixelProKeyboard"
        private const val MIN_RECORDING_DURATION_MS = 500L
        private const val VOICE_RESTART_DELAY_MS = 1000L
        private const val PREFS_NAME = "PixelProKeyboardPrefs"
        private const val CREDENTIALS_KEY = "saved_credentials"
        private const val CLIPBOARD_HISTORY_KEY = "clipboard_history"
    }

    // --- STATE ---
    private var isCaps = false
    private var currentLang = "en"
    private var voiceEngine = "android"
    private val isVoiceActive = AtomicBoolean(false)
    private var keyboardMode = "text"
    
    // For credential adding
    private var currentCredentialName = ""
    private var currentCredentialValue = ""
    private var isAddingCredential = false
    private var credentialInputTarget = "name" // "name" or "value"

    // --- THEME COLORS ---
    private val colorBg = Color.parseColor("#E8EAED")
    private val colorKeyBg = Color.parseColor("#FFFFFF")
    private val colorKeyText = Color.parseColor("#202124")
    private val colorSpecialBg = Color.parseColor("#DADCE0")
    private val colorAccent = Color.parseColor("#1A73E8")
    private val colorIcon = Color.parseColor("#5f6368")
    private val colorDivider = Color.parseColor("#1F000000")
    private val colorError = Color.parseColor("#EA4335")
    private val colorSuccess = Color.parseColor("#34A853")
    private val colorCardBg = Color.parseColor("#F8F9FA")

    // --- VIEWS ---
    private var container: LinearLayout? = null
    private var toolbar: LinearLayout? = null
    private var suggestionBar: LinearLayout? = null
    private var voiceBar: LinearLayout? = null
    private var keyGridContainer: LinearLayout? = null
    private var extraPanel: LinearLayout? = null
    private var tvSugg1: TextView? = null
    private var tvSugg2: TextView? = null
    private var tvSugg3: TextView? = null
    private var tvVoiceLang: TextView? = null
    private var tvVoiceStatus: TextView? = null
    private var visualizerBars: MutableList<View>? = null
    
    // Panel header text
    private var panelHeaderText: TextView? = null
    private var panelContentArea: LinearLayout? = null

    // --- VOICE ---
    private var speechRecognizer: SpeechRecognizer? = null
    private var isSpeechRecognizerAvailable = false
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var groqVisualizerJob: Job? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var prefs: SharedPreferences
    private lateinit var clipboardManager: ClipboardManager

    private var recordingStartTime = 0L

    // Emoji categories
    private val emojiCategories = mapOf(
        "😊" to listOf("😊","😂","🥰","😍","🤩","😎","🥳","😇","🙃","😋","🤗","😏","😌","😴","🤔","😤","😢","😭","😱","😡"),
        "👍" to listOf("👍","👎","👏","🙌","🤝","✌️","🤞","👌","🤟","👋","🤚","✋","🖖","👌","🤌","🤏","✊","👊","🤛","🤜"),
        "❤️" to listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","💟","♥️"),
        "🎉" to listOf("🎉","🎊","🎈","🎁","🎀","🎄","🎅","🤶","🧑‍🎄","🎆","🎇","✨","🎐","🎑","🧧","🎂","🎃","🪔","🎋","🎁"),
        "🐶" to listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🐔","🐧","🐦","🐤","🦆"),
        "🍎" to listOf("🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍑","🍒","🥝","🍅","🥥","🥑","🍆","🥔","🥕","🌽","🌶️"),
        "🏠" to listOf("🏠","🏡","🏘️","🏗️","🏢","🏬","🏣","🏤","🏥","🏦","🏨","🏪","🏫","🏩","💒","🏛️","⛪","🕌","🕍","🛕"),
        "🚗" to listOf("🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚐","🚚","🚛","🚜","🛴","🚲","🛵","🏍️","✈️","🚀","🛸"),
        "⚡" to listOf("⚡","🔥","💧","🌊","💨","🌪️","🌈","☀️","🌙","⭐","🌟","✨","💫","🌠","☁️","⛈️","🌤️","🌧️","❄️","☃️"),
        "💻" to listOf("💻","📱","📲","☎️","📞","📟","📠","📺","📻","🎙️","🎚️","🎛️","🧭","⏱️","⏲️","⏰","🕰️","💾","💿","📀")
    )
    
    private var currentEmojiCategory = "😊"

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
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            initializeSpeechRecognizer()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "onCreate error", e)
        }
    }

    private fun initializeSpeechRecognizer() {
        try {
            isSpeechRecognizerAvailable = SpeechRecognizer.isRecognitionAvailable(this)
            if (isSpeechRecognizerAvailable) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            }
        } catch (e: Exception) {
            isSpeechRecognizerAvailable = false
            speechRecognizer = null
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
            container!!
        } catch (e: Exception) {
            TextView(this).apply { text = "Keyboard error: ${e.message}" }
        }
    }

    private fun setupTopArea() {
        val topArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // TOOLBAR
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
            setPadding(dp(4), 0, dp(4), 0)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(colorBg)
        }

        toolbar?.addView(makeToolbarBtn(R.drawable.ic_smile) { toggleEmojiKeyboard() })
        toolbar?.addView(makeToolbarBtn(R.drawable.ic_clipboard) { toggleClipboardPanel() })
        toolbar?.addView(makeToolbarBtn(R.drawable.ic_key) { toggleCredentialsPanel() })
        toolbar?.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        toolbar?.addView(makeToolbarBtn(R.drawable.ic_globe) { swapLanguage() })
        toolbar?.addView(makeToolbarBtn(R.drawable.ic_settings) { showSettings() })
        toolbar?.addView(makeToolbarBtn(R.drawable.ic_mic) { toggleVoiceBar() })

        topArea.addView(toolbar)

        // SUGGESTION BAR
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

        suggestionBar?.addView(tvSugg1)
        suggestionBar?.addView(makeDivider())
        suggestionBar?.addView(tvSugg2)
        suggestionBar?.addView(makeDivider())
        suggestionBar?.addView(tvSugg3)

        topArea.addView(suggestionBar)

        // VOICE BAR
        voiceBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
        }

        tvVoiceLang = TextView(this).apply {
            text = "English (Android)"
            setTextColor(colorKeyText)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedDrawable(colorSpecialBg, 16)
            isClickable = true
            setOnClickListener { swapVoiceEngine() }
        }
        voiceBar?.addView(tvVoiceLang)

        val centerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
        }

        tvVoiceStatus = TextView(this).apply {
            text = "Tap mic to start"
            textSize = 12f
            setTextColor(colorIcon)
            gravity = Gravity.CENTER
        }
        centerContainer.addView(tvVoiceStatus)

        val visualizer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(20))
            gravity = Gravity.CENTER
        }

        visualizerBars?.clear()
        repeat(5) {
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), dp(8)).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
                setBackgroundColor(colorAccent)
                alpha = 0.3f
            }
            visualizerBars?.add(bar)
            visualizer.addView(bar)
        }
        centerContainer.addView(visualizer)
        voiceBar?.addView(centerContainer)

        val stopBtn = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            setImageResource(R.drawable.ic_stop)
            setColorFilter(Color.WHITE)
            background = roundedDrawable(colorError, 20)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            setOnClickListener { toggleVoiceBar() }
        }
        voiceBar?.addView(stopBtn)

        topArea.addView(voiceBar)
        container?.addView(topArea)
    }

    // --- EMOJI KEYBOARD ---
    
    private fun toggleEmojiKeyboard() {
        vibrate()
        if (keyboardMode == "emoji") {
            showTextKeyboard()
        } else {
            showEmojiKeyboard()
        }
    }
    
    private fun showEmojiKeyboard() {
        keyboardMode = "emoji"
        hideAllPanels()
        keyGridContainer?.visibility = View.GONE
        
        extraPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(200))
            setBackgroundColor(Color.WHITE)
        }
        
        // Category row
        val categoryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
            setBackgroundColor(colorBg)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        
        emojiCategories.keys.forEach { category ->
            val catBtn = TextView(this@PixelProKeyboard).apply {
                text = category
                textSize = 18f
                setPadding(dp(10), dp(6), dp(10), dp(6))
                gravity = Gravity.CENTER
                background = if (category == currentEmojiCategory) 
                    roundedDrawable(colorAccent, 16) 
                else 
                    roundedDrawable(Color.TRANSPARENT, 16)
                setTextColor(if (category == currentEmojiCategory) Color.WHITE else colorKeyText)
                setOnClickListener { 
                    vibrate()
                    currentEmojiCategory = category
                    updateEmojiGrid()
                }
            }
            categoryRow.addView(catBtn)
        }
        extraPanel?.addView(categoryRow)
        
        // Emoji grid
        emojiGrid = GridView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            numColumns = 5
            verticalSpacing = dp(4)
            horizontalSpacing = dp(4)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            adapter = EmojiAdapter(emojiCategories[currentEmojiCategory] ?: emptyList())
            setOnItemClickListener { _, _, position, _ ->
                val emoji = emojiCategories[currentEmojiCategory]?.getOrNull(position)
                if (emoji != null) sendText(emoji)
            }
        }
        extraPanel?.addView(emojiGrid)
        
        container?.addView(extraPanel)
    }
    
    private fun updateEmojiGrid() {
        val catRow = extraPanel?.getChildAt(0) as? LinearLayout ?: return
        for (i in 0 until catRow.childCount) {
            val btn = catRow.getChildAt(i) as? TextView ?: continue
            val emoji = btn.text.toString()
            btn.background = if (emoji == currentEmojiCategory) 
                roundedDrawable(colorAccent, 16) 
            else 
                roundedDrawable(Color.TRANSPARENT, 16)
            btn.setTextColor(if (emoji == currentEmojiCategory) Color.WHITE else colorKeyText)
        }
        (emojiGrid?.adapter as? EmojiAdapter)?.updateEmojis(emojiCategories[currentEmojiCategory] ?: emptyList())
    }
    
    private var emojiGrid: GridView? = null
    
    inner class EmojiAdapter(private var emojis: List<String>) : android.widget.BaseAdapter() {
        override fun getCount(): Int = emojis.size
        override fun getItem(position: Int): Any = emojis[position]
        override fun getItemId(position: Int): Long = position.toLong()
        
        override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup?): View {
            return (convertView as? TextView ?: TextView(this@PixelProKeyboard).apply {
                textSize = 26f
                gravity = Gravity.CENTER
            }).apply { text = emojis[position] }
        }
        
        fun updateEmojis(newEmojis: List<String>) {
            emojis = newEmojis
            notifyDataSetChanged()
        }
    }

    // --- CLIPBOARD MANAGER ---
    
    private var clipboardListContainer: LinearLayout? = null
    
    private fun toggleClipboardPanel() {
        vibrate()
        if (keyboardMode == "clipboard") {
            showTextKeyboard()
        } else {
            showClipboardPanel()
        }
    }
    
    private fun showClipboardPanel() {
        keyboardMode = "clipboard"
        hideAllPanels()
        keyGridContainer?.visibility = View.VISIBLE  // Keep keyboard visible for typing!
        
        extraPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180))
            setBackgroundColor(colorBg)
        }
        
        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
            setBackgroundColor(Color.WHITE)
        }
        
        header.addView(TextView(this).apply {
            text = "📋 Clipboard"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        
        // Save to Credentials button
        header.addView(Button(this).apply {
            text = "Save to Credentials"
            textSize = 11f
            setTextColor(colorAccent)
            background = roundedDrawable(colorCardBg, 12)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                vibrate()
                saveCurrentClipboardToCredentials()
            }
        })
        
        extraPanel?.addView(header)
        
        // Content area - cards layout (no scrollbar)
        clipboardListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        
        updateClipboardList()
        extraPanel?.addView(clipboardListContainer)
        
        container?.addView(extraPanel, container?.childCount?.minus(1) ?: 1)
    }
    
    private fun updateClipboardList() {
        clipboardListContainer?.removeAllViews()
        
        val history = getClipboardHistory()
        val currentClip = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        
        val allItems = mutableListOf<String>()
        if (!currentClip.isNullOrBlank() && !history.contains(currentClip)) {
            allItems.add(0, currentClip)
        }
        allItems.addAll(history)
        
        if (allItems.isEmpty()) {
            clipboardListContainer?.addView(TextView(this).apply {
                text = "No clipboard items\nCopy something to see it here"
                textSize = 13f
                setTextColor(colorIcon)
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(24), dp(16), dp(24))
            })
            return
        }
        
        // Show last 5 items as cards (no scroll)
        allItems.take(5).forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(4)
                }
                setBackgroundColor(Color.WHITE)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundedDrawable(Color.WHITE, 8)
                isClickable = true
                setOnClickListener {
                    vibrate()
                    sendText(item)
                }
                setOnLongClickListener {
                    vibrate()
                    removeFromClipboardHistory(item)
                    updateClipboardList()
                    true
                }
            }
            
            card.addView(TextView(this).apply {
                text = if (item.length > 40) item.take(40) + "..." else item
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            
            card.addView(TextView(this).apply {
                text = "tap to paste"
                textSize = 10f
                setTextColor(colorIcon)
            })
            
            clipboardListContainer?.addView(card)
        }
    }
    
    private fun saveCurrentClipboardToCredentials() {
        val currentClip = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        if (currentClip.isNullOrBlank()) {
            show("Clipboard is empty")
            return
        }
        
        // Store the clipboard value and switch to credential panel to get name
        currentCredentialValue = currentClip
        currentCredentialName = ""
        credentialInputTarget = "name"
        isAddingCredential = true
        
        // Show credential panel with name input
        showCredentialInputPanel()
    }
    
    private fun getClipboardHistory(): List<String> {
        val json = prefs.getString(CLIPBOARD_HISTORY_KEY, "[]") ?: "[]"
        return try {
            org.json.JSONArray(json).let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveToClipboardHistory(text: String) {
        if (text.length <= 2) return
        val history = getClipboardHistory().toMutableList()
        history.remove(text)
        history.add(0, text)
        if (history.size > 20) history.removeLast()
        
        val json = org.json.JSONArray(history).toString()
        prefs.edit().putString(CLIPBOARD_HISTORY_KEY, json).apply()
    }
    
    private fun removeFromClipboardHistory(text: String) {
        val history = getClipboardHistory().toMutableList()
        history.remove(text)
        val json = org.json.JSONArray(history).toString()
        prefs.edit().putString(CLIPBOARD_HISTORY_KEY, json).apply()
    }

    // --- CREDENTIAL SAFE ---
    
    private var credentialListContainer: LinearLayout? = null
    
    private fun toggleCredentialsPanel() {
        vibrate()
        if (keyboardMode == "credentials") {
            showTextKeyboard()
        } else {
            showCredentialsPanel()
        }
    }
    
    private fun showCredentialsPanel() {
        keyboardMode = "credentials"
        hideAllPanels()
        keyGridContainer?.visibility = View.VISIBLE  // Keep keyboard visible!
        
        extraPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180))
            setBackgroundColor(colorBg)
        }
        
        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
            setBackgroundColor(Color.WHITE)
        }
        
        header.addView(TextView(this).apply {
            text = "🔐 Credential Safe"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        
        header.addView(Button(this).apply {
            text = "+ Add New"
            textSize = 12f
            setTextColor(colorAccent)
            background = roundedDrawable(colorCardBg, 12)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener {
                vibrate()
                startAddingCredential()
            }
        })
        
        extraPanel?.addView(header)
        
        // Content area
        credentialListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        
        updateCredentialList()
        extraPanel?.addView(credentialListContainer)
        
        container?.addView(extraPanel, container?.childCount?.minus(1) ?: 1)
    }
    
    private fun startAddingCredential() {
        currentCredentialName = ""
        currentCredentialValue = ""
        credentialInputTarget = "name"
        isAddingCredential = true
        showCredentialInputPanel()
    }
    
    private fun showCredentialInputPanel() {
        credentialListContainer?.removeAllViews()
        
        // Input card
        val inputCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedDrawable(Color.WHITE, 8)
        }
        
        // Name field
        inputCard.addView(TextView(this).apply {
            text = "Account Name:"
            textSize = 12f
            setTextColor(colorIcon)
        })
        
        inputCard.addView(TextView(this).apply {
            text = if (currentCredentialName.isEmpty()) "Type name..." else currentCredentialName
            textSize = 16f
            setTextColor(if (currentCredentialName.isEmpty()) colorIcon else colorKeyText)
            setPadding(0, dp(8), 0, dp(12))
            background = roundedDrawable(colorCardBg, 4)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            isClickable = true
            setOnClickListener {
                vibrate()
                credentialInputTarget = "name"
                show("Type the account name, then tap 'Next'")
            }
        })
        
        // Value field
        inputCard.addView(TextView(this).apply {
            text = "Password/Value:"
            textSize = 12f
            setTextColor(colorIcon)
        })
        
        inputCard.addView(TextView(this).apply {
            text = if (currentCredentialValue.isEmpty()) "Type value..." else "••••••••"
            textSize = 16f
            setTextColor(if (currentCredentialValue.isEmpty()) colorIcon else colorKeyText)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = roundedDrawable(colorCardBg, 4)
            isClickable = true
            setOnClickListener {
                vibrate()
                credentialInputTarget = "value"
                show("Type the password, then tap 'Save'")
            }
        })
        
        // Buttons
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(12), 0, 0)
        }
        
        buttonRow.addView(Button(this).apply {
            text = "Cancel"
            textSize = 12f
            setTextColor(colorIcon)
            background = null
            setOnClickListener {
                vibrate()
                isAddingCredential = false
                showCredentialsPanel()
            }
        })
        
        buttonRow.addView(Button(this).apply {
            text = if (credentialInputTarget == "name") "Next →" else "Save"
            textSize = 12f
            setTextColor(colorAccent)
            background = null
            setOnClickListener {
                vibrate()
                if (credentialInputTarget == "name") {
                    if (currentCredentialName.isNotEmpty()) {
                        credentialInputTarget = "value"
                        showCredentialInputPanel()
                    } else {
                        show("Please type an account name first")
                    }
                } else {
                    if (currentCredentialName.isNotEmpty() && currentCredentialValue.isNotEmpty()) {
                        saveCredential(currentCredentialName, currentCredentialValue)
                        show("Saved: ${currentCredentialName}")
                        isAddingCredential = false
                        showCredentialsPanel()
                    } else {
                        show("Please fill both fields")
                    }
                }
            }
        })
        
        inputCard.addView(buttonRow)
        credentialListContainer?.addView(inputCard)
        
        // Instruction
        credentialListContainer?.addView(TextView(this).apply {
            text = "💡 Type using the keyboard below, then tap the buttons above"
            textSize = 11f
            setTextColor(colorIcon)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), 0)
        })
    }
    
    private fun updateCredentialList() {
        credentialListContainer?.removeAllViews()
        
        if (isAddingCredential) {
            showCredentialInputPanel()
            return
        }
        
        val credentials = getCredentials()
        
        if (credentials.isEmpty()) {
            credentialListContainer?.addView(TextView(this).apply {
                text = "No saved credentials\nTap '+ Add New' to save your first credential"
                textSize = 13f
                setTextColor(colorIcon)
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(24), dp(16), dp(24))
            })
            return
        }
        
        credentials.take(5).forEach { (name, _) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(4)
                }
                setBackgroundColor(Color.WHITE)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundedDrawable(Color.WHITE, 8)
                isClickable = true
                setOnClickListener {
                    vibrate()
                    val cred = getCredentials().find { it.first == name }
                    if (cred != null) sendText(cred.second)
                }
                setOnLongClickListener {
                    vibrate()
                    deleteCredential(name)
                    updateCredentialList()
                    true
                }
            }
            
            card.addView(TextView(this).apply {
                text = "🔑 $name"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            
            card.addView(TextView(this).apply {
                text = "tap to insert"
                textSize = 10f
                setTextColor(colorIcon)
            })
            
            credentialListContainer?.addView(card)
        }
    }
    
    private fun getCredentials(): List<Pair<String, String>> {
        val json = prefs.getString(CREDENTIALS_KEY, "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                obj.getString("name") to obj.getString("value")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveCredential(name: String, value: String) {
        val credentials = getCredentials().toMutableList()
        credentials.removeIf { it.first == name }
        credentials.add(0, name to value)
        
        val arr = org.json.JSONArray()
        credentials.forEach { (n, v) ->
            arr.put(org.json.JSONObject().apply {
                put("name", n)
                put("value", v)
            })
        }
        prefs.edit().putString(CREDENTIALS_KEY, arr.toString()).apply()
    }
    
    private fun deleteCredential(name: String) {
        val credentials = getCredentials().toMutableList()
        credentials.removeIf { it.first == name }
        
        val arr = org.json.JSONArray()
        credentials.forEach { (n, v) ->
            arr.put(org.json.JSONObject().apply {
                put("name", n)
                put("value", v)
            })
        }
        prefs.edit().putString(CREDENTIALS_KEY, arr.toString()).apply()
        show("Deleted: $name")
    }

    // --- TEXT KEYBOARD ---
    
    private fun showTextKeyboard() {
        keyboardMode = "text"
        hideAllPanels()
        keyGridContainer?.visibility = View.VISIBLE
        rebuildKeys()
    }
    
    private fun hideAllPanels() {
        extraPanel?.let {
            container?.removeView(it)
            extraPanel = null
        }
    }
    
    private fun showSettings() {
        vibrate()
        show("Settings: Explore the toolbar features!")
    }

    private fun setupKeyboardGrid() {
        keyGridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), 0, dp(6), 0)
        }

        addKeyRow(listOf("q","w","e","r","t","y","u","i","o","p"))
        addKeyRow(listOf("a","s","d","f","g","h","j","k","l"))
        addKeyRow(listOf("⇧","z","x","c","v","b","n","m","⌫"), hasSpecial = true)
        addKeyRow(listOf("123",",","SPACE",".","↵"), isBottom = true)

        container?.addView(keyGridContainer)
    }

    private fun addKeyRow(keys: List<String>, hasSpecial: Boolean = false, isBottom: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(3) }
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
                    btn.setOnClickListener { 
                        handleDelete()
                    }
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

        keyGridContainer?.addView(row)
    }

    private fun makeToolbarBtn(iconRes: Int, action: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(40))
            setImageResource(iconRes)
            setColorFilter(colorIcon)
            background = null
            scaleType = android.widget.ImageView.ScaleType.CENTER
            isClickable = true
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
            setOnClickListener { 
                val word = text?.toString() ?: ""
                if (word.isNotEmpty()) sendText("$word ")
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
        
        // Handle credential input if we're adding credentials
        if (isAddingCredential && keyboardMode == "credentials") {
            if (credentialInputTarget == "name") {
                currentCredentialName += text
                showCredentialInputPanel()
            } else {
                currentCredentialValue += text
                showCredentialInputPanel()
            }
            return
        }
        
        saveToClipboardHistory(text)
        updateSuggestions(text)
    }

    private fun sendChar(c: Char) {
        vibrate()
        
        // Handle credential input
        if (isAddingCredential && keyboardMode == "credentials") {
            if (credentialInputTarget == "name") {
                currentCredentialName += c
                showCredentialInputPanel()
            } else {
                currentCredentialValue += c
                showCredentialInputPanel()
            }
            return
        }
        
        currentInputConnection?.commitText(c.toString(), 1)
    }
    
    private fun handleDelete() {
        vibrate()
        
        // Handle credential input deletion
        if (isAddingCredential && keyboardMode == "credentials") {
            if (credentialInputTarget == "name" && currentCredentialName.isNotEmpty()) {
                currentCredentialName = currentCredentialName.dropLast(1)
                showCredentialInputPanel()
                return
            } else if (credentialInputTarget == "value" && currentCredentialValue.isNotEmpty()) {
                currentCredentialValue = currentCredentialValue.dropLast(1)
                showCredentialInputPanel()
                return
            }
        }
        
        currentInputConnection?.let { ic ->
            val sel = ic.getSelectedText(0)
            if (sel.isNullOrEmpty()) {
                ic.deleteSurroundingText(1, 0)
            } else {
                ic.commitText("", 1)
            }
        }
        suggestionBar?.visibility = View.GONE
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
        suggestionBar?.visibility = View.VISIBLE
        val last = typed.trim().split(" ").lastOrNull() ?: ""
        if (last.isNotEmpty() && currentLang == "en") {
            tvSugg1?.text = "${last}ing"
            tvSugg2?.text = "${last}ed"
            tvSugg3?.text = "${last}ly"
        } else {
            tvSugg1?.text = "the"
            tvSugg2?.text = "to"
            tvSugg3?.text = "and"
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
        keyGridContainer?.removeAllViews()

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
        if (isVoiceActive.get()) {
            stopVoice()
            voiceBar?.visibility = View.GONE
            toolbar?.visibility = View.VISIBLE
        } else {
            voiceBar?.visibility = View.VISIBLE
            toolbar?.visibility = View.GONE
            suggestionBar?.visibility = View.GONE
            startVoice()
        }
    }

    private fun swapVoiceEngine() {
        vibrate()
        voiceEngine = if (voiceEngine == "android") "groq" else "android"
        val langName = if (currentLang == "en") "English" else "বাংলা"
        tvVoiceLang?.text = "$langName (${voiceEngine.replaceFirstChar { it.uppercase() }})"
        if (isVoiceActive.get()) { stopVoice(); startVoice() }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            return cm.activeNetworkInfo?.isConnected == true
        }
    }

    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            show("Please grant microphone permission")
            tvVoiceStatus?.text = "No permission"
            return
        }

        isVoiceActive.set(true)
        animateVisualizer(true)

        when (voiceEngine) {
            "android" -> {
                if (isSpeechRecognizerAvailable && speechRecognizer != null) startAndroidVoice()
                else {
                    tvVoiceStatus?.text = "Voice not available"
                    isVoiceActive.set(false)
                    animateVisualizer(false)
                }
            }
            "groq" -> {
                if (isNetworkAvailable()) startGroqVoice()
                else {
                    tvVoiceStatus?.text = "No internet"
                    isVoiceActive.set(false)
                    animateVisualizer(false)
                }
            }
        }
    }

    private fun stopVoice() {
        isVoiceActive.set(false)
        animateVisualizer(false)
        when (voiceEngine) {
            "android" -> { try { speechRecognizer?.stopListening() } catch (e: Exception) {} }
            "groq" -> stopGroqVoice()
        }
    }

    private fun startAndroidVoice() {
        tvVoiceStatus?.text = "Listening..."
        try {
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p0: Bundle?) { if (isVoiceActive.get()) tvVoiceStatus?.text = "Speak now..." }
                override fun onBeginningOfSpeech() { if (isVoiceActive.get()) tvVoiceStatus?.text = "Listening..." }
                override fun onRmsChanged(rms: Float) {
                    if (!isVoiceActive.get()) return
                    val n = (rms / 10).coerceIn(0f, 1f)
                    visualizerBars?.forEachIndexed { i, bar ->
                        bar.layoutParams.height = dp(8 + (n * 12 * (i % 2 + 1)).toInt())
                        bar.alpha = 0.5f + n * 0.5f
                        bar.requestLayout()
                    }
                }
                override fun onBufferReceived(p0: ByteArray?) {}
                override fun onEndOfSpeech() {
                    if (isVoiceActive.get()) {
                        tvVoiceStatus?.text = "Processing..."
                        mainHandler.postDelayed({ if (isVoiceActive.get()) startAndroidVoice() }, VOICE_RESTART_DELAY_MS)
                    }
                }
                override fun onError(error: Int) {
                    if (!isVoiceActive.get()) return
                    tvVoiceStatus?.text = "Error: $error"
                    if (error != 7 && error != 6 && error != 9) {
                        mainHandler.postDelayed({ if (isVoiceActive.get()) startAndroidVoice() }, VOICE_RESTART_DELAY_MS)
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (!isVoiceActive.get()) return
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { sendText(it) }
                    if (isVoiceActive.get()) mainHandler.postDelayed({ if (isVoiceActive.get()) startAndroidVoice() }, 300)
                }
                override fun onPartialResults(partial: Bundle?) {
                    if (!isVoiceActive.get()) return
                    partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { tvVoiceStatus?.text = it }
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
            tvVoiceStatus?.text = "Voice error"
            if (isVoiceActive.get()) mainHandler.postDelayed({ if (isVoiceActive.get()) startAndroidVoice() }, VOICE_RESTART_DELAY_MS)
        }
    }

    private fun startGroqVoice() {
        tvVoiceStatus?.text = "Recording..."
        recordingStartTime = System.currentTimeMillis()
        try {
            audioFile?.delete()
            audioFile = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
            groqVisualizerJob = scope.launch {
                var a = 0
                while (isVoiceActive.get()) {
                    a = (a + 1) % 10
                    withContext(Dispatchers.Main) {
                        visualizerBars?.forEachIndexed { i, bar ->
                            bar.layoutParams.height = dp(8 + ((Math.sin((a + i).toDouble()) + 1) * 6).toInt())
                            bar.alpha = 0.5f + (Math.sin((a + i).toDouble()).toFloat() * 0.25f) + 0.25f
                            bar.requestLayout()
                        }
                    }
                    delay(100)
                }
            }
        } catch (e: Exception) {
            tvVoiceStatus?.text = "Recording failed"
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
        }
    }

    private fun stopGroqVoice() {
        groqVisualizerJob?.cancel()
        groqVisualizerJob = null
        val f = audioFile
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
            val dur = System.currentTimeMillis() - recordingStartTime
            f?.let { if (it.exists() && it.length() > 1000 && dur >= MIN_RECORDING_DURATION_MS) { tvVoiceStatus?.text = "Transcribing..."; sendToGroq(it) } else { it.delete() } }
        } catch (e: Exception) { f?.delete() }
        audioFile = null
    }

    private fun sendToGroq(file: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-large-v3")
                    .addFormDataPart("file", "audio.m4a", file.asRequestBody("audio/mp4".toMediaType()))
                    .addFormDataPart("language", if (currentLang == "bn") "bn" else "en")
                    .addFormDataPart("response_format", "json").build()
                val req = Request.Builder().url("https://api.groq.com/openai/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}").post(body).build()
                val resp = client.newCall(req).execute()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        val t = resp.body?.string()?.substringAfter("\"text\":\"")?.substringBefore("\"")
                            ?.replace("\\n", "\n")?.replace("\\\"", "\"")?.replace("\\'", "'") ?: ""
                        if (t.isNotEmpty()) { sendText(t); tvVoiceStatus?.text = "Done!" } else tvVoiceStatus?.text = "No speech"
                    } else tvVoiceStatus?.text = "API Error: ${resp.code}"
                    resp.body?.close()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { tvVoiceStatus?.text = "Error" }
            } finally { try { file.delete() } catch (e: Exception) {} }
        }
    }

    private fun animateVisualizer(active: Boolean) {
        visualizerBars?.forEach { bar ->
            bar.alpha = if (active) 0.5f else 0.3f
            bar.layoutParams.height = dp(if (active) 12 else 8)
            bar.requestLayout()
        }
    }

    // --- UTILS ---

    private fun show(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun vibrate() {
        try {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(10)
        } catch (e: Exception) {}
    }

    private fun dp(v: Int): Int = if (::displayMetrics.isInitialized) (v * displayMetrics.density).toInt() else v

    override fun onDestroy() {
        super.onDestroy()
        try {
            scope.cancel()
            groqVisualizerJob?.cancel()
            speechRecognizer?.destroy()
            mediaRecorder?.release()
            audioFile?.delete()
        } catch (e: Exception) {}
    }
}
