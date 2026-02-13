package com.telegrammer.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0088CC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E9F7),
    secondary = Color(0xFF4CAF50),
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F0),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF1A3A5C),
    secondary = Color(0xFF81C784),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

@Composable
fun TelegrammerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
