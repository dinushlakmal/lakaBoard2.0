package com.dinushlakmal.lakaboard.viewmodel

import androidx.lifecycle.ViewModel
import com.dinushlakmal.lakaboard.audio.SoundProfile
import com.dinushlakmal.lakaboard.engine.SinglishTransliterationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * KeyboardViewModel
 * ---------------------------------------------------------------------
 * Single source of truth for keyboard UI state, shared between the IME
 * service (which drives the InputConnection) and the Compose UI layer.
 * Holds no Android Context / View references so it stays testable.
 */
class KeyboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState

    /** Callback wired up by the IME service to actually commit finalized text. */
    var onCommitText: ((String) -> Unit)? = null

    /**
     * Callback wired up by the IME service to re-render the "live"
     * (not-yet-committed) Singlish word in the host text field. The
     * service is responsible for deleting the previously-rendered
     * length via InputConnection.deleteSurroundingText before setting
     * the new composing text - see LakaInputMethodService.updateComposingWord().
     */
    var onUpdateComposingWord: ((String) -> Unit)? = null
    var onDeleteBackward: (() -> Unit)? = null

    /** Called when a Singlish word is finalized (space/enter/mode switch). */
    var onFinishComposing: (() -> Unit)? = null

    fun switchMode(mode: KeyboardMode) {
        commitPendingWord()
        _uiState.update { it.copy(mode = mode) }
    }

    fun cycleShift() {
        _uiState.update {
            val next = when (it.shiftState) {
                ShiftState.OFF -> ShiftState.SHIFT_ONCE
                ShiftState.SHIFT_ONCE -> ShiftState.CAPS_LOCK
                ShiftState.CAPS_LOCK -> ShiftState.OFF
            }
            it.copy(shiftState = next)
        }
    }

    /** Called after any single character commit to collapse a one-shot shift. */
    private fun consumeOneShotShift() {
        _uiState.update {
            if (it.shiftState == ShiftState.SHIFT_ONCE) it.copy(shiftState = ShiftState.OFF) else it
        }
    }

    fun onCharacterKey(rawChar: Char) {
        val state = _uiState.value
        val isShiftActive = state.shiftState != ShiftState.OFF
        val effectiveChar = if (isShiftActive) rawChar.uppercaseChar() else rawChar

        when (state.mode) {
            KeyboardMode.SINGLISH -> {
                // Buffer letters to re-transliterate the whole current word
                // on every keystroke, so earlier letters can change meaning
                // (e.g. typing "k" then "e" retroactively becomes "කෙ").
                val newBuffer = state.currentWordBuffer + effectiveChar
                val rendered = SinglishTransliterationEngine.transliterateWord(newBuffer)
                val suggestions = SinglishTransliterationEngine.suggestionsFor(newBuffer)
                _uiState.update {
                    it.copy(
                        currentWordBuffer = newBuffer,
                        suggestions = suggestions
                    )
                }
                replaceLiveWordInHost(rendered)
            }
            KeyboardMode.ENGLISH, KeyboardMode.WIJESEKARA, KeyboardMode.SYMBOLS -> {
                onCommitText?.invoke(effectiveChar.toString())
                updateCounts(effectiveChar.toString())
            }
            else -> Unit
        }
        consumeOneShotShift()
    }

    /** Commits a fully-resolved glyph string directly (Wijesekara taps, emoji, phrases). */
    fun commitGlyph(glyph: String) {
        commitPendingWord()
        onCommitText?.invoke(glyph)
        updateCounts(glyph)
        addToClipboardHistory(glyph, autoCapture = false)
    }

    fun onSpace() {
        commitPendingWord()
        onCommitText?.invoke(" ")
        updateCounts(" ")
    }

    fun onEnter() {
        commitPendingWord()
        onCommitText?.invoke("\n")
    }

    fun onBackspace() {
        val state = _uiState.value
        if (state.currentWordBuffer.isNotEmpty()) {
            val newBuffer = state.currentWordBuffer.dropLast(1)
            val rendered = SinglishTransliterationEngine.transliterateWord(newBuffer)
            _uiState.update {
                it.copy(
                    currentWordBuffer = newBuffer,
                    suggestions = SinglishTransliterationEngine.suggestionsFor(newBuffer)
                )
            }
            replaceLiveWordInHost(rendered)
        } else {
            onDeleteBackward?.invoke()
        }
    }

    fun selectSuggestion(candidate: String) {
        val state = _uiState.value
        replaceLiveWordInHost(candidate)
        onCommitText?.invoke(" ")
        updateCounts(candidate + " ")
        addToClipboardHistory(candidate, autoCapture = false)
        _uiState.update { it.copy(currentWordBuffer = "", suggestions = emptyList()) }
    }

    private fun commitPendingWord() {
        val state = _uiState.value
        if (state.mode == KeyboardMode.SINGLISH && state.currentWordBuffer.isNotEmpty()) {
            onFinishComposing?.invoke()
            _uiState.update { it.copy(currentWordBuffer = "", suggestions = emptyList()) }
        }
    }

    /** Forwards the newly-rendered composing word to the IME service. */
    private fun replaceLiveWordInHost(rendered: String) {
        onUpdateComposingWord?.invoke(rendered)
    }

    private fun updateCounts(delta: String) {
        _uiState.update {
            it.copy(
                charCount = it.charCount + delta.length,
                wordCount = it.wordCount + delta.count { c -> c == ' ' }
            )
        }
    }

    fun addToClipboardHistory(text: String, autoCapture: Boolean) {
        if (text.isBlank()) return
        _uiState.update { state ->
            val existing = state.clipboardHistory.filterNot { it.text == text }
            val updated = (listOf(
                ClipboardItem(id = UUID.randomUUID().toString(), text = text)
            ) + existing).take(50)
            state.copy(clipboardHistory = updated)
        }
    }

    fun togglePinClipboardItem(id: String) {
        _uiState.update { state ->
            state.copy(clipboardHistory = state.clipboardHistory.map {
                if (it.id == id) it.copy(pinned = !it.pinned) else it
            }.sortedByDescending { it.pinned })
        }
    }

    fun deleteClipboardItem(id: String) {
        _uiState.update { state ->
            state.copy(clipboardHistory = state.clipboardHistory.filterNot { it.id == id })
        }
    }

    fun pasteClipboardItem(id: String) {
        val item = _uiState.value.clipboardHistory.find { it.id == id } ?: return
        onCommitText?.invoke(item.text)
        updateCounts(item.text)
    }

    fun setTheme(theme: ThemeSpec) {
        _uiState.update { it.copy(theme = theme) }
    }

    fun setSoundProfile(profile: SoundProfile) {
        _uiState.update { it.copy(soundProfile = profile) }
    }

    fun setSoundEnabled(enabled: Boolean) = _uiState.update { it.copy(soundEnabled = enabled) }
    fun setHapticEnabled(enabled: Boolean) = _uiState.update { it.copy(hapticEnabled = enabled) }

    fun toggleThemeCustomizer(show: Boolean) = _uiState.update { it.copy(showThemeCustomizer = show) }
    fun toggleTransliterationGuide(show: Boolean) =
        _uiState.update { it.copy(showTransliterationGuide = show) }
}
