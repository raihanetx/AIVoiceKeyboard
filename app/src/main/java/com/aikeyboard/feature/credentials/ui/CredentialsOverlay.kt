package com.aikeyboard.feature.credentials.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.core.theme.KbType
import com.aikeyboard.feature.credentials.domain.model.CredIconType
import com.aikeyboard.feature.credentials.domain.model.CredentialEntry
import com.aikeyboard.feature.shared.ui.ListTile
import com.aikeyboard.feature.shared.ui.OverlayHeader

@Composable
fun CredentialsOverlay(
    items              : List<CredentialEntry>,
    onBack             : () -> Unit,
    onPaste            : (String) -> Unit,
    showAddDialog      : Boolean,
    newCredentialName  : String,
    newCredentialValue : String,
    onShowAddDialog    : () -> Unit,
    onHideAddDialog    : () -> Unit,
    onNameChange       : (String) -> Unit,
    onValueChange      : (String) -> Unit,
    onSaveCredential   : () -> Unit,
    modifier           : Modifier = Modifier,
) {
    val colors = KbTheme.colors
    
    Column(modifier = modifier.fillMaxSize().background(colors.kbSurface)) {
        // Header with Add button
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "← Back", style = KbType.navBack, color = colors.accent)
            }
            
            Spacer(Modifier.weight(1f))
            
            Text(text = "Credentials", style = KbType.overlayTitle, color = colors.keyText)
            
            Spacer(Modifier.weight(1f))
            
            // Add button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accent)
                    .clickable(onClick = onShowAddDialog)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add",
                        tint = colors.accentText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = "Add", style = KbType.tileSub, color = colors.accentText)
                }
            }
        }
        
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        
        // Content
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved credentials\nTap 'Add' to save one",
                    style = KbType.tileText,
                    color = colors.keyIcon,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { entry ->
                    ListTile(
                        icon = when (entry.iconType) {
                            CredIconType.PHONE -> Icons.Outlined.Phone
                            CredIconType.EMAIL -> Icons.Outlined.Email
                            else -> Icons.Outlined.Lock
                        },
                        iconTint = colors.accent,
                        iconBg = colors.accentSoft,
                        title = entry.label,
                        subtitle = entry.text.let { if (it.length > 20) "••••••••" else it },
                        onClick = { onPaste(entry.text) },
                    )
                }
            }
        }
        
        // Add Credential Dialog
        if (showAddDialog) {
            AddCredentialDialog(
                name = newCredentialName,
                value = newCredentialValue,
                onNameChange = onNameChange,
                onValueChange = onValueChange,
                onSave = onSaveCredential,
                onDismiss = onHideAddDialog
            )
        }
    }
}

@Composable
private fun AddCredentialDialog(
    name: String,
    value: String,
    onNameChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = KbTheme.colors
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.kbSurface.copy(alpha = 0.95f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.keyBg)
                .padding(20.dp)
                .clickable(enabled = false) { } // Prevent dismiss when clicking inside
        ) {
            Text(
                text = "Add New Credential",
                style = KbType.overlayTitle,
                color = colors.keyText
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Name field
            Text(
                text = "Account Name",
                style = KbType.tileSub,
                color = colors.keyIcon
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.specialBg)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (name.isEmpty()) {
                    Text(
                        text = "e.g., Gmail, Facebook",
                        style = KbType.tileText,
                        color = colors.keyIcon.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    textStyle = KbType.tileText.copy(color = colors.keyText),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Value field
            Text(
                text = "Password / Value",
                style = KbType.tileSub,
                color = colors.keyIcon
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.specialBg)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Enter password or value",
                        style = KbType.tileText,
                        color = colors.keyIcon.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = KbType.tileText.copy(color = colors.keyText),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Cancel",
                    style = KbType.tileSub,
                    color = colors.keyIcon,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(12.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent)
                        .clickable(enabled = name.isNotBlank() && value.isNotBlank(), onClick = onSave)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Save",
                        style = KbType.tileSub,
                        color = colors.accentText
                    )
                }
            }
        }
    }
}
