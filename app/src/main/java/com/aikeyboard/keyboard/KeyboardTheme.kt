package com.aikeyboard.keyboard

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dark color scheme for keyboard (always dark theme for keyboard)
private val KeyboardDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4285F4),
    secondary = androidx.compose.ui.graphics.Color(0xFF4285F4),
    tertiary = androidx.compose.ui.graphics.Color(0xFF4285F4),
    background = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
    surface = androidx.compose.ui.graphics.Color(0xFF16213E),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White
)

@Composable
fun KeyboardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KeyboardDarkColorScheme,
        content = content
    )
}
