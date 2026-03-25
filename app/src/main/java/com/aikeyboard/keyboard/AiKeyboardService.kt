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

class AiKeyboardService : InputMethodService() {
    
    companion object {
        private const val TAG = "AiKeyboard"
    }
    
    private var currentText = StringBuilder()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== KEYBOARD SERVICE CREATED ===")
    }
    
    override fun onCreateInputView(): View {
        Log.d(TAG, "=== Creating keyboard view ===")
        
        return createSimpleKeyboard()
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
        
        // Text preview
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
        
        val deleteBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_input_delete)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(8, 8, 8, 8)
            setOnClickListener {
                if (currentText.isNotEmpty()) {
                    currentText.deleteCharAt(currentText.length - 1)
                    currentInputConnection?.deleteSurroundingText(1, 0)
                }
            }
            setOnLongClickListener {
                currentText.clear()
                currentInputConnection?.deleteSurroundingText(100, 0)
                true
            }
        }
        
        previewLayout.addView(previewText)
        previewLayout.addView(deleteBtn)
        mainLayout.addView(previewLayout)
        
        // Panel buttons
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
                val keyBtn = Button(this).apply {
                    text = key
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.parseColor("#2D2D2D"))
                    textSize = 16f
                    setAllCaps(false)
                    setTypeface(null, Typeface.NORMAL)
                    
                    val width = when (key) {
                        "Space" -> 150
                        "?123", "😊", "." -> 60
                        else -> 35
                    }
                    
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(width),
                        dpToPx(42)
                    ).apply {
                        setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
                    }
                    
                    setOnClickListener {
                        when (key) {
                            "⌫" -> {
                                if (currentText.isNotEmpty()) {
                                    currentText.deleteCharAt(currentText.length - 1)
                                }
                                currentInputConnection?.deleteSurroundingText(1, 0)
                            }
                            "Space" -> {
                                currentText.append(" ")
                                currentInputConnection?.commitText(" ", 1)
                            }
                            "↵" -> {
                                currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
                            }
                            "⇧" -> {
                                // Shift - could toggle caps
                            }
                            else -> {
                                currentText.append(key)
                                currentInputConnection?.commitText(key, 1)
                            }
                        }
                    }
                    
                    setOnLongClickListener {
                        if (key == "⌫") {
                            currentText.clear()
                            currentInputConnection?.deleteSurroundingText(100, 0)
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
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 4)
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
