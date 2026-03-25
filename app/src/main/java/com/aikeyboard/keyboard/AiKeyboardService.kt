package com.aikeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.view.Gravity
import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.aikeyboard.ui.theme.KeyboardTheme

class AiKeyboardService : InputMethodService() {
    
    companion object {
        private const val TAG = "AiKeyboardService"
    }
    
    private var keyboardView: View? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Keyboard service created successfully")
    }
    
    override fun onCreateInputView(): View {
        Log.d(TAG, "Creating input view")
        
        return try {
            val composeView = ComposeView(this)
            composeView.setContent {
                KeyboardTheme {
                    KeyboardContent(
                        onTextCommit = { text ->
                            try {
                                currentInputConnection?.commitText(text, 1)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error committing text", e)
                            }
                        },
                        onDelete = {
                            try {
                                currentInputConnection?.deleteSurroundingText(1, 0)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting text", e)
                            }
                        },
                        onEnter = {
                            try {
                                currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error performing enter action", e)
                            }
                        }
                    )
                }
            }
            keyboardView = composeView
            Log.d(TAG, "ComposeView created successfully")
            composeView
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ComposeView, using fallback", e)
            // Create a simple fallback view
            createFallbackView()
        }
    }
    
    private fun createFallbackView(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                300
            )
            gravity = Gravity.CENTER
        }
        
        val textView = TextView(this).apply {
            text = "AI Voice Keyboard\n(Tap to type)"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        
        layout.addView(textView)
        keyboardView = layout
        Log.d(TAG, "Fallback view created")
        return layout
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput: restarting=$restarting")
    }
    
    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        Log.d(TAG, "onStartInputView: restarting=$restarting")
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "onFinishInput")
    }
    
    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d(TAG, "onFinishInputView: finishing=$finishingInput")
    }
    
    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        try {
            (keyboardView as? ComposeView)?.disposeComposition()
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing composition", e)
        }
        keyboardView = null
        super.onDestroy()
        Log.d(TAG, "Keyboard service destroyed")
    }
}
