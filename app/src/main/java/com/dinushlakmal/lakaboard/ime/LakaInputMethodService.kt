package com.dinushlakmal.lakaboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dinushlakmal.lakaboard.audio.SoundHapticHelper
import com.dinushlakmal.lakaboard.ui.LakaBoardKeyboard
import com.dinushlakmal.lakaboard.viewmodel.KeyboardViewModel

/**
 * LakaInputMethodService
 * ---------------------------------------------------------------------
 * System-registered IME service. Hosts a ComposeView for the keyboard UI
 * and bridges KeyboardViewModel callbacks to the active InputConnection.
 */
class LakaInputMethodService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var viewModel: KeyboardViewModel? = null
    private var soundHapticHelper: SoundHapticHelper? = null

    override fun onCreate() {
        super.onCreate()
        try {
            savedStateRegistryController.performRestore(null)
        } catch (_: Throwable) {}
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        soundHapticHelper = SoundHapticHelper(this)
        viewModel = KeyboardViewModel()
        wireViewModelCallbacks()
    }

    private fun wireViewModelCallbacks() {
        val vm = viewModel ?: return
        vm.onCommitText = { text ->
            try {
                currentInputConnection?.commitText(text, 1)
            } catch (_: Throwable) {}
        }
        vm.onUpdateComposingWord = { rendered ->
            try {
                currentInputConnection?.setComposingText(rendered, 1)
            } catch (_: Throwable) {}
        }
        vm.onFinishComposing = {
            try {
                currentInputConnection?.finishComposingText()
            } catch (_: Throwable) {}
        }
        vm.onDeleteBackward = {
            try {
                currentInputConnection?.deleteSurroundingText(1, 0)
            } catch (_: Throwable) {}
        }
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        try {
            window?.window?.decorView?.let { decorView ->
                decorView.setViewTreeLifecycleOwner(this)
                decorView.setViewTreeViewModelStoreOwner(this)
                decorView.setViewTreeSavedStateRegistryOwner(this)
            }
        } catch (_: Throwable) {}

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@LakaInputMethodService.lifecycle))
            setViewTreeLifecycleOwner(this@LakaInputMethodService)
            setViewTreeViewModelStoreOwner(this@LakaInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@LakaInputMethodService)
            setContent {
                val vm = viewModel ?: KeyboardViewModel().also { viewModel = it; wireViewModelCallbacks() }
                val sh = soundHapticHelper ?: SoundHapticHelper(this@LakaInputMethodService).also { soundHapticHelper = it }

                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides this@LakaInputMethodService
                ) {
                    LakaBoardKeyboard(
                        viewModel = vm,
                        soundHapticHelper = sh,
                        onRequestHideKeyboard = {
                            try {
                                requestHideSelf(0)
                            } catch (_: Throwable) {}
                        }
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
        try {
            currentInputConnection?.finishComposingText()
        } catch (_: Throwable) {}
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        soundHapticHelper?.release()
        soundHapticHelper = null
        store.clear()
        super.onDestroy()
    }
}
