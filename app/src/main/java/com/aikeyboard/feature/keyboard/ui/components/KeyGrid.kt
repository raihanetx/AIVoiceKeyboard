package com.aikeyboard.feature.keyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.KeyboardReturn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.core.theme.KbType
import com.aikeyboard.feature.keyboard.data.source.KeyboardLayouts
import com.aikeyboard.feature.keyboard.domain.model.KeyboardLanguage
import com.aikeyboard.feature.keyboard.domain.model.KeyboardMode
import kotlinx.coroutines.*

/** Full keyboard key grid: EN / BN / BN_SHIFT / NUM layouts with correct flex weights. */
@Composable
fun KeyGrid(
    language     : KeyboardLanguage,
    mode         : KeyboardMode,
    isCaps       : Boolean,
    onChar       : (String) -> Unit,
    onBackspace  : () -> Unit,
    onShift      : () -> Unit,
    onModeToggle : () -> Unit,
    onComma      : () -> Unit,
    onPeriod     : () -> Unit,
    onSpace      : () -> Unit,
    onEnter      : () -> Unit,
    modifier     : Modifier = Modifier,
) {
    val layout: List<List<String>> = when {
        mode == KeyboardMode.NUM                   -> KeyboardLayouts.num
        language == KeyboardLanguage.BN && isCaps  -> KeyboardLayouts.bnShift
        language == KeyboardLanguage.BN            -> KeyboardLayouts.bn
        else                                       -> KeyboardLayouts.en
    }

    Column(
        modifier = modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        layout.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .then(if (rowIndex == 1) Modifier.padding(horizontal = 10.dp) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    when (key) {
                        "⌫"  -> BackspaceKey(onBackspace = onBackspace)
                        "⇧"  -> ShiftKey(isCaps = isCaps, onShift = onShift)
                        else -> {
                            val ch = if (isCaps && mode == KeyboardMode.ALPHA && language == KeyboardLanguage.EN)
                                key.uppercase() else key
                            CharKey(label = ch, onClick = { onChar(ch) })
                        }
                    }
                }
            }
        }

        // Bottom row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpecialKey(label = if (mode == KeyboardMode.NUM) "ABC" else "?123", flex = 1.3f, onClick = onModeToggle)
            SpecialKey(label = ",", flex = 1f, onClick = onComma)
            SpaceKey(label = if (language == KeyboardLanguage.EN) "English" else "বাংলা", onClick = onSpace)
            SpecialKey(label = ".", flex = 1f, onClick = onPeriod)
            EnterKey(onClick = onEnter)
        }
    }
}

// ── Key atoms ─────────────────────────────────────────────────────────────────

@Composable
private fun RowScope.CharKey(label: String, onClick: () -> Unit) {
    val colors = KbTheme.colors
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.weight(1f).height(42.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(if (pressed) colors.specialBg else colors.keyBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap   = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) { Text(text = label, style = KbType.keyLabel, color = colors.keyText, textAlign = TextAlign.Center) }
}

@Composable
private fun RowScope.SpecialKey(label: String, flex: Float, onClick: () -> Unit) {
    val colors = KbTheme.colors
    Box(
        modifier = Modifier.weight(flex).height(42.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(colors.specialBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) { Text(text = label, style = KbType.specialKeyLabel, color = colors.keyText) }
}

@Composable
private fun RowScope.ShiftKey(isCaps: Boolean, onShift: () -> Unit) {
    val colors = KbTheme.colors
    Box(
        modifier = Modifier.weight(1.6f).height(42.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(colors.specialBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { onShift() }) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = Icons.Outlined.ArrowUpward, contentDescription = "Shift",
             tint = if (isCaps) colors.accent else colors.specialIcon, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RowScope.BackspaceKey(onBackspace: () -> Unit) {
    val colors    = KbTheme.colors
    val scope     = rememberCoroutineScope()
    var deleteJob by remember { mutableStateOf<Job?>(null) }
    Box(
        modifier = Modifier.weight(1.6f).height(42.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(colors.specialBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onBackspace()
                        deleteJob = scope.launch {
                            delay(400); while (isActive) { onBackspace(); delay(50) }
                        }
                        tryAwaitRelease()
                        deleteJob?.cancel(); deleteJob = null
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) { Text(text = "⌫", style = KbType.keyLabel, color = colors.specialIcon) }
}

@Composable
private fun RowScope.SpaceKey(label: String, onClick: () -> Unit) {
    val colors = KbTheme.colors
    Box(
        modifier = Modifier.weight(5f).height(42.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(colors.keyBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) { Text(text = label, style = KbType.spaceLabel, color = colors.keyIcon) }
}

@Composable
private fun RowScope.EnterKey(onClick: () -> Unit) {
    val colors = KbTheme.colors
    Box(
        modifier = Modifier.weight(1.6f).height(42.dp)
            .shadow(1.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(colors.accent)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = Icons.Outlined.KeyboardReturn, contentDescription = "Enter",
             tint = colors.accentText, modifier = Modifier.size(20.dp))
    }
}
