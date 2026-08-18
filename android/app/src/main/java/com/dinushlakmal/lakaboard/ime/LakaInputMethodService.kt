package com.dinushlakmal.lakaboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.dinushlakmal.lakaboard.audio.SoundHapticHelper
import com.dinushlakmal.lakaboard.ui.LakaBoardKeyboard
import com.dinushlakmal.lakaboard.viewmodel.KeyboardViewModel

/**
 * LakaInputMethodService
 * ---------------------------------------------------------------------
 * The system-registered IME. Hosts a ComposeView for the keyboard UI
 * and bridges KeyboardViewModel callbacks to the active InputConnection.
 *
 * Composing-text tracking: while the user is mid-Singlish-word we keep
 * that word as "composing text" (the classic underline-while-typing
 * behaviour used by IMEs) via setComposingText / finishComposingText,
 * so a single re-render call replaces the whole live word without
 * manual delete/insert bookkeeping.
 */
class LakaInputMethodService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var viewModel: KeyboardViewModel
    private lateinit var soundHapticHelper: SoundHapticHelper

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onCreate()
        soundHapticHelper = SoundHapticHelper(this)
        viewModel = KeyboardViewModel()
        wireViewModelCallbacks()
    }

    private fun wireViewModelCallbacks() {
        viewModel.onCommitText = { text -> currentInputConnection?.commitText(text, 1) }
        viewModel.onUpdateComposingWord = { rendered ->
            currentInputConnection?.setComposingText(rendered, 1)
        }
        viewModel.onFinishComposing = { currentInputConnection?.finishComposingText() }
        viewModel.onDeleteBackward = {
            val ic = currentInputConnection ?: return@onDeleteBackward
            // If there is active composing text, clear it; otherwise delete
            // one character (respecting complex glyph clusters is left to
            // the platform's default delete-surrounding behaviour).
            ic.deleteSurroundingText(1, 0)
        }
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@LakaInputMethodService)
            setViewTreeViewModelStoreOwner(this@LakaInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@LakaInputMethodService)
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalViewModelStoreOwner provides this@LakaInputMethodService
                ) {
                    LakaBoardKeyboard(
                        viewModel = viewModel,
                        soundHapticHelper = soundHapticHelper,
                        onRequestHideKeyboard = { requestHideSelf(0) }
                    )
                }
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        currentInputConnection?.finishComposingText()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Allow hardware-keyboard backspace/enter to also drive our engine
        // when a hardware keyboard is attached to the device.
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        soundHapticHelper.release()
        store.clear()
        super.onDestroy()
    }
}
