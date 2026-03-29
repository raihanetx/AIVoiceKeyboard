package com.aikeyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.aikeyboard.core.di.AppModule
import com.aikeyboard.feature.keyboard.ui.KeyboardScreen

/**
 * The Android InputMethodService that hosts the entire Compose keyboard.
 *
 * Because an IME is NOT a ComponentActivity we manually implement
 * LifecycleOwner, ViewModelStoreOwner, and SavedStateRegistryOwner
 * so that ComposeView and ViewModel machinery works correctly.
 */
class PixelProIME :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    // ── ViewModelStore ─────────────────────────────────────────────────────────

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    // ── SavedStateRegistry ─────────────────────────────────────────────────────

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    // ── ViewModel (one per IME session via DI module) ──────────────────────────

    private val viewModel by lazy { AppModule.provideKeyboardViewModel(this) }

    // ── IME lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        return ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                KeyboardScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        viewModel.setInputConnection(currentInputConnection)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
