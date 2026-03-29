package com.aikeyboard.core.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Raw color palette  (exact hex values from original HTML)
// ─────────────────────────────────────────────────────────────────────────────
object Palette {
    // Light
    val AppBgLight      = Color(0xFFF2F4F8)
    val KbSurfaceLight  = Color(0xFFE8EAED)
    val KeyBgLight      = Color(0xFFFFFFFF)
    val KeyTextLight    = Color(0xFF202124)
    val KeyIconLight    = Color(0xFF5F6368)
    val SpecialBgLight  = Color(0xFFDADCE0)
    val AccentLight     = Color(0xFF1A73E8)
    val AccentSoftLight = Color(0xFFE8F0FE)
    val AccentTextLight = Color(0xFFFFFFFF)
    val DividerLight    = Color(0x1A000000)
    val RippleLight     = Color(0x0F000000)

    // Dark
    val AppBgDark      = Color(0xFF121212)
    val KbSurfaceDark  = Color(0xFF1E1E1E)
    val KeyBgDark      = Color(0xFF2D2D2D)
    val KeyTextDark    = Color(0xFFE8EAED)
    val KeyIconDark    = Color(0xFF9AA0A6)
    val SpecialBgDark  = Color(0xFF3C3C3C)
    val AccentDark     = Color(0xFF8AB4F8)
    val AccentSoftDark = Color(0xFF2D3A4F)
    val AccentTextDark = Color(0xFF000000)
    val DividerDark    = Color(0x26FFFFFF)
    val RippleDark     = Color(0x0FFFFFFF)

    // Shared
    val ErrorRed = Color(0xFFEA4335)
    val White    = Color(0xFFFFFFFF)
}

// ─────────────────────────────────────────────────────────────────────────────
// Semantic color set (consumed everywhere via KbTheme.colors)
// ─────────────────────────────────────────────────────────────────────────────
@Stable
data class KeyboardColors(
    val appBg       : Color,
    val kbSurface   : Color,
    val keyBg       : Color,
    val keyText     : Color,
    val keyIcon     : Color,
    val specialBg   : Color,
    val specialIcon : Color,
    val accent      : Color,
    val accentSoft  : Color,
    val accentText  : Color,
    val divider     : Color,
    val ripple      : Color,
)

val LightKeyboardColors = KeyboardColors(
    appBg       = Palette.AppBgLight,
    kbSurface   = Palette.KbSurfaceLight,
    keyBg       = Palette.KeyBgLight,
    keyText     = Palette.KeyTextLight,
    keyIcon     = Palette.KeyIconLight,
    specialBg   = Palette.SpecialBgLight,
    specialIcon = Palette.KeyTextLight,
    accent      = Palette.AccentLight,
    accentSoft  = Palette.AccentSoftLight,
    accentText  = Palette.AccentTextLight,
    divider     = Palette.DividerLight,
    ripple      = Palette.RippleLight,
)

val DarkKeyboardColors = KeyboardColors(
    appBg       = Palette.AppBgDark,
    kbSurface   = Palette.KbSurfaceDark,
    keyBg       = Palette.KeyBgDark,
    keyText     = Palette.KeyTextDark,
    keyIcon     = Palette.KeyIconDark,
    specialBg   = Palette.SpecialBgDark,
    specialIcon = Palette.KeyTextDark,
    accent      = Palette.AccentDark,
    accentSoft  = Palette.AccentSoftDark,
    accentText  = Palette.AccentTextDark,
    divider     = Palette.DividerDark,
    ripple      = Palette.RippleDark,
)

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal — access colors anywhere with KbTheme.colors
// ─────────────────────────────────────────────────────────────────────────────
val LocalKeyboardColors = staticCompositionLocalOf { LightKeyboardColors }

object KbTheme {
    val colors: KeyboardColors
        @Composable @ReadOnlyComposable
        get() = LocalKeyboardColors.current
}

// ─────────────────────────────────────────────────────────────────────────────
// Typography tokens
// ─────────────────────────────────────────────────────────────────────────────
object KbType {
    val keyLabel         = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal)
    val specialKeyLabel  = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    val spaceLabel       = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val suggestion       = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
    val overlayTitle     = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val tileText         = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val tileSub          = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val groupTitle       = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val navBack          = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    val voiceLang        = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}

// ─────────────────────────────────────────────────────────────────────────────
// Theme wrapper composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PixelProKeyboardTheme(
    isDark  : Boolean = false,
    content : @Composable () -> Unit,
) {
    val colors = if (isDark) DarkKeyboardColors else LightKeyboardColors
    CompositionLocalProvider(LocalKeyboardColors provides colors, content = content)
}
