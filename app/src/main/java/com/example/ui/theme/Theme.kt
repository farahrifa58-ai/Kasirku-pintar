package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0D253F),
    primaryContainer = NavyVariant,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = AccentEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0F3B2C),
    onSecondaryContainer = Color(0xFF98FB98),
    tertiary = AccentAmber,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkBorder,
    error = AccentCoral
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7F5),
    onPrimaryContainer = NavyPrimary,
    secondary = AccentEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F8E7),
    onSecondaryContainer = Color(0xFF004D2C),
    tertiary = AccentAmber,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = LightBorder,
    error = AccentCoral
)

@Composable
fun KasirProTheme(
    darkTheme: Boolean = false, // Initial theme is Light as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
