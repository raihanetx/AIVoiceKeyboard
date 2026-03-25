package com.aikeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import com.aikeyboard.ui.theme.AIVoiceKeyboardTheme

class AiKeyboardService : InputMethodService() {
    
    private var keyboardView: View? = null
    
    override fun onCreate() {
        super.onCreate()
    }
    
    override fun onCreateInputView(): View {
        keyboardView = ComposeView(this).apply {
            setContent {
                AIVoiceKeyboardTheme {
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
        return keyboardView!!
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
    }
    
    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
    }
}
