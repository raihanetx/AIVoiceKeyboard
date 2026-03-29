package com.aikeyboard.feature.keyboard.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.core.theme.KbType

/** Animated 3-word suggestion bar with vertical dividers between words. */
@Composable
fun SuggestionBar(
    suggestions : List<String>,
    onWordClick : (String) -> Unit,
    modifier    : Modifier = Modifier,
) {
    val colors  = KbTheme.colors
    val visible = suggestions.any { it.isNotEmpty() }

    AnimatedVisibility(
        visible  = visible,
        enter    = expandVertically() + fadeIn(),
        exit     = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Column {
            Divider(color = colors.divider, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.kbSurface)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                suggestions.forEachIndexed { index, word ->
                    SuggestionWord(
                        word      = word,
                        alignment = when (index) { 0 -> TextAlign.Start; 1 -> TextAlign.Center; else -> TextAlign.End },
                        onClick   = { if (word.isNotEmpty()) onWordClick(word) },
                        modifier  = Modifier.weight(1f),
                    )
                    if (index < suggestions.size - 1) {
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(colors.divider))
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionWord(word: String, alignment: TextAlign, onClick: () -> Unit, modifier: Modifier) {
    val colors            = KbTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(interactionSource = interactionSource,
                       indication = rememberRipple(color = colors.ripple),
                       onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = when (alignment) {
            TextAlign.Start -> Alignment.CenterStart
            TextAlign.End   -> Alignment.CenterEnd
            else            -> Alignment.Center
        },
    ) {
        Text(text = word, style = KbType.suggestion, color = colors.keyText,
             textAlign = alignment, maxLines = 1)
    }
}
