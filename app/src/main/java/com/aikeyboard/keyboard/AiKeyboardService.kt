package com.aikeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.view.Gravity
import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.aikeyboard.ui.theme.KeyboardTheme

class AiKeyboardService : InputMethodService() {
    
    companion object {
        private const val TAG = "AiKeyboardService"
        var isServiceRunning = false
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        Log.d(TAG, "=== SERVICE CREATED ===")
    }
    
    override fun onCreateInputView(): View {
        Log.d(TAG, "=== onCreateInputView called ===")
        
        return try {
            val view = ComposeView(this).apply {
                setContent {
                    KeyboardTheme {
                        KeyboardContent(
                            onTextCommit = { text ->
                                currentInputConnection?.commitText(text, 1)
                            },
                            onDelete = {
                                currentInputConnection?.deleteSurroundingText(1, 0)
                            },
                            onEnter = {
                                currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
                            }
                        )
                    }
                }
            }
            Log.d(TAG, "=== ComposeView SUCCESS ===")
            view
        } catch (e: Exception) {
            Log.e(TAG, "=== ComposeView FAILED: ${e.message}", e)
            createFallbackView()
        }
    }
    
    private fun createFallbackView(): View {
        Log.d(TAG, "=== Creating FALLBACK view ===")
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                400
            )
            gravity = Gravity.CENTER
            
            addView(TextView(context).apply {
                text = "AI Voice Keyboard"
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            })
            
            addView(TextView(context).apply {
                text = "Type in any text field"
                textSize = 14f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 20, 0, 0)
            })
        }
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput")
    }
    
    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        Log.d(TAG, "=== KEYBOARD VISIBLE ===")
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "onFinishInput")
    }
    
    override fun onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===")
        isServiceRunning = false
        super.onDestroy()
    }
}
