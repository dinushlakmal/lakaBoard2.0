package com.dinushlakmal.lakaboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinushlakmal.lakaboard.audio.SoundHapticHelper
import com.dinushlakmal.lakaboard.engine.SinglishTransliterationEngine
import com.dinushlakmal.lakaboard.engine.WijesekaraEngine
import com.dinushlakmal.lakaboard.viewmodel.*

private val SINGLISH_ROWS = listOf(
    "q w e r t y u i o p".split(" "),
    "a s d f g h j k l".split(" "),
    "z x c v b n m".split(" ")
)

private val EMOJI_GRID = listOf(
    "😀", "😂", "🥰", "😎", "🙏", "👍", "❤️", "🔥", "🎉", "😢",
    "😅", "🤔", "😴", "🙌", "👏", "💯", "✨", "🌸", "🍚", "☕",
    "🇱🇰", "🐘", "🪷", "🇱🇰", "🤝", "🎉", "👌", "💡", "💪", "🌟"
)

private data class InlineGuideRow(val rule: String, val type: String, val example: String)

private val INLINE_GUIDE_ROWS = listOf(
    InlineGuideRow("k, g, c, j, t, d, n, p, b, m, y, r, l, v, s, h", "Base Consonants", "ka → ක, ga → ග, ma → ම"),
    InlineGuideRow("+ hal kirima (end of syllable)", "Hal Kirima (Pure)", "k → ක්, n → න්, l → ල්"),
    InlineGuideRow("+ aa / A", "Alapilla (Long aa)", "kaa → කා, mA → මා"),
    InlineGuideRow("+ i / ii / I", "I-pilla Family", "ki → කි, kii → කී"),
    InlineGuideRow("+ u / uu / U / oo", "U-pilla Family", "ku → කු, koo → කූ"),
    InlineGuideRow("+ e / E / ee", "Kombuva Family", "ke → කෙ, kE → කේ"),
    InlineGuideRow("+ o / O / oo", "Kombuva + Aela", "ko → කො, kO → කෝ"),
    InlineGuideRow("+ ai / au", "Diphthongs", "kai → කයි, kau → කෞ"),
    InlineGuideRow("+ y + vowel", "Yansaya", "kya → ක්‍ය, pya → ප්‍ය"),
    InlineGuideRow("+ r + vowel", "Rakaransaya", "kra → ක්‍ර, pra → ප්‍ර"),
    InlineGuideRow("nd, mb, ng, ngg, ndh", "Bandi Akuru", "kanda → කන්ඬ, ganga → ගඟ, lanka → ලංකා"),
    InlineGuideRow("T, D, N, L (Capital)", "Retroflex", "Tikak → ටිකක්, Lassanai → ලස්සනයි"),
    InlineGuideRow("sh / Sh / SH", "Sibilants", "sha → ශ, Sha → ෂ")
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
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("lakaboard_keyboard_container")
    ) {
        TopBar(
            state = state,
            viewModel = viewModel,
            onCloseSpecial = { viewModel.switchMode(KeyboardMode.SINGLISH) },
            onRequestHideKeyboard = onRequestHideKeyboard
        )

        if (state.mode == KeyboardMode.SINGLISH && state.suggestions.isNotEmpty()) {
            SuggestionBar(state.suggestions) {
                press()
                viewModel.selectSuggestion(it)
            }
        }

        Spacer(Modifier.height(4.dp))

        when (state.mode) {
            KeyboardMode.SINGLISH -> SinglishLayout(theme, onKey = { press(); viewModel.onCharacterKey(it) })
            KeyboardMode.WIJESEKARA -> WijesekaraLayout(state, theme, onGlyph = { press(); viewModel.commitGlyph(it) }, onShift = { press(); viewModel.cycleShift() })
            KeyboardMode.ENGLISH -> EnglishLayout(state, theme, onKey = { press(); viewModel.onCharacterKey(it) }, onShift = { press(); viewModel.cycleShift() })
            KeyboardMode.SYMBOLS -> SymbolsLayout(theme, onKey = { press(); viewModel.onCharacterKey(it) })
            KeyboardMode.EMOJI_PHRASES -> EmojiPhrasesLayout(theme, onGlyph = { press(); viewModel.commitGlyph(it) })
            KeyboardMode.CLIPBOARD -> ClipboardLayout(state, viewModel)
            KeyboardMode.THEMES -> InlineThemeSelector(current = state.theme, onSelect = { viewModel.setTheme(it) }, onDone = { viewModel.switchMode(KeyboardMode.SINGLISH) })
            KeyboardMode.GUIDE -> InlineGuideLayout(theme = theme, onDone = { viewModel.switchMode(KeyboardMode.SINGLISH) })
        }

        if (state.mode != KeyboardMode.THEMES && state.mode != KeyboardMode.GUIDE) {
            Spacer(Modifier.height(4.dp))
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
}

@Composable
private fun TopBar(
    state: KeyboardUiState,
    viewModel: KeyboardViewModel,
    onCloseSpecial: () -> Unit,
    onRequestHideKeyboard: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "LakaBoard",
                color = Color(state.theme.accent),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(state.theme.keyBackgroundPressed)
            ) {
                Text(
                    text = when (state.mode) {
                        KeyboardMode.SINGLISH -> "Singlish"
                        KeyboardMode.WIJESEKARA -> "Wijesekara"
                        KeyboardMode.ENGLISH -> "English"
                        KeyboardMode.SYMBOLS -> "Symbols"
                        KeyboardMode.EMOJI_PHRASES -> "Phrases & Emoji"
                        KeyboardMode.CLIPBOARD -> "Clipboard"
                        KeyboardMode.THEMES -> "Themes"
                        KeyboardMode.GUIDE -> "Singlish Guide"
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color(state.theme.keyText).copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Row {
            IconButton(
                onClick = onRequestHideKeyboard,
                modifier = Modifier.size(36.dp).testTag("hide_keyboard_button")
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Hide Keyboard",
                    tint = Color(state.theme.keyText).copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            if (state.mode == KeyboardMode.THEMES || state.mode == KeyboardMode.GUIDE) {
                IconButton(
                    onClick = onCloseSpecial,
                    modifier = Modifier.size(36.dp).testTag("close_special_mode_button")
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color(state.theme.keyText),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = { viewModel.switchMode(KeyboardMode.GUIDE) },
                    modifier = Modifier.size(36.dp).testTag("guide_icon_button")
                ) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = "Transliteration Guide",
                        tint = Color(state.theme.keyText),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.switchMode(KeyboardMode.THEMES) },
                    modifier = Modifier.size(36.dp).testTag("theme_icon_button")
                ) {
                    Icon(
                        Icons.Filled.Palette,
                        contentDescription = "Theme Customizer",
                        tint = Color(state.theme.keyText),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineThemeSelector(current: ThemeSpec, onSelect: (ThemeSpec) -> Unit, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Select Theme Style", color = Color(current.keyText), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            FilledTonalButton(
                onClick = onDone,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text("Done", fontSize = 12.sp)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(BuiltInThemes.ALL) { theme ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(theme.keyboardBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (theme.id == current.id) 2.dp else 1.dp,
                            color = if (theme.id == current.id) Color(theme.accent) else Color(theme.keyBackgroundPressed),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(theme) }
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(theme.accent)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(theme.keyBackground))
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(theme.name, color = Color(theme.keyText), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (theme.id == current.id) {
                            Icon(Icons.Filled.Check, contentDescription = "Active", tint = Color(theme.accent), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineGuideLayout(theme: ThemeSpec, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Singlish Phonetic Guide", color = Color(theme.keyText), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            FilledTonalButton(
                onClick = onDone,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text("Done", fontSize = 12.sp)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(INLINE_GUIDE_ROWS) { row ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(theme.keyBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(row.type, color = Color(theme.accent), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Keys: ${row.rule}", color = Color(theme.keyText), fontSize = 11.sp)
                        Text("Example: ${row.example}", color = Color(theme.keyText).copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionBar(suggestions: List<String>, onPick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { candidate ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPick(candidate) }
                    .testTag("suggestion_$candidate")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = candidate,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    theme: ThemeSpec,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 17.sp,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(2.dp)
            .height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(theme.keyBackground).copy(alpha = theme.keyOpacity),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(label, color = Color(theme.keyText), fontSize = fontSize, fontWeight = FontWeight.Medium)
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
                    KeyButton(
                        if (state.shiftState == ShiftState.CAPS_LOCK) "⇪" else "⇧",
                        theme,
                        Modifier.weight(1.4f)
                    ) { onShift() }
                }
                row.forEach { key ->
                    val label = if (shifted) key.uppercase() else key
                    KeyButton(label, theme, Modifier.weight(1f)) { onKey(key[0]) }
                }
                if (idx == 2) {
                    Spacer(Modifier.weight(0.4f))
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
                    KeyButton(
                        if (state.shiftState == ShiftState.CAPS_LOCK) "⇪" else "⇧",
                        theme,
                        Modifier.weight(1.4f)
                    ) { onShift() }
                }
                row.forEach { key ->
                    val glyph = WijesekaraEngine.map(key[0], shifted)
                    KeyButton(glyph, theme, Modifier.weight(1f), fontSize = 15.sp) { onGlyph(glyph) }
                }
                if (idx == 2) {
                    Spacer(Modifier.weight(0.4f))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            KeyButton("් (Hal)", theme, Modifier.weight(1f), fontSize = 13.sp) { onGlyph(WijesekaraEngine.HAL_KIRIMA) }
            KeyButton("්‍ය (Yansaya)", theme, Modifier.weight(1f), fontSize = 13.sp) { onGlyph(WijesekaraEngine.YANSAYA) }
            KeyButton("්‍ර (Rakaransaya)", theme, Modifier.weight(1f), fontSize = 13.sp) { onGlyph(WijesekaraEngine.RAKARANSAYA) }
            KeyButton("ර්‍ (Rephaya)", theme, Modifier.weight(1f), fontSize = 13.sp) { onGlyph(WijesekaraEngine.REPHAYA) }
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
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.height(84.dp)
        ) {
            items(diacritics) { d ->
                KeyButton(d, theme, Modifier.padding(1.dp), fontSize = 16.sp) { onKey(d[0]) }
            }
        }
    }
}

@Composable
private fun EmojiPhrasesLayout(theme: ThemeSpec, onGlyph: (String) -> Unit) {
    Column {
        Text(
            "Quick Sinhala Phrases",
            color = Color(theme.keyText),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
        SinglishTransliterationEngine.QUICK_PHRASES.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth()) {
                pair.forEach { (label, phrase) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .clickable { onGlyph(phrase) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(theme.keyBackground)
                    ) {
                        Column(Modifier.padding(6.dp)) {
                            Text(phrase, color = Color(theme.keyText), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(label, color = Color(theme.keyText).copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Emoji",
            color = Color(theme.keyText),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
        LazyVerticalGrid(columns = GridCells.Fixed(8), modifier = Modifier.height(84.dp)) {
            items(EMOJI_GRID) { emoji ->
                KeyButton(emoji, theme, Modifier.padding(1.dp), fontSize = 18.sp) { onGlyph(emoji) }
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
                Text(
                    "Copied text and typed phrases will appear here",
                    color = Color(theme.keyText).copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.clipboardHistory) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(theme.keyBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                item.text.take(36),
                                color = Color(theme.keyText),
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.pasteClipboardItem(item.id) }
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.togglePinClipboardItem(item.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text(if (item.pinned) "📌" else "📍", fontSize = 13.sp)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteClipboardItem(item.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = Color(theme.keyText).copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
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
            Modifier.weight(1.1f),
            fontSize = 13.sp
        ) { onModeChange(if (state.mode == KeyboardMode.SINGLISH) KeyboardMode.ENGLISH else KeyboardMode.SINGLISH) }

        KeyButton("Wij", theme, Modifier.weight(0.9f), fontSize = 12.sp) { onModeChange(KeyboardMode.WIJESEKARA) }
        KeyButton("?123", theme, Modifier.weight(0.9f), fontSize = 12.sp) { onModeChange(KeyboardMode.SYMBOLS) }

        Surface(
            modifier = Modifier
                .weight(2.8f)
                .padding(2.dp)
                .height(44.dp)
                .clickable { onSpace() }
                .testTag("space_key_button"),
            shape = RoundedCornerShape(8.dp),
            color = Color(theme.keyBackground)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Space", color = Color(theme.keyText), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        IconButton(
            onClick = { onModeChange(KeyboardMode.EMOJI_PHRASES) },
            modifier = Modifier.size(38.dp).testTag("emoji_mode_button")
        ) {
            Icon(Icons.Filled.Mood, contentDescription = "Emoji", tint = Color(theme.keyText), modifier = Modifier.size(20.dp))
        }
        IconButton(
            onClick = { onModeChange(KeyboardMode.CLIPBOARD) },
            modifier = Modifier.size(38.dp).testTag("clipboard_mode_button")
        ) {
            Icon(Icons.Filled.ContentPaste, contentDescription = "Clipboard", tint = Color(theme.keyText), modifier = Modifier.size(20.dp))
        }
        IconButton(
            onClick = onBackspace,
            modifier = Modifier.size(38.dp).testTag("backspace_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = Color(theme.keyText), modifier = Modifier.size(20.dp))
        }
        IconButton(
            onClick = onEnter,
            modifier = Modifier.size(38.dp).testTag("enter_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardReturn, contentDescription = "Enter", tint = Color(theme.accent), modifier = Modifier.size(20.dp))
        }
    }
}
