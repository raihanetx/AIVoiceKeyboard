package com.aikeyboard.presentation.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme for the application
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color.Primary,
    secondary = Color.Secondary,
    background = Color.Background,
    surface = Color.Surface,
    onPrimary = Color.OnPrimary,
    onBackground = Color.OnBackground,
    onSurface = Color.OnSurface,
    error = Color.Error,
    onError = Color.White
)

/**
 * Main application theme
 * 
 * Use this for Activities that need proper status bar styling.
 */
@Composable
fun AIVoiceKeyboardTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Theme for keyboard service
 * 
 * Safe to use without Activity context. Does not modify window properties.
 */
@Composable
fun KeyboardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
