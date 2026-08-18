package com.dinushlakmal.lakaboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinushlakmal.lakaboard.audio.SoundHapticHelper
import com.dinushlakmal.lakaboard.engine.WijesekaraEngine
import com.dinushlakmal.lakaboard.viewmodel.*

private val SINGLISH_ROWS = listOf(
    "q w e r t y u i o p".split(" "),
    "a s d f g h j k l".split(" "),
    "z x c v b n m".split(" ")
)

private val EMOJI_GRID = listOf(
    "😀", "😂", "🥰", "😎", "🙏", "👍", "❤️", "🔥", "🎉", "😢",
    "😅", "🤔", "😴", "🙌", "👏", "💯", "✨", "🌸", "🍚", "☕"
)

@Composable
fun LakaBoardKeyboard(
    viewModel: KeyboardViewModel,
    soundHapticHelper: SoundHapticHelper,
    onRequestHideKeyboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val theme = state.theme

    LaunchedEffect(state.soundEnabled, state.hapticEnabled) {
        soundHapticHelper.setSoundEnabled(state.soundEnabled)
        soundHapticHelper.setHapticEnabled(state.hapticEnabled)
    }

    fun press() = soundHapticHelper.onKeyPress(state.soundProfile)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(theme.keyboardBackground))
            .padding(6.dp)
    ) {
        TopBar(state = state, viewModel = viewModel)

        if (state.mode == KeyboardMode.SINGLISH && state.suggestions.isNotEmpty()) {
            SuggestionBar(state.suggestions) {
                press(); viewModel.selectSuggestion(it)
            }
        }

        Spacer(Modifier.height(6.dp))

        when (state.mode) {
            KeyboardMode.SINGLISH -> SinglishLayout(theme, onKey = { press(); viewModel.onCharacterKey(it) })
            KeyboardMode.WIJESEKARA -> WijesekaraLayout(state, theme, onGlyph = { press(); viewModel.commitGlyph(it) }, onShift = { press(); viewModel.cycleShift() })
            KeyboardMode.ENGLISH -> EnglishLayout(state, theme, onKey = { press(); viewModel.onCharacterKey(it) }, onShift = { press(); viewModel.cycleShift() })
            KeyboardMode.SYMBOLS -> SymbolsLayout(theme, onKey = { press(); viewModel.onCharacterKey(it) })
            KeyboardMode.EMOJI_PHRASES -> EmojiPhrasesLayout(theme, onGlyph = { press(); viewModel.commitGlyph(it) })
            KeyboardMode.CLIPBOARD -> ClipboardLayout(state, viewModel)
        }

        Spacer(Modifier.height(6.dp))
        BottomRow(
            state = state,
            theme = theme,
            onSpace = { press(); viewModel.onSpace() },
            onBackspace = { press(); viewModel.onBackspace() },
            onEnter = { press(); viewModel.onEnter() },
            onModeChange = { viewModel.switchMode(it) }
        )
    }
}

@Composable
private fun TopBar(state: KeyboardUiState, viewModel: KeyboardViewModel) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "LakaBoard",
            color = Color(state.theme.accent),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Row {
            IconButton(onClick = { viewModel.toggleTransliterationGuide(true) }) {
                Icon(Icons.Filled.Language, contentDescription = "Guide", tint = Color(state.theme.keyText))
            }
            IconButton(onClick = { viewModel.toggleThemeCustomizer(true) }) {
                Icon(Icons.Filled.Palette, contentDescription = "Theme", tint = Color(state.theme.keyText))
            }
        }
    }
    if (state.showThemeCustomizer) {
        ThemeCustomizerDialog(
            current = state.theme,
            onSelect = { viewModel.setTheme(it) },
            onDismiss = { viewModel.toggleThemeCustomizer(false) }
        )
    }
    if (state.showTransliterationGuide) {
        TransliterationGuideDialog(onDismiss = { viewModel.toggleTransliterationGuide(false) })
    }
}

@Composable
private fun SuggestionBar(suggestions: List<String>, onPick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { candidate ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.clickable { onPick(candidate) }
            ) {
                Text(
                    text = candidate,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    theme: ThemeSpec,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(2.dp)
            .height(46.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(theme.keyBackground).copy(alpha = theme.keyOpacity),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(label, color = Color(theme.keyText), fontSize = fontSize)
        }
    }
}

@Composable
private fun SinglishLayout(theme: ThemeSpec, onKey: (Char) -> Unit) {
    Column {
        SINGLISH_ROWS.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    KeyButton(key, theme, Modifier.weight(1f)) { onKey(key[0]) }
                }
            }
        }
    }
}

@Composable
private fun EnglishLayout(state: KeyboardUiState, theme: ThemeSpec, onKey: (Char) -> Unit, onShift: () -> Unit) {
    val shifted = state.shiftState != ShiftState.OFF
    Column {
        SINGLISH_ROWS.forEachIndexed { idx, row ->
            Row(Modifier.fillMaxWidth()) {
                if (idx == 2) {
                    KeyButton(if (state.shiftState == ShiftState.CAPS_LOCK) "⇪" else "⇧", theme, Modifier.weight(1.5f)) { onShift() }
                }
                row.forEach { key ->
                    val label = if (shifted) key.uppercase() else key
                    KeyButton(label, theme, Modifier.weight(1f)) { onKey(key[0]) }
                }
            }
        }
    }
}

@Composable
private fun WijesekaraLayout(state: KeyboardUiState, theme: ThemeSpec, onGlyph: (String) -> Unit, onShift: () -> Unit) {
    val shifted = state.shiftState != ShiftState.OFF
    Column {
        SINGLISH_ROWS.forEachIndexed { idx, row ->
            Row(Modifier.fillMaxWidth()) {
                if (idx == 2) {
                    KeyButton(if (state.shiftState == ShiftState.CAPS_LOCK) "⇪" else "⇧", theme, Modifier.weight(1.5f)) { onShift() }
                }
                row.forEach { key ->
                    val glyph = WijesekaraEngine.map(key[0], shifted)
                    KeyButton(glyph, theme, Modifier.weight(1f), fontSize = 16.sp) { onGlyph(glyph) }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            KeyButton("්", theme, Modifier.weight(1f)) { onGlyph(WijesekaraEngine.HAL_KIRIMA) }
            KeyButton("්‍ය", theme, Modifier.weight(1f)) { onGlyph(WijesekaraEngine.YANSAYA) }
            KeyButton("්‍ර", theme, Modifier.weight(1f)) { onGlyph(WijesekaraEngine.RAKARANSAYA) }
            KeyButton("ර්‍", theme, Modifier.weight(1f)) { onGlyph(WijesekaraEngine.REPHAYA) }
        }
    }
}

@Composable
private fun SymbolsLayout(theme: ThemeSpec, onKey: (Char) -> Unit) {
    val symbolRows = listOf(
        "1 2 3 4 5 6 7 8 9 0".split(" "),
        "@ # ₹ & * - + ( ) /".split(" "),
        "! \" ' : ; , . ? ~".split(" ")
    )
    val diacritics = listOf("ං", "ඃ", "්", "ා", "ැ", "ෑ", "ි", "ී", "ු", "ූ", "ෘ", "ෙ", "ේ", "ො", "ෝ", "ෞ")
    Column {
        symbolRows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { key -> KeyButton(key, theme, Modifier.weight(1f)) { onKey(key[0]) } }
            }
        }
        Text(
            "Sinhala Diacritics",
            color = Color(theme.keyText),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.height(96.dp)
        ) {
            items(diacritics) { d ->
                KeyButton(d, theme, Modifier.padding(2.dp)) { onKey(d[0]) }
            }
        }
    }
}

@Composable
private fun EmojiPhrasesLayout(theme: ThemeSpec, onGlyph: (String) -> Unit) {
    Column {
        Text("Quick Sinhala Phrases", color = Color(theme.keyText), fontSize = 12.sp, modifier = Modifier.padding(6.dp))
        com.dinushlakmal.lakaboard.engine.SinglishTransliterationEngine.QUICK_PHRASES.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth()) {
                pair.forEach { (label, phrase) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                            .clickable { onGlyph(phrase) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(theme.keyBackground)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(phrase, color = Color(theme.keyText), fontSize = 15.sp)
                            Text(label, color = Color(theme.keyText).copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Emoji", color = Color(theme.keyText), fontSize = 12.sp, modifier = Modifier.padding(6.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(8), modifier = Modifier.height(100.dp)) {
            items(EMOJI_GRID) { emoji ->
                KeyButton(emoji, theme, Modifier.padding(2.dp)) { onGlyph(emoji) }
            }
        }
    }
}

@Composable
private fun ClipboardLayout(state: KeyboardUiState, viewModel: KeyboardViewModel) {
    val theme = state.theme
    Column(Modifier.height(180.dp)) {
        if (state.clipboardHistory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Copied text will appear here", color = Color(theme.keyText).copy(alpha = 0.6f))
            }
        } else {
            state.clipboardHistory.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .background(Color(theme.keyBackground), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        item.text.take(40),
                        color = Color(theme.keyText),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.pasteClipboardItem(item.id) }
                    )
                    Row {
                        IconButton(onClick = { viewModel.togglePinClipboardItem(item.id) }) {
                            Text(if (item.pinned) "📌" else "📍", fontSize = 14.sp)
                        }
                        IconButton(onClick = { viewModel.deleteClipboardItem(item.id) }) {
                            Icon(Icons.Filled.Backspace, contentDescription = "Delete", tint = Color(theme.keyText))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomRow(
    state: KeyboardUiState,
    theme: ThemeSpec,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onModeChange: (KeyboardMode) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        KeyButton(
            if (state.mode == KeyboardMode.SINGLISH) "En" else "සිං",
            theme,
            Modifier.weight(1.2f)
        ) { onModeChange(if (state.mode == KeyboardMode.SINGLISH) KeyboardMode.ENGLISH else KeyboardMode.SINGLISH) }

        KeyButton("Wij", theme, Modifier.weight(1f)) { onModeChange(KeyboardMode.WIJESEKARA) }
        KeyButton("?123", theme, Modifier.weight(1f)) { onModeChange(KeyboardMode.SYMBOLS) }

        Surface(
            modifier = Modifier
                .weight(3f)
                .padding(2.dp)
                .height(46.dp)
                .clickable { onSpace() },
            shape = RoundedCornerShape(8.dp),
            color = Color(theme.keyBackground)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Space", color = Color(theme.keyText), fontSize = 12.sp)
            }
        }

        IconButton(onClick = { onModeChange(KeyboardMode.EMOJI_PHRASES) }) {
            Icon(Icons.Filled.Mood, contentDescription = "Emoji", tint = Color(theme.keyText))
        }
        IconButton(onClick = { onModeChange(KeyboardMode.CLIPBOARD) }) {
            Icon(Icons.Filled.ContentPaste, contentDescription = "Clipboard", tint = Color(theme.keyText))
        }
        IconButton(onClick = onBackspace) {
            Icon(Icons.Filled.Backspace, contentDescription = "Backspace", tint = Color(theme.keyText))
        }
        IconButton(onClick = onEnter) {
            Icon(Icons.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color(theme.keyText))
        }
    }
}
