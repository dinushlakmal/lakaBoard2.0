package com.dinushlakmal.lakaboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class GuideRow(val rule: String, val type: String, val example: String)

private val GUIDE_ROWS = listOf(
    GuideRow("k, g, c/ch, j, t, d, n, p, b, m, y, r, l, v, s, h", "Base consonant (inherent 'a')", "ka -> ක"),
    GuideRow("+ hal kirima (no vowel / word end)", "Hal kirima", "k -> ක්"),
    GuideRow("+ aa / A", "Alapilla (long aa)", "kaa -> කා"),
    GuideRow("+ i / ii / I", "Kombuva family", "ki -> කි, kii -> කී"),
    GuideRow("+ u / uu / U / oo", "Papilla family", "ku -> කු, koo -> කූ"),
    GuideRow("+ e / E", "Kombuva", "ke -> කෙ, kE -> කේ"),
    GuideRow("+ o / O", "Kombu+Paapilla", "ko -> කො, kO -> කෝ"),
    GuideRow("+ y + vowel", "Yansaya", "kya -> ක්‍ය"),
    GuideRow("+ r + vowel", "Rakaransaya", "kra -> ක්‍ර"),
    GuideRow("nd, mb, ng", "Bandi akuru (prenasalized)", "kanda -> කන්ඬ style words"),
    GuideRow("T, D, N, L (capital)", "Retroflex letters", "Tikak -> ටිකක්"),
    GuideRow("sh / Sh", "Sibilants", "sha -> ශ, Sha -> ෂ")
)

@Composable
fun TransliterationGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Singlish Transliteration Guide") },
        text = {
            Column {
                Text(
                    "Type Sinhala phonetically using English letters. LakaBoard converts it live to Sinhala Unicode.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(GUIDE_ROWS) { row ->
                        Column(Modifier) {
                            Text(row.type, style = MaterialTheme.typography.labelLarge)
                            Text("Keys: ${row.rule}", style = MaterialTheme.typography.bodySmall)
                            Text("Example: ${row.example}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}
