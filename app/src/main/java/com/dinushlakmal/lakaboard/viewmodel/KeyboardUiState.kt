package com.dinushlakmal.lakaboard.viewmodel

import com.dinushlakmal.lakaboard.audio.SoundProfile

enum class KeyboardMode { SINGLISH, WIJESEKARA, ENGLISH, SYMBOLS, EMOJI_PHRASES, CLIPBOARD, THEMES, GUIDE }

enum class ShiftState { OFF, SHIFT_ONCE, CAPS_LOCK }

data class ThemeSpec(
    val id: String,
    val name: String,
    val keyBackground: Long,
    val keyBackgroundPressed: Long,
    val keyText: Long,
    val keyboardBackground: Long,
    val accent: Long,
    val isDark: Boolean,
    val backgroundImageUri: String? = null,
    val backgroundBlur: Float = 0f,
    val keyOpacity: Float = 1f
)

data class ClipboardItem(
    val id: String,
    val text: String,
    val pinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class KeyboardUiState(
    val mode: KeyboardMode = KeyboardMode.SINGLISH,
    val shiftState: ShiftState = ShiftState.OFF,
    val currentWordBuffer: String = "",
    val suggestions: List<String> = emptyList(),
    val committedText: String = "",
    val theme: ThemeSpec = BuiltInThemes.CYBER_DARK,
    val soundProfile: SoundProfile = SoundProfile.MECHANICAL_CLICK,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val clipboardHistory: List<ClipboardItem> = emptyList(),
    val charCount: Int = 0,
    val wordCount: Int = 0,
    val showThemeCustomizer: Boolean = false,
    val showTransliterationGuide: Boolean = false
)

object BuiltInThemes {
    val CYBER_DARK = ThemeSpec(
        id = "cyber_dark", name = "Cyber Dark",
        keyBackground = 0xFF16161FL, keyBackgroundPressed = 0xFF2A2A3DL,
        keyText = 0xFFF5D67FL, keyboardBackground = 0xFF0B0B12L,
        accent = 0xFFF5C34DL, isDark = true
    )
    val EMERALD_GOLD = ThemeSpec(
        id = "emerald_gold", name = "Emerald Gold",
        keyBackground = 0xFF0F2A22L, keyBackgroundPressed = 0xFF1B4636L,
        keyText = 0xFFE9C46AL, keyboardBackground = 0xFF08211AL,
        accent = 0xFFE9C46AL, isDark = true
    )
    val NEON_SUNSET = ThemeSpec(
        id = "neon_sunset", name = "Neon Sunset",
        keyBackground = 0xFF241436L, keyBackgroundPressed = 0xFF3B1E57L,
        keyText = 0xFFFF6FA5L, keyboardBackground = 0xFF160B26L,
        accent = 0xFFFF9E4FL, isDark = true
    )
    val AMOLED_BLACK = ThemeSpec(
        id = "amoled_black", name = "AMOLED Pure Black",
        keyBackground = 0xFF000000L, keyBackgroundPressed = 0xFF1C1C1CL,
        keyText = 0xFFFFFFFFL, keyboardBackground = 0xFF000000L,
        accent = 0xFF3DDC84L, isDark = true
    )
    val ARCTIC_LIGHT = ThemeSpec(
        id = "arctic_light", name = "Arctic Light",
        keyBackground = 0xFFFFFFFFL, keyBackgroundPressed = 0xFFE2ECF5L,
        keyText = 0xFF1B2733L, keyboardBackground = 0xFFEFF4F8L,
        accent = 0xFF3B82F6L, isDark = false
    )

    val ALL = listOf(CYBER_DARK, EMERALD_GOLD, NEON_SUNSET, AMOLED_BLACK, ARCTIC_LIGHT)
}
