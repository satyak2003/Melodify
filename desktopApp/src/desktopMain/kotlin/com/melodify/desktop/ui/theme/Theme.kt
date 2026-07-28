package com.melodify.desktop.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MelodifyDarkColors = darkColorScheme(
    primary = Color(0xFFA875FF),            // Bright electric purple
    onPrimary = Color(0xFF180038),
    primaryContainer = Color(0xFF3F1975),    // Deep rich purple container
    onPrimaryContainer = Color(0xFFF1E6FF),   // High contrast container text
    secondary = Color(0xFF00F0D2),          // Vibrant cyan accent
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF004D43),
    onSecondaryContainer = Color(0xFFA3FFEB),
    tertiary = Color(0xFFFF6B81),           // Neon coral pink
    onTertiary = Color(0xFF3B000B),
    background = Color(0xFF0F0E17),         // Rich obsidian dark
    onBackground = Color(0xFFF8F7FF),       // Crisp high-contrast off-white
    surface = Color(0xFF161524),            // Elevated dark surface
    onSurface = Color(0xFFF3F2F8),          // High contrast bright white
    surfaceVariant = Color(0xFF242338),     // Distinct card surface
    onSurfaceVariant = Color(0xFFBAC0DA),   // Crisp readable slate gray
    surfaceContainer = Color(0xFF1E1D30),
    outline = Color(0xFF4A4866),
    outlineVariant = Color(0xFF323048),
    error = Color(0xFFFF5252),
    onError = Color(0xFF3B0000)
)

@Composable
fun MelodifyDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MelodifyDarkColors,
        content = content
    )
}

