package com.aikeyboard.feature.clipboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.feature.clipboard.domain.model.ClipIconType
import com.aikeyboard.feature.clipboard.domain.model.ClipboardEntry
import com.aikeyboard.feature.shared.ui.ListTile
import com.aikeyboard.feature.shared.ui.OverlayHeader
import com.aikeyboard.feature.shared.ui.TileActionButton

@Composable
fun ClipboardOverlay(
    items   : List<ClipboardEntry>,
    onBack  : () -> Unit,
    onPaste : (String) -> Unit,
    onSave  : (ClipboardEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KbTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.kbSurface)) {
        OverlayHeader(title = "Clipboard", onBack = onBack)
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.id }) { entry ->
                ListTile(
                    icon     = when (entry.iconType) {
                        ClipIconType.LINK    -> Icons.Outlined.Link
                        ClipIconType.HISTORY -> Icons.Outlined.History
                        else                 -> Icons.Outlined.TextFields
                    },
                    title    = entry.text,
                    subtitle = entry.timeLabel,
                    onClick  = { onPaste(entry.text) },
                ) { TileActionButton(label = "Save", onClick = { onSave(entry) }) }
            }
        }
    }
}
