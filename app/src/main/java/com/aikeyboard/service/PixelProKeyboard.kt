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
 * PixelProKeyboard v1.2.0 - Fixed All UI Issues
 */
class PixelProKeyboard : android.inputmethodservice.InputMethodService() {

    companion object {
        private const val TAG = "PixelProKeyboard"
        private const val PREFS_NAME = "PixelProKeyboardPrefs"
        private const val CREDENTIALS_KEY = "saved_credentials"
        private const val CLIPBOARD_HISTORY_KEY = "clipboard_history"
    }

    // State
    private var isCaps = false
    private var currentLang = "en"
    private var voiceEngine = "android"
    private val isVoiceActive = AtomicBoolean(false)
    private var keyboardMode = "text"
    
    // Credential input state
    private var currentCredentialName = ""
    private var currentCredentialValue = ""
    private var isAddingCredential = false
    private var inputTarget = "name"

    // Colors
    private val colorBg = Color.parseColor("#E8EAED")
    private val colorKeyBg = Color.parseColor("#FFFFFF")
    private val colorKeyText = Color.parseColor("#202124")
    private val colorSpecialBg = Color.parseColor("#DADCE0")
    private val colorAccent = Color.parseColor("#1A73E8")
    private val colorIcon = Color.parseColor("#5F6368")
    private val colorError = Color.parseColor("#EA4335")
    private val colorCardBg = Color.parseColor("#F1F3F4")

    // Views
    private var mainContainer: LinearLayout? = null
    private var topAreaContainer: LinearLayout? = null
    private var toolbar: LinearLayout? = null
    private var panelArea: LinearLayout? = null
    private var keyboardArea: LinearLayout? = null
    private var voiceBar: LinearLayout? = null
    private var suggestionBar: LinearLayout? = null
    
    // Voice
    private var speechRecognizer: SpeechRecognizer? = null
    private var isSpeechAvailable = false
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var visualizerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var prefs: SharedPreferences
    private lateinit var clipboardMgr: ClipboardManager
    private var recordingStart = 0L

    // Emoji
    private val emojis = mapOf(
        "faces" to listOf("😀","😃","😄","😁","😅","😂","🤣","😊","😇","🙂","😉","😌","😍","🥰","😘","😗","😙","😚","😋","😛"),
        "hands" to listOf("👍","👎","👌","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","👇","☝️","✋","🤚","🖐","🖖","👋","🤝","👏"),
        "hearts" to listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","💟","♥️"),
        "food" to listOf("🍎","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍑","🍒","🥝","🍅","🥥","🥑","🍆","🥔","🥕","🌽","🌶️","🥒"),
        "animals" to listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🐔","🐧","🐦","🐤","🦆")
    )
    private var currentCategory = "faces"
    private var emojiGrid: GridView? = null

    // Bangla
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
        displayMetrics = resources.displayMetrics
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        clipboardMgr = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        isSpeechAvailable = SpeechRecognizer.isRecognitionAvailable(this)
        if (isSpeechAvailable) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }
    }

    override fun onCreateInputView(): View {
        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
        }
        
        topAreaContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        setupToolbar()
        setupSuggestionBar()
        setupVoiceBar()
        
        mainContainer?.addView(topAreaContainer)
        
        panelArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        mainContainer?.addView(panelArea)
        
        keyboardArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(8))
        }
        setupKeyboard()
        mainContainer?.addView(keyboardArea)
        
        return mainContainer!!
    }

    private fun setupToolbar() {
        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(40))
            setPadding(dp(4), 0, dp(4), 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        val buttons = listOf(
            Triple(R.drawable.ic_smile, "emoji") { togglePanel("emoji") },
            Triple(R.drawable.ic_clipboard, "clipboard") { togglePanel("clipboard") },
            Triple(R.drawable.ic_key, "credentials") { togglePanel("credentials") }
        )
        
        buttons.forEach { (icon, _, action) ->
            toolbar?.addView(ImageButton(this).apply {
                setImageResource(icon)
                setColorFilter(colorIcon)
                background = null
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(36))
                scaleType = android.widget.ImageView.ScaleType.CENTER
                setOnClickListener { action() }
            })
        }
        
        toolbar?.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        
        toolbar?.addView(ImageButton(this).apply {
            setImageResource(R.drawable.ic_globe)
            setColorFilter(colorIcon)
            background = null
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(36))
            scaleType = android.widget.ImageView.ScaleType.CENTER
            setOnClickListener { swapLanguage() }
        })
        
        toolbar?.addView(ImageButton(this).apply {
            setImageResource(R.drawable.ic_mic)
            setColorFilter(colorIcon)
            background = null
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(36))
            scaleType = android.widget.ImageView.ScaleType.CENTER
            setOnClickListener { toggleVoice() }
        })
        
        topAreaContainer?.addView(toolbar)
    }

    private fun setupSuggestionBar() {
        suggestionBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(40))
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
            weightSum = 3f
        }
        repeat(3) {
            suggestionBar?.addView(TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(colorKeyText)
                setOnClickListener { 
                    if (!text.isNullOrEmpty()) sendText("$text ")
                }
            })
        }
        topAreaContainer?.addView(suggestionBar)
    }

    private fun setupVoiceBar() {
        voiceBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(48))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
        }
        
        voiceBar?.addView(TextView(this).apply {
            text = "Android"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(colorAccent)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { voiceEngine = if (voiceEngine == "android") "groq" else "android" }
        })
        
        voiceBar?.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            gravity = Gravity.CENTER
            text = "Tap mic to start"
            textSize = 13f
            setTextColor(colorIcon)
        })
        
        voiceBar?.addView(ImageButton(this).apply {
            setImageResource(R.drawable.ic_stop)
            setColorFilter(Color.WHITE)
            setBackgroundColor(colorError)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            setOnClickListener { toggleVoice() }
        })
        
        topAreaContainer?.addView(voiceBar)
    }

    // Panel Management
    private fun togglePanel(panel: String) {
        vibrate()
        if (keyboardMode == panel) {
            closePanel()
        } else {
            showPanel(panel)
        }
    }

    private fun showPanel(panel: String) {
        keyboardMode = panel
        panelArea?.visibility = View.VISIBLE
        panelArea?.removeAllViews()
        
        when (panel) {
            "emoji" -> showEmojiPanel()
            "clipboard" -> showClipboardPanel()
            "credentials" -> showCredentialsPanel()
        }
    }

    private fun closePanel() {
        keyboardMode = "text"
        panelArea?.visibility = View.GONE
        panelArea?.removeAllViews()
        isAddingCredential = false
    }

    // Emoji Panel
    private fun showEmojiPanel() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        
        // Category tabs
        val tabs = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(40))
        }
        val tabRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        
        emojis.keys.forEach { cat ->
            tabRow.addView(TextView(this).apply {
                text = if (cat == "faces") "😀" else if (cat == "hands") "👍" else if (cat == "hearts") "❤️" else if (cat == "food") "🍎" else "🐶"
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(4), dp(12), dp(4))
                setBackgroundColor(if (cat == currentCategory) colorAccent else Color.TRANSPARENT)
                setTextColor(if (cat == currentCategory) Color.WHITE else colorKeyText)
                setOnClickListener {
                    vibrate()
                    currentCategory = cat
                    showEmojiPanel()
                }
            })
        }
        tabs.addView(tabRow)
        container.addView(tabs)
        
        // Grid
        emojiGrid = GridView(this).apply {
            numColumns = 5
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            verticalSpacing = dp(4)
            horizontalSpacing = dp(4)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            adapter = object : android.widget.BaseAdapter() {
                val items = emojis[currentCategory] ?: emptyList()
                override fun getCount() = items.size
                override fun getItem(p: Int) = items[p]
                override fun getItemId(p: Int) = p.toLong()
                override fun getView(p: Int, c: View?, p2: android.view.ViewGroup?): View {
                    return (c as? TextView ?: TextView(this@PixelProKeyboard).apply {
                        textSize = 28f
                        gravity = Gravity.CENTER
                    }).apply { text = items[p] }
                }
            }
            setOnItemClickListener { _, _, pos, _ ->
                sendText(emojis[currentCategory]?.get(pos) ?: "")
            }
        }
        container.addView(emojiGrid)
        
        panelArea?.addView(container)
    }

    // Clipboard Panel
    private fun showClipboardPanel() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        
        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.WHITE)
        }
        
        header.addView(TextView(this).apply {
            text = "📋 Clipboard History"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        
        header.addView(Button(this).apply {
            text = "Save to Credentials"
            textSize = 10f
            setTextColor(colorAccent)
            background = null
            setOnClickListener {
                val clip = clipboardMgr.primaryClip?.getItemAt(0)?.text?.toString()
                if (!clip.isNullOrBlank()) {
                    currentCredentialValue = clip
                    currentCredentialName = ""
                    inputTarget = "name"
                    isAddingCredential = true
                    showPanel("credentials")
                    showCredentialInput()
                }
            }
        })
        container.addView(header)
        
        // Items
        val items = getClipboardItems()
        if (items.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No clipboard items"
                textSize = 13f
                setTextColor(colorIcon)
                gravity = Gravity.CENTER
                setPadding(0, dp(20), 0, dp(20))
            })
        } else {
            items.take(5).forEach { item ->
                container.addView(createCard(item) {
                    vibrate()
                    sendText(item)
                })
            }
        }
        
        panelArea?.addView(container, LinearLayout.LayoutParams(-1, dp(160)))
    }

    // Credentials Panel
    private fun showCredentialsPanel() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        
        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.WHITE)
        }
        
        header.addView(TextView(this).apply {
            text = "🔐 Credential Safe"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        
        header.addView(Button(this).apply {
            text = "+ Add"
            textSize = 12f
            setTextColor(colorAccent)
            background = null
            setOnClickListener {
                vibrate()
                currentCredentialName = ""
                currentCredentialValue = ""
                inputTarget = "name"
                isAddingCredential = true
                showCredentialInput()
            }
        })
        container.addView(header)
        
        // Content
        if (isAddingCredential) {
            showCredentialInput()
            return
        }
        
        val creds = getCredentials()
        if (creds.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No saved credentials\nTap '+ Add' to save one"
                textSize = 13f
                setTextColor(colorIcon)
                gravity = Gravity.CENTER
                setPadding(0, dp(20), 0, dp(20))
            })
        } else {
            creds.take(5).forEach { (name, _) ->
                container.addView(createCard("🔑 $name") {
                    vibrate()
                    val cred = getCredentials().find { it.first == name }
                    if (cred != null) sendText(cred.second)
                })
            }
        }
        
        panelArea?.addView(container, LinearLayout.LayoutParams(-1, dp(160)))
    }

    private fun showCredentialInput() {
        panelArea?.removeAllViews()
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        
        // Title
        container.addView(TextView(this).apply {
            text = if (inputTarget == "name") "Enter Account Name" else "Enter Password/Value"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        })
        
        // Input display
        container.addView(TextView(this).apply {
            text = if (inputTarget == "name") {
                if (currentCredentialName.isEmpty()) "Type name below..." else currentCredentialName
            } else {
                if (currentCredentialValue.isEmpty()) "Type value below..." else "••••••••"
            }
            textSize = 18f
            setTextColor(if ((if (inputTarget == "name") currentCredentialName else currentCredentialValue).isEmpty()) colorIcon else colorKeyText)
            setPadding(dp(12), dp(16), dp(12), dp(16))
            setBackgroundColor(Color.WHITE)
        })
        
        // Instructions
        container.addView(TextView(this).apply {
            text = "💡 Use the keyboard below to type"
            textSize = 11f
            setTextColor(colorIcon)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        })
        
        // Buttons
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        
        btnRow.addView(Button(this).apply {
            text = "Cancel"
            setTextColor(colorIcon)
            background = null
            setOnClickListener {
                isAddingCredential = false
                showCredentialsPanel()
            }
        })
        
        btnRow.addView(Button(this).apply {
            text = if (inputTarget == "name") "Next →" else "Save"
            setTextColor(colorAccent)
            background = null
            setOnClickListener {
                if (inputTarget == "name") {
                    if (currentCredentialName.isNotEmpty()) {
                        inputTarget = "value"
                        showCredentialInput()
                    }
                } else {
                    if (currentCredentialName.isNotEmpty() && currentCredentialValue.isNotEmpty()) {
                        saveCredential(currentCredentialName, currentCredentialValue)
                        toast("Saved: $currentCredentialName")
                        isAddingCredential = false
                        showCredentialsPanel()
                    }
                }
            }
        })
        container.addView(btnRow)
        
        panelArea?.addView(container, LinearLayout.LayoutParams(-1, dp(160)))
    }

    private fun createCard(text: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(4)
            }
            setOnClickListener { onClick() }
            setOnLongClickListener {
                vibrate()
                if (text.startsWith("🔑")) {
                    deleteCredential(text.substring(2).trim())
                    showCredentialsPanel()
                }
                true
            }
            addView(TextView(context).apply {
                this.text = text
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            addView(TextView(context).apply {
                this.text = "tap"
                textSize = 10f
                setTextColor(colorIcon)
            })
        }
    }

    // Keyboard
    private fun setupKeyboard() {
        keyboardArea?.removeAllViews()
        if (currentLang == "en") {
            addRow(listOf("q","w","e","r","t","y","u","i","o","p"))
            addRow(listOf("a","s","d","f","g","h","j","k","l"))
            addRow(listOf("⇧","z","x","c","v","b","n","m","⌫"), special = true)
            addRow(listOf("123",",","SPACE",".","↵"), bottom = true)
        } else {
            banglaRows.forEachIndexed { i, row ->
                addRow(row, special = i == 2, bottom = i == banglaRows.lastIndex)
            }
        }
    }

    private fun addRow(keys: List<String>, special: Boolean = false, bottom: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        keys.forEach { key ->
            val btn = Button(this).apply {
                isAllCaps = false
                setAllCaps(false)
                textSize = 18f
                setTextColor(colorKeyText)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(5).toFloat()
                    setColor(if (key == "↵") colorAccent else if (key in listOf("⇧","⇪","⌫","123")) colorSpecialBg else colorKeyBg)
                }
                if (key == "↵") setTextColor(Color.WHITE)
                
                val weight = when {
                    bottom && key == "SPACE" -> 4f
                    bottom && key == "123" -> 1.5f
                    bottom && key == "↵" -> 1.5f
                    special && key in listOf("⇧","⇪","⌫") -> 1.5f
                    else -> 1f
                }
                
                layoutParams = LinearLayout.LayoutParams(0, dp(42)).apply {
                    this.weight = weight
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    stateListAnimator = null
                }
            }
            
            when (key) {
                "⇧" -> {
                    btn.text = if (isCaps) "⇪" else "⇧"
                    btn.setOnClickListener { isCaps = !isCaps; setupKeyboard() }
                }
                "⌫" -> {
                    btn.text = "⌫"
                    btn.setOnClickListener { delete() }
                }
                "SPACE" -> {
                    btn.text = if (currentLang == "en") "English" else "বাংলা"
                    btn.textSize = 13f
                    btn.setOnClickListener { sendChar(' ') }
                }
                "↵" -> {
                    btn.text = "↵"
                    btn.setOnClickListener { enter() }
                }
                "123" -> {
                    btn.text = "123"
                    btn.setOnClickListener { toast("Numbers coming soon") }
                }
                else -> {
                    btn.text = if (isCaps && currentLang == "en") key.uppercase() else key
                    btn.setOnClickListener {
                        sendText(btn.text.toString())
                        if (isCaps && currentLang == "en") { isCaps = false; setupKeyboard() }
                    }
                }
            }
            row.addView(btn)
        }
        keyboardArea?.addView(row)
    }

    // Input Logic
    private fun sendText(t: String) {
        vibrate()
        
        // Handle credential input
        if (isAddingCredential) {
            if (inputTarget == "name") {
                currentCredentialName += t
            } else {
                currentCredentialValue += t
            }
            showCredentialInput()
            return
        }
        
        currentInputConnection?.commitText(t, 1)
        saveToClipboard(t)
        updateSuggestions(t)
    }

    private fun sendChar(c: Char) {
        vibrate()
        if (isAddingCredential) {
            if (inputTarget == "name") currentCredentialName += c
            else currentCredentialValue += c
            showCredentialInput()
            return
        }
        currentInputConnection?.commitText(c.toString(), 1)
    }

    private fun delete() {
        vibrate()
        if (isAddingCredential) {
            if (inputTarget == "name" && currentCredentialName.isNotEmpty()) {
                currentCredentialName = currentCredentialName.dropLast(1)
                showCredentialInput()
                return
            } else if (inputTarget == "value" && currentCredentialValue.isNotEmpty()) {
                currentCredentialValue = currentCredentialValue.dropLast(1)
                showCredentialInput()
                return
            }
        }
        currentInputConnection?.deleteSurroundingText(1, 0)
        suggestionBar?.visibility = View.GONE
    }

    private fun enter() {
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

    private fun updateSuggestions(t: String) {
        suggestionBar?.visibility = View.VISIBLE
        val last = t.trim().split(" ").lastOrNull() ?: ""
        (0 until (suggestionBar?.childCount ?: 0)).forEach { i ->
            val tv = suggestionBar?.getChildAt(i) as? TextView
            tv?.text = when (i) {
                0 -> if (last.isNotEmpty()) "${last}ing" else "the"
                1 -> if (last.isNotEmpty()) "${last}ed" else "to"
                2 -> if (last.isNotEmpty()) "${last}ly" else "and"
                else -> ""
            }
        }
    }

    private fun swapLanguage() {
        currentLang = if (currentLang == "en") "bn" else "en"
        toast(if (currentLang == "en") "English" else "বাংলা")
        setupKeyboard()
    }

    // Voice
    private fun toggleVoice() {
        if (isVoiceActive.get()) {
            stopVoice()
            voiceBar?.visibility = View.GONE
            toolbar?.visibility = View.VISIBLE
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                toast("Microphone permission required")
                return
            }
            voiceBar?.visibility = View.VISIBLE
            toolbar?.visibility = View.GONE
            startVoice()
        }
    }

    private fun startVoice() {
        isVoiceActive.set(true)
        if (voiceEngine == "android" && isSpeechAvailable && speechRecognizer != null) {
            startAndroidVoice()
        } else if (voiceEngine == "groq" && isNetworkAvailable()) {
            startGroqVoice()
        } else {
            toast("Voice not available")
            isVoiceActive.set(false)
        }
    }

    private fun stopVoice() {
        isVoiceActive.set(false)
        speechRecognizer?.stopListening()
        mediaRecorder?.apply { stop(); release() }
        mediaRecorder = null
        visualizerJob?.cancel()
    }

    private fun startAndroidVoice() {
        try {
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(b: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(r: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() { if (isVoiceActive.get()) handler.postDelayed({ startAndroidVoice() }, 1000) }
                override fun onError(e: Int) { if (isVoiceActive.get() && e != 7) handler.postDelayed({ startAndroidVoice() }, 1000) }
                override fun onResults(b: Bundle?) {
                    b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { sendText(it) }
                    if (isVoiceActive.get()) handler.postDelayed({ startAndroidVoice() }, 300)
                }
                override fun onPartialResults(b: Bundle?) {}
                override fun onEvent(p: Int, b: Bundle?) {}
            })
            speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentLang == "bn") "bn-BD" else "en-US")
            })
        } catch (e: Exception) { stopVoice() }
    }

    private fun startGroqVoice() {
        try {
            recordingStart = System.currentTimeMillis()
            audioFile = File(cacheDir, "voice_${recordingStart}.m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) { stopVoice() }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.activeNetwork?.let { cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } ?: false
        } else @Suppress("DEPRECATION") cm.activeNetworkInfo?.isConnected == true
    }

    // Storage
    private fun getClipboardItems(): List<String> {
        val json = prefs.getString(CLIPBOARD_HISTORY_KEY, "[]") ?: "[]"
        return try { (0 until org.json.JSONArray(json).length()).map { org.json.JSONArray(json).getString(it) } } catch (e: Exception) { emptyList() }
    }

    private fun saveToClipboard(t: String) {
        if (t.length <= 2) return
        val items = getClipboardItems().toMutableList()
        items.remove(t); items.add(0, t)
        if (items.size > 20) items.removeLast()
        prefs.edit().putString(CLIPBOARD_HISTORY_KEY, org.json.JSONArray(items).toString()).apply()
    }

    private fun getCredentials(): List<Pair<String, String>> {
        val json = prefs.getString(CREDENTIALS_KEY, "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { arr.getJSONObject(it).getString("name") to arr.getJSONObject(it).getString("value") }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveCredential(name: String, value: String) {
        val creds = getCredentials().toMutableList()
        creds.removeIf { it.first == name }
        creds.add(0, name to value)
        val arr = org.json.JSONArray()
        creds.forEach { arr.put(org.json.JSONObject().put("name", it.first).put("value", it.second)) }
        prefs.edit().putString(CREDENTIALS_KEY, arr.toString()).apply()
    }

    private fun deleteCredential(name: String) {
        val creds = getCredentials().toMutableList()
        creds.removeIf { it.first == name }
        val arr = org.json.JSONArray()
        creds.forEach { arr.put(org.json.JSONObject().put("name", it.first).put("value", it.second)) }
        prefs.edit().putString(CREDENTIALS_KEY, arr.toString()).apply()
        toast("Deleted")
    }

    // Utils
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    private fun vibrate() {
        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(8)
        }
    }
    private fun dp(v: Int) = (v * displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        speechRecognizer?.destroy()
        mediaRecorder?.release()
    }
}
