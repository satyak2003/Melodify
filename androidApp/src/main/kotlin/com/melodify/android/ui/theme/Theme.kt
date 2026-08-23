package com.melodify.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA875FF),            // Bright electric purple
    onPrimary = Color(0xFF180038),
    primaryContainer = Color(0xFF282828),    // Dark gray container
    onPrimaryContainer = Color(0xFFF1E6FF),   // High contrast container text
    secondary = Color(0xFF00F0D2),          // Vibrant cyan accent
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF202020),
    onSecondaryContainer = Color(0xFFA3FFEB),
    tertiary = Color(0xFFFF6B81),           // Neon coral pink
    onTertiary = Color(0xFF3B000B),
    background = Color(0xFF000000),         // Pure black background
    onBackground = Color(0xFFFFFFFF),       // Crisp high-contrast off-white
    surface = Color(0xFF121212),            // Solid dark surface
    onSurface = Color(0xFFFFFFFF),          // High contrast bright white
    surfaceVariant = Color(0xFF1E1E1E),     // Solid dark gray
    onSurfaceVariant = Color(0xFFAAAAAA),   // Crisp readable slate gray
    surfaceContainer = Color(0xFF1A1A1A),
    outline = Color(0xFF444444),
    outlineVariant = Color(0xFF333333),
    error = Color(0xFFFF5252),
    onError = Color(0xFF3B0000)
)

val MelodifyTypography = Typography(
    // Use default Material3 typography â€” looks great out of the box
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium),
)

@Composable
fun MelodifyTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = when {
        darkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Dynamic color on Android 12+
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        else -> DarkColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MelodifyTypography,
        content = content
    )
}
