package com.dinushlakmal.lakaboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dinushlakmal.lakaboard.ui.MobileFrameSimulator
import com.dinushlakmal.lakaboard.ui.theme.LakaBoardTheme
import com.dinushlakmal.lakaboard.viewmodel.KeyboardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KeyboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LakaBoardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LakaBoardHomeScreen(
                        viewModel = viewModel,
                        onEnableKeyboard = ::openImeSettings,
                        onSwitchKeyboard = ::showImePicker
                    )
                }
            }
        }
    }

    private fun openImeSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

    private fun showImePicker() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }
}

@Composable
fun LakaBoardHomeScreen(
    viewModel: KeyboardViewModel,
    onEnableKeyboard: () -> Unit,
    onSwitchKeyboard: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopAppBar(
            title = { Text("LakaBoard v2.0") },
            actions = {
                IconButton(onClick = onSwitchKeyboard) {
                    Icon(Icons.Filled.Keyboard, contentDescription = "Switch keyboard")
                }
                IconButton(onClick = onEnableKeyboard) {
                    Icon(Icons.Filled.Settings, contentDescription = "Enable keyboard")
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Step 1: Enable LakaBoard in system settings.\n" +
                "Step 2: Switch to LakaBoard from any text field.\n" +
                "Try it below in the interactive simulator.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        MobileFrameSimulator(viewModel = viewModel)
    }
}
