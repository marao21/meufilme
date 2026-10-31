package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvColorScheme = darkColorScheme(
    primary = TvPrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = TvPrimaryLight,
    secondary = TvAccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0E7490),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = TvAccentPink,
    onTertiary = Color.White,
    background = TvMidnight,
    onBackground = TvTextPrimary,
    surface = TvDeepSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvCardBg,
    onSurfaceVariant = TvTextSecondary,
    outline = TvTextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // TV displays always look best in dark cinematic mode
    dynamicColor: Boolean = false, // Keep consistent high-contrast colors for TV remote visibility
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvColorScheme,
        typography = Typography,
        content = content
    )
}
