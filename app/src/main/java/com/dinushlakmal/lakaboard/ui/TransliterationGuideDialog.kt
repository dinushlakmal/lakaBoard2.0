package com.dinushlakmal.lakaboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class GuideRow(val rule: String, val type: String, val example: String)

private val GUIDE_ROWS = listOf(
    GuideRow("k, g, c/ch, j, t, d, n, p, b, m, y, r, l, v, s, h", "Base Consonants", "ka → ක, ga → ග, ma → ම"),
    GuideRow("+ hal kirima (no vowel / word end)", "Hal Kirima (Pure Consonant)", "k → ක්, n → න්, l → ල්"),
    GuideRow("+ aa / A", "Alapilla (Long 'aa')", "kaa → කා, mA → මා"),
    GuideRow("+ i / ii / I", "I-pilla Family", "ki → කි, kii → කී"),
    GuideRow("+ u / uu / U / oo", "U-pilla Family", "ku → කු, koo → කූ, kU → කූ"),
    GuideRow("+ e / E / ee", "Kombuva / Diga Kombuva", "ke → කෙ, kE → කේ"),
    GuideRow("+ o / O / oo", "Kombuva + Aela-pilla", "ko → කො, kO → කෝ"),
    GuideRow("+ ai / au", "Diphthongs", "kai → කයි, kau → කෞ"),
    GuideRow("+ y + vowel", "Yansaya", "kya → ක්‍ය, pya → ප්‍ය"),
    GuideRow("+ r + vowel", "Rakaransaya", "kra → ක්‍ර, pra → ප්‍ර"),
    GuideRow("nd, mb, ng, ngg, ndh", "Bandi Akuru (Prenasalized)", "kanda → කන්ඬ, ganga → ගඟ, lanka → ලංකා"),
    GuideRow("T, D, N, L (Capitalized)", "Retroflex Letters", "Tikak → ටිකක්, Lassanai → ලස්සනයි"),
    GuideRow("sh / Sh / SH", "Sibilants (Taaluja / Murdhaja)", "sha → ශ, Sha → ෂ")
)

@Composable
fun TransliterationGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Singlish Transliteration Guide",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    "Type Sinhala phonetically using English letters. LakaBoard converts it live to Sinhala Unicode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GUIDE_ROWS) { row ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    row.type,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Keys: ${row.rule}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Example: ${row.example}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_guide_button")
            ) {
                Text("Got It")
            }
        }
    )
}
