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
    primary = Color(0xFFBB86FC),          // Vivid purple
    onPrimary = Color(0xFF1A0036),
    primaryContainer = Color(0xFF4A148C),
    onPrimaryContainer = Color(0xFFEDD9FF),
    secondary = Color(0xFF03DAC6),         // Teal accent
    onSecondary = Color(0xFF003733),
    surface = Color(0xFF0D0D1A),           // Deep dark background
    onSurface = Color(0xFFE6E1E5),
    background = Color(0xFF0D0D1A),
    onBackground = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF1E1E3A),
    onSurfaceVariant = Color(0xFFCAC4D0),
    tertiary = Color(0xFFEF9A9A),          // Warm pink accent
)

val MelodifyTypography = Typography(
    // Use default Material3 typography — looks great out of the box
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
