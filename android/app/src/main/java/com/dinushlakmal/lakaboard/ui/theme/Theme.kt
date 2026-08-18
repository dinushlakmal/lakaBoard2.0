package com.dinushlakmal.lakaboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LakaDarkColors = darkColorScheme(
    primary = Color(0xFFF5C34D),
    secondary = Color(0xFF3DDC84),
    background = Color(0xFF0B0B12),
    surface = Color(0xFF16161F)
)

private val LakaLightColors = lightColorScheme(
    primary = Color(0xFF3B82F6),
    secondary = Color(0xFF10B981),
    background = Color(0xFFEFF4F8),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun LakaBoardTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) LakaDarkColors else LakaLightColors,
        content = content
    )
}
