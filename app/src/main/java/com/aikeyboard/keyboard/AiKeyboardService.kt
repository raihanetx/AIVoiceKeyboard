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
    
    private var keyboardView: ComposeView? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Keyboard service created")
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
            composeView
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
    
    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d(TAG, "onFinishInputView: finishing=$finishingInput")
    }
    
    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        // Properly dispose ComposeView to prevent memory leaks
        try {
            keyboardView?.disposeComposition()
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing composition", e)
        }
        keyboardView = null
        super.onDestroy()
        Log.d(TAG, "Keyboard service destroyed")
    }
}
