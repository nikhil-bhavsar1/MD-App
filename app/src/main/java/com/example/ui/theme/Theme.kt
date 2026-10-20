package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = CyberCyan,
    secondary = ElectricViolet,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3B0764),
    onSecondaryContainer = ElectricViolet,
    tertiary = EmeraldGlow,
    onTertiary = Color.Black,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateElevated,
    onSurfaceVariant = TextMuted,
    outline = BorderDark,
    outlineVariant = Color(0xFF131D2E)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
