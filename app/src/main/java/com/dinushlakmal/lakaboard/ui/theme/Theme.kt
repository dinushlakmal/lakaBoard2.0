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
    surface = Color(0xFF16161F),
    surfaceVariant = Color(0xFF222232),
    onPrimary = Color(0xFF1E1400),
    onSurface = Color(0xFFEDE8E3),
    onBackground = Color(0xFFEDE8E3)
)

private val LakaLightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF10B981),
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    onPrimary = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    onBackground = Color(0xFF0F172A)
)

@Composable
fun LakaBoardTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) LakaDarkColors else LakaLightColors,
        content = content
    )
}
