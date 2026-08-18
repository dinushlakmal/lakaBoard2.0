package com.dinushlakmal.lakaboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dinushlakmal.lakaboard.audio.SoundHapticHelper
import com.dinushlakmal.lakaboard.viewmodel.KeyboardViewModel

private enum class SimApp { CHAT, NOTEPAD, SEARCH, EMAIL }

/**
 * MobileFrameSimulator
 * ---------------------------------------------------------------------
 * An in-app "testing studio": a smartphone-shaped frame containing a
 * few simulated apps (Chat, Notepad, Search, Email) with real text
 * fields wired to the same KeyboardViewModel used by the real IME, so
 * users (and QA) can validate typing behaviour without leaving the app.
 */
@Composable
fun MobileFrameSimulator(viewModel: KeyboardViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val soundHapticHelper = remember { SoundHapticHelper(context) }
    var selectedApp by remember { mutableStateOf(SimApp.CHAT) }
    var fieldText by remember { mutableStateOf("") }

    // Wire the simulator's local text field as the "InputConnection" target.
    DisposableEffect(selectedApp) {
        viewModel.onCommitText = { fieldText += it }
        viewModel.onUpdateComposingWord = { rendered ->
            // Replace the trailing "live word" chunk with the new render.
            val lastSpace = fieldText.lastIndexOf(' ')
            val base = if (lastSpace >= 0) fieldText.substring(0, lastSpace + 1) else ""
            fieldText = base + rendered
        }
        viewModel.onFinishComposing = { /* no-op in simulator: text already committed */ }
        viewModel.onDeleteBackward = { if (fieldText.isNotEmpty()) fieldText = fieldText.dropLast(1) }
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF111318))
            .padding(10.dp)
    ) {
        // Simulated status/top bar
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${fieldText.length} chars • ${fieldText.trim().split(" ").filter { it.isNotBlank() }.size} words",
                color = Color.White, style = MaterialTheme.typography.labelSmall
            )
            Row {
                TextButton(onClick = { /* copy intent */ }) { Text("Copy", color = Color.White) }
                TextButton(onClick = { /* share intent */ }) { Text("Share", color = Color.White) }
            }
        }

        // App tabs
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SimApp.values().forEach { app ->
                FilterChip(
                    selected = selectedApp == app,
                    onClick = { selectedApp = app; fieldText = "" },
                    label = { Text(app.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(12.dp)
        ) {
            when (selectedApp) {
                SimApp.CHAT -> ChatBubblePreview(fieldText)
                SimApp.NOTEPAD -> Text(fieldText.ifEmpty { "Start writing your Sinhala note..." })
                SimApp.SEARCH -> SearchBarPreview(fieldText)
                SimApp.EMAIL -> EmailComposerPreview(fieldText)
            }
        }

        Spacer(Modifier.height(8.dp))
        LakaBoardKeyboard(
            viewModel = viewModel,
            soundHapticHelper = soundHapticHelper,
            onRequestHideKeyboard = { }
        )
    }
}

@Composable
private fun ChatBubblePreview(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp))
            .background(Color(0xFFDCF8C6))
            .padding(10.dp)
    ) {
        Text(text.ifEmpty { "Type a message…" })
    }
}

@Composable
private fun SearchBarPreview(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🔍", modifier = Modifier.padding(end = 8.dp))
        Text(text.ifEmpty { "Search Sinhala or English…" })
    }
}

@Composable
private fun EmailComposerPreview(text: String) {
    Column {
        Text("To: someone@example.com", style = MaterialTheme.typography.labelSmall)
        Text("Subject: (no subject)", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        Text(text.ifEmpty { "Write your email…" })
    }
}
