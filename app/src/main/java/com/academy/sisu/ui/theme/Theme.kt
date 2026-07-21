package com.academy.sisu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Accent2,
    onSecondary = Color.White,
    background = Bg,
    onBackground = TextCol,
    surface = Surface1,
    onSurface = TextCol,
    surfaceVariant = Surface2,
    onSurfaceVariant = MutedCol,
    outline = LineCol,
    outlineVariant = LineSoft,
    error = RedC,
    onError = Color.White,
    scrim = Color(0x8C000000)
)

private val AppTypography = Typography()

@Composable
fun SisuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
