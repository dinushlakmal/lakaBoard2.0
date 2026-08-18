package com.dinushlakmal.lakaboard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinushlakmal.lakaboard.audio.SoundHapticHelper
import com.dinushlakmal.lakaboard.viewmodel.KeyboardViewModel

private enum class SimApp(val title: String, val icon: String) {
    CHAT("Chat", "💬"),
    NOTEPAD("Notepad", "📝"),
    SEARCH("Search", "🔍"),
    EMAIL("Email", "✉️")
}

/**
 * MobileFrameSimulator
 * ---------------------------------------------------------------------
 * Interactive smartphone-shaped testing studio: lets users test
 * Singlish transliteration, Wijesekara layout, clipboard history,
 * phrases, sound profiles, and themes across multiple simulated apps.
 */
@Composable
fun MobileFrameSimulator(viewModel: KeyboardViewModel) {
    val context = LocalContext.current
    val soundHapticHelper = remember { SoundHapticHelper(context) }
    var selectedApp by remember { mutableStateOf(SimApp.CHAT) }
    val appTexts = remember { mutableStateMapOf(
        SimApp.CHAT to "ආයුබෝවන්! කොහොමද?",
        SimApp.NOTEPAD to "",
        SimApp.SEARCH to "",
        SimApp.EMAIL to ""
    ) }

    val currentText = appTexts[selectedApp] ?: ""

    // Wire up the simulator as the active text input target for KeyboardViewModel
    DisposableEffect(selectedApp) {
        viewModel.onCommitText = { textToAppend ->
            try {
                val existing = appTexts[selectedApp] ?: ""
                appTexts[selectedApp] = existing + textToAppend
            } catch (_: Throwable) {}
        }
        viewModel.onUpdateComposingWord = { rendered ->
            try {
                val prev = appTexts[selectedApp] ?: ""
                val lastSpace = prev.lastIndexOf(' ')
                val base = if (lastSpace >= 0 && lastSpace < prev.length) prev.substring(0, lastSpace + 1) else ""
                appTexts[selectedApp] = base + rendered
            } catch (_: Throwable) {}
        }
        viewModel.onFinishComposing = {
            // Composing word finalized
        }
        viewModel.onDeleteBackward = {
            try {
                val prev = appTexts[selectedApp] ?: ""
                if (prev.isNotEmpty()) {
                    appTexts[selectedApp] = prev.dropLast(1)
                }
            } catch (_: Throwable) {}
        }
        onDispose { }
    }

    val copyToClipboard: (String) -> Unit = { text ->
        try {
            if (text.isNotEmpty()) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("LakaBoard text", text))
                viewModel.addToClipboardHistory(text, autoCapture = true)
                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Throwable) {}
    }

    val shareText: (String) -> Unit = { text ->
        try {
            if (text.isNotEmpty()) {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share Sinhala Text").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(shareIntent)
            }
        } catch (_: Throwable) {
            Toast.makeText(context, "Cannot open sharing dialog", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF13151D))
            .border(1.dp, Color(0xFF2E3245), RoundedCornerShape(24.dp))
            .padding(8.dp)
            .testTag("simulator_frame")
    ) {
        // Simulator top bar / telemetry
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${currentText.length} chars • ${if (currentText.isBlank()) 0 else currentText.trim().split("\\s+".toRegex()).size} words",
                    color = Color(0xFF9E9EB2),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentText.isNotEmpty()) {
                    IconButton(
                        onClick = { appTexts[selectedApp] = "" },
                        modifier = Modifier.size(32.dp).testTag("clear_text_button")
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFF9E9EB2),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { copyToClipboard(currentText) },
                    modifier = Modifier.size(32.dp).testTag("copy_text_button")
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFFF5C34D),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { shareText(currentText) },
                    modifier = Modifier.size(32.dp).testTag("share_text_button")
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF3DDC84),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // App category chips
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SimApp.values().forEach { app ->
                val isSelected = selectedApp == app
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedApp = app },
                    label = { Text("${app.icon} ${app.title}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2C324A),
                        selectedLabelColor = Color(0xFFF5C34D),
                        containerColor = Color(0xFF1E212D),
                        labelColor = Color(0xFFB0B4C8)
                    ),
                    modifier = Modifier.weight(1f).testTag("app_tab_${app.name.lowercase()}")
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Screen canvas for current app preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFAFAFC))
                .padding(10.dp)
        ) {
            when (selectedApp) {
                SimApp.CHAT -> ChatBubblePreview(currentText)
                SimApp.NOTEPAD -> NotepadPreview(currentText)
                SimApp.SEARCH -> SearchBarPreview(currentText)
                SimApp.EMAIL -> EmailComposerPreview(currentText)
            }
        }

        Spacer(Modifier.height(6.dp))

        // Keyboard Composable
        LakaBoardKeyboard(
            viewModel = viewModel,
            soundHapticHelper = soundHapticHelper,
            onRequestHideKeyboard = { }
        )
    }
}

@Composable
private fun ChatBubblePreview(text: String) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 2.dp))
                .background(Color(0xFFE7FED4))
                .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 2.dp))
                .padding(10.dp)
                .align(Alignment.End)
        ) {
            Text(
                text = text.ifEmpty { "Type a Sinhala message..." },
                color = if (text.isEmpty()) Color.Gray else Color(0xFF1B2733),
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun NotepadPreview(text: String) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = text.ifEmpty { "Start typing your Sinhala notes, essays, or thoughts here..." },
            color = if (text.isEmpty()) Color.Gray else Color(0xFF1B2733),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun SearchBarPreview(text: String) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEEF2F6), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔍", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = text.ifEmpty { "Search Sinhala phrases or English terms..." },
                color = if (text.isEmpty()) Color.Gray else Color(0xFF1B2733),
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Trending: ලංකාව, ආයුබෝවන්, ශ්‍රී ලංකා, කොළඹ",
            fontSize = 11.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun EmailComposerPreview(text: String) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("To: friend@example.com", fontSize = 11.sp, color = Color.Gray)
        Text("Subject: සුබ පැතුම් / Greetings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color(0xFFE5E7EB))
        Text(
            text = text.ifEmpty { "Compose Sinhala email message..." },
            color = if (text.isEmpty()) Color.Gray else Color(0xFF1B2733),
            fontSize = 14.sp
        )
    }
}
