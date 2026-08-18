package com.dinushlakmal.lakaboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dinushlakmal.lakaboard.audio.SoundProfile
import com.dinushlakmal.lakaboard.ui.MobileFrameSimulator
import com.dinushlakmal.lakaboard.ui.ThemeCustomizerDialog
import com.dinushlakmal.lakaboard.ui.TransliterationGuideDialog
import com.dinushlakmal.lakaboard.ui.theme.LakaBoardTheme
import com.dinushlakmal.lakaboard.viewmodel.KeyboardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KeyboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LakaBoardTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize().testTag("main_screen_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    LakaBoardHomeScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        onEnableKeyboard = ::openImeSettings,
                        onSwitchKeyboard = ::showImePicker
                    )
                }
            }
        }
    }

    private fun openImeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        } catch (e: Throwable) {
            Toast.makeText(this, "Could not open Keyboard Settings automatically. Please enable LakaBoard in System Settings > System > Languages & input > Keyboards.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showImePicker() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        } catch (e: Throwable) {
            Toast.makeText(this, "Could not launch Input Method Picker", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LakaBoardHomeScreen(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
    onEnableKeyboard: () -> Unit,
    onSwitchKeyboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showSoundDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Header Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        "LakaBoard",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Smart Sinhala Keyboard v2.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.toggleTransliterationGuide(true) },
                    modifier = Modifier.testTag("appbar_guide_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Singlish Guide")
                }
                IconButton(
                    onClick = { showSoundDialog = true },
                    modifier = Modifier.testTag("appbar_sound_button")
                ) {
                    Icon(
                        if (state.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Sound Settings"
                    )
                }
                IconButton(
                    onClick = { viewModel.toggleThemeCustomizer(true) },
                    modifier = Modifier.testTag("appbar_theme_button")
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = "Themes")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Setup & Activation Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("activation_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Keyboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Keyboard Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "To use LakaBoard system-wide across WhatsApp, Messenger, Notes and browsers:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEnableKeyboard,
                        modifier = Modifier.weight(1f).testTag("enable_ime_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("1. Enable", fontSize = 13.sp)
                    }
                    FilledTonalButton(
                        onClick = onSwitchKeyboard,
                        modifier = Modifier.weight(1f).testTag("switch_ime_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("2. Switch", fontSize = 13.sp)
                    }
                }
            }
        }

        // Section Title
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Interactive Testing Studio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    "Live Transliteration",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Mobile Testing Studio Frame
        MobileFrameSimulator(viewModel = viewModel)

        Spacer(Modifier.height(16.dp))
    }

    // Dialogs
    if (state.showThemeCustomizer) {
        ThemeCustomizerDialog(
            current = state.theme,
            onSelect = { viewModel.setTheme(it) },
            onDismiss = { viewModel.toggleThemeCustomizer(false) }
        )
    }

    if (state.showTransliterationGuide) {
        TransliterationGuideDialog(onDismiss = { viewModel.toggleTransliterationGuide(false) })
    }

    if (showSoundDialog) {
        SoundSettingsDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showSoundDialog = false }
        )
    }
}

@Composable
private fun SoundSettingsDialog(
    state: com.dinushlakmal.lakaboard.viewmodel.KeyboardUiState,
    viewModel: KeyboardViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sound & Haptics", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Keypress Audio Sound")
                    Switch(
                        checked = state.soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) },
                        modifier = Modifier.testTag("sound_toggle_switch")
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Haptic Vibration Feedback")
                    Switch(
                        checked = state.hapticEnabled,
                        onCheckedChange = { viewModel.setHapticEnabled(it) },
                        modifier = Modifier.testTag("haptic_toggle_switch")
                    )
                }
                HorizontalDivider()
                Text("Sound Profile", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                val profiles = listOf(
                    SoundProfile.MECHANICAL_CLICK to "Mechanical Click",
                    SoundProfile.SOFT_BUBBLE to "Soft Bubble",
                    SoundProfile.MODERN_POP to "Modern Pop",
                    SoundProfile.TYPEWRITER to "Typewriter"
                )
                profiles.forEach { (profile, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.soundProfile == profile,
                            onClick = { viewModel.setSoundProfile(profile) }
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("dismiss_sound_dialog")) {
                Text("Done")
            }
        }
    )
}
