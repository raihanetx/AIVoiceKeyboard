package com.aikeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import com.aikeyboard.ui.theme.KeyboardTheme

class AiKeyboardService : InputMethodService() {
    
    companion object {
        private const val TAG = "AiKeyboardService"
    }
    
    private var keyboardView: View? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Keyboard service created")
    }
    
    override fun onCreateInputView(): View {
        Log.d(TAG, "Creating input view")
        return try {
            keyboardView = ComposeView(this).apply {
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
            keyboardView!!
        } catch (e: Exception) {
            Log.e(TAG, "Error creating keyboard view", e)
            // Return empty view as fallback
            View(this)
        }
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
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Keyboard service destroyed")
    }
}
