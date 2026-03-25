package com.aikeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

class AiKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "AiKeyboard"
    }

    private var currentText = StringBuilder()
    private var capsLock = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== KEYBOARD SERVICE CREATED ===")
    }

    override fun onCreateInputView(): View {
        Log.d(TAG, "=== Creating keyboard view ===")

        return try {
            createNativeKeyboardView()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating keyboard view", e)
            createErrorView("Error: ${e.message}")
        }
    }

    private fun createNativeKeyboardView(): View {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Header with panel buttons
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
        }

        val panels = listOf("ABC", "🎤", "🌐", "😀")
        panels.forEach { panel ->
            val btn = Button(this).apply {
                text = panel
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 14f
                setAllCaps(false)
                setPadding(16, 8, 16, 8)
            }
            headerLayout.addView(btn)
        }
        mainLayout.addView(headerLayout)

        // English QWERTY keyboard
        val rows = listOf(
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("⇧","z","x","c","v","b","n","m","⌫"),
            listOf("🌐","😊","    Space    ",".", "↵")
        )

        rows.forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 2, 0, 2)
            }

            row.forEach { key ->
                val keyBtn = Button(this).apply {
                    text = key
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#2D2D2D"))
                    textSize = 16f
                    setAllCaps(false)

                    val width = when {
                        key.contains("Space") -> 180
                        key == "⇧" || key == "⌫" -> 50
                        key == "🌐" || key == "😊" || key == "." -> 45
                        else -> 35
                    }

                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(width),
                        dpToPx(44)
                    ).apply {
                        setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
                    }

                    setOnClickListener {
                        handleKeyPress(key)
                    }

                    setOnLongClickListener {
                        if (key == "⌫") {
                            currentText.clear()
                            currentInputConnection?.deleteSurroundingText(10000, 0)
                            Toast.makeText(this@AiKeyboardService, "Cleared all", Toast.LENGTH_SHORT).show()
                            true
                        } else {
                            false
                        }
                    }
                }
                rowLayout.addView(keyBtn)
            }
            mainLayout.addView(rowLayout)
        }

        // Language switch
        val langLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER
            setPadding(8, 6, 8, 6)
        }

        val enBtn = Button(this).apply {
            text = "EN"
            setTextColor(Color.parseColor("#4285F4"))
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 12f
        }

        val bnBtn = Button(this).apply {
            text = "বাং"
            setTextColor(Color.GRAY)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 12f
        }

        langLayout.addView(enBtn)
        langLayout.addView(bnBtn)
        mainLayout.addView(langLayout)

        Log.d(TAG, "=== Keyboard view created successfully ===")
        return mainLayout
    }

    private fun handleKeyPress(key: String) {
        Log.d(TAG, "Key pressed: $key")

        when {
            key == "⌫" -> {
                if (currentText.isNotEmpty()) {
                    currentText.deleteCharAt(currentText.length - 1)
                }
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            key.contains("Space") -> {
                currentText.append(" ")
                currentInputConnection?.commitText(" ", 1)
            }
            key == "↵" -> {
                currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
            }
            key == "⇧" -> {
                capsLock = !capsLock
                Toast.makeText(this, "CAPS: ${if (capsLock) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            }
            key == "🌐" -> {
                Toast.makeText(this, "Translation coming soon!", Toast.LENGTH_SHORT).show()
            }
            key == "🎤" -> {
                Toast.makeText(this, "Voice typing coming soon!", Toast.LENGTH_SHORT).show()
            }
            key == "😊" -> {
                Toast.makeText(this, "Emoji panel coming soon!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val char = if (capsLock) key.uppercase() else key
                currentText.append(char)
                currentInputConnection?.commitText(char, 1)
            }
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput")
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        Log.d(TAG, "=== KEYBOARD VISIBLE - READY TO TYPE ===")
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "onFinishInput")
    }

    override fun onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===")
        super.onDestroy()
    }
}
