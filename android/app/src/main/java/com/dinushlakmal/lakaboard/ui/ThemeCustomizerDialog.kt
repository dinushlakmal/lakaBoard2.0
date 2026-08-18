package com.dinushlakmal.lakaboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dinushlakmal.lakaboard.viewmodel.BuiltInThemes
import com.dinushlakmal.lakaboard.viewmodel.ThemeSpec

/**
 * ThemeCustomizerDialog
 * ---------------------------------------------------------------------
 * Lets the user pick a built-in preset, or tune background image, blur
 * and key opacity for a custom look. Import/export of FlorisBoard-
 * compatible theme JSON is exposed via [onImportJson] / [onExportJson]
 * hooks so the host Activity can wire real file pickers (SAF) - kept
 * out of this composable to keep it Context-free and previewable.
 */
@Composable
fun ThemeCustomizerDialog(
    current: ThemeSpec,
    onSelect: (ThemeSpec) -> Unit,
    onDismiss: () -> Unit,
    onImportJson: (() -> Unit)? = null,
    onExportJson: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Theme") },
        text = {
            Column {
                Text("Presets", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(BuiltInThemes.ALL) { theme ->
                        ThemeRow(theme = theme, selected = theme.id == current.id) {
                            onSelect(theme)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onImportJson?.invoke() }) { Text("Import JSON") }
                    OutlinedButton(onClick = { onExportJson?.invoke() }) { Text("Export JSON") }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Custom background image, blur and key-opacity controls are " +
                        "available from the full Theme Studio screen (MainActivity).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun ThemeRow(theme: ThemeSpec, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) Color(theme.accent).copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(theme.keyboardBackground))
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(theme.name)
                Text(
                    if (theme.isDark) "Dark" else "Light",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(theme.accent))
        )
    }
}
