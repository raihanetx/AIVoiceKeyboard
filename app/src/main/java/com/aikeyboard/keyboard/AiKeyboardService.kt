package com.aikeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.view.Gravity
import android.graphics.Color
import android.view.ViewGroup
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper

class AiKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "AiKeyboard"
        private var instance: AiKeyboardService? = null

        fun getInstance(): AiKeyboardService? = instance
    }

    private var currentText = StringBuilder()
    private var isCaps = false
    private var keyboardView: View? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "=== KEYBOARD SERVICE CREATED ===")
        Log.d(TAG, "Package: ${packageName}")
    }

    override fun onCreateInputView(): View {
        Log.d(TAG, "=== Creating keyboard view ===")

        try {
            keyboardView = createSimpleKeyboard()
            Log.d(TAG, "=== Keyboard view created successfully ===")
            return keyboardView!!
        } catch (e: Exception) {
            Log.e(TAG, "Error creating keyboard view", e)
            // Return a simple fallback view
            return createFallbackView()
        }
    }

    private fun createFallbackView(): View {
        return TextView(this).apply {
            text = "AI Voice Keyboard"
            setPadding(32, 32, 32, 32)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            textSize = 18f
            gravity = Gravity.CENTER
        }
    }

    private fun createSimpleKeyboard(): View {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Text preview bar
        val previewLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 12, 16, 12)
            gravity = Gravity.CENTER_VERTICAL
        }

        val previewText = TextView(this).apply {
            text = "AI Voice Keyboard"
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val deleteBtn = createImageButton(android.R.drawable.ic_input_delete) {
            performDelete()
        }

        // Long press delete to clear all
        deleteBtn.setOnLongClickListener {
            currentText.clear()
            currentInputConnection?.deleteSurroundingText(10000, 0)
            Log.d(TAG, "Cleared all text")
            true
        }

        previewLayout.addView(previewText)
        previewLayout.addView(deleteBtn)
        mainLayout.addView(previewLayout)

        // Panel buttons (Voice, Translate, Emoji)
        val panelLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0D1B2A"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }

        val panels = listOf(
            "ABC" to { /* Already on ABC */ },
            "🎤" to { onVoiceButtonClick() },
            "🌐" to { onTranslateButtonClick() },
            "😀" to { /* Emoji picker */ }
        )

        panels.forEach { (text, _) ->
            val btn = createPanelButton(text)
            panelLayout.addView(btn)
        }
        mainLayout.addView(panelLayout)

        // Keyboard rows
        val rows = listOf(
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("⇧","z","x","c","v","b","n","m","⌫"),
            listOf("?123","😊","Space",".","↵")
        )

        rows.forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
            }

            row.forEach { key ->
                val keyBtn = createKeyButton(key)
                rowLayout.addView(keyBtn)
            }
            mainLayout.addView(rowLayout)
        }

        // Language switch bar
        val langLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 4)
        }

        val enBtn = createLangButton("EN", true)
        val bnBtn = createLangButton("বাং", false)

        langLayout.addView(enBtn)
        langLayout.addView(bnBtn)
        mainLayout.addView(langLayout)

        Log.d(TAG, "Keyboard view built with ${mainLayout.childCount} sections")
        return mainLayout
    }

    private fun createKeyButton(key: String): Button {
        return Button(this).apply {
            text = key
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 16f
            setAllCaps(false)
            setTypeface(null, Typeface.NORMAL)
            setPadding(0, 8, 0, 8)

            val width = when (key) {
                "Space" -> 150
                "?123", "😊", "." -> 60
                "⇧", "⌫" -> 45
                else -> 35
            }

            layoutParams = LinearLayout.LayoutParams(
                dpToPx(width),
                dpToPx(44)
            ).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }

            // Set background with rounded corners
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2D2D2D"))
                cornerRadius = dpToPx(4).toFloat()
            }

            setOnClickListener {
                onKeyPress(key)
            }

            setOnLongClickListener {
                if (key == "⌫") {
                    currentText.clear()
                    currentInputConnection?.deleteSurroundingText(10000, 0)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun createPanelButton(text: String): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 14f
            setAllCaps(false)
            setPadding(16, 8, 16, 8)
        }
    }

    private fun createLangButton(text: String, isActive: Boolean): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(if (isActive) Color.parseColor("#4285F4") else Color.GRAY)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 12f
            setPadding(16, 4, 16, 4)
        }
    }

    private fun createImageButton(iconRes: Int, onClick: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            setImageResource(iconRes)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(8, 8, 8, 8)
            setOnClickListener { onClick() }
        }
    }

    private fun onKeyPress(key: String) {
        Log.d(TAG, "Key pressed: $key")

        when (key) {
            "⌫" -> performDelete()
            "Space" -> performInput(" ")
            "↵" -> performEnter()
            "⇧" -> toggleCaps()
            "?123", "😊" -> { /* Symbol/Emoji panel - placeholder */ }
            else -> {
                val char = if (isCaps) key.uppercase() else key
                performInput(char)
            }
        }
    }

    private fun performInput(text: String) {
        currentText.append(text)
        currentInputConnection?.commitText(text, 1)
        Log.d(TAG, "Input: '$text', Current: '${currentText}'")
    }

    private fun performDelete() {
        if (currentText.isNotEmpty()) {
            currentText.deleteCharAt(currentText.length - 1)
        }
        currentInputConnection?.deleteSurroundingText(1, 0)
        Log.d(TAG, "Delete, Current: '${currentText}'")
    }

    private fun performEnter() {
        currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
        // Or send newline for multi-line fields
        // currentInputConnection?.commitText("\n", 1)
        Log.d(TAG, "Enter pressed")
    }

    private fun toggleCaps() {
        isCaps = !isCaps
        Log.d(TAG, "Caps: $isCaps")
        // In a real implementation, you'd rebuild the keyboard with uppercase
        // For now, just toggle the state
    }

    private fun onVoiceButtonClick() {
        Log.d(TAG, "Voice button clicked - starting voice recognition")
        Toast.makeText(this, "Voice input coming soon!", Toast.LENGTH_SHORT).show()
        // TODO: Implement voice recognition with Gemini
    }

    private fun onTranslateButtonClick() {
        Log.d(TAG, "Translate button clicked")
        Toast.makeText(this, "Translation coming soon!", Toast.LENGTH_SHORT).show()
        // TODO: Implement translation with z-ai
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput: restarting=$restarting, inputType=${attribute?.inputType}")
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        Log.d(TAG, "=== KEYBOARD VISIBLE - READY TO TYPE ===")
        Log.d(TAG, "EditorInfo: actionId=${editorInfo?.imeOptions}, inputType=${editorInfo?.inputType}")
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // Sync current text with actual input
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "onFinishInput")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d(TAG, "onFinishInputView: finishing=$finishingInput")
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        Log.d(TAG, "Keyboard window hidden")
    }

    override fun onWindowShown() {
        super.onWindowShown()
        Log.d(TAG, "Keyboard window shown")
    }

    override fun onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===")
        instance = null
        keyboardView = null
        super.onDestroy()
    }
}
