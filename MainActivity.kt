package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CalculatorScreen() }
    }
}

private enum class Kind { DIGIT, FN, OP, EQ }
private data class Key(val label: String, val id: String, val kind: Kind, val desc: String = label)

private val rows = listOf(
    listOf(Key("AC", "AC", Kind.FN, "Clear all"), Key("⌫", "DEL", Kind.FN, "Delete"),
        Key("%", "%", Kind.FN, "Percent"), Key("÷", "/", Kind.OP, "Divide")),
    listOf(Key("7", "7", Kind.DIGIT), Key("8", "8", Kind.DIGIT), Key("9", "9", Kind.DIGIT),
        Key("×", "*", Kind.OP, "Multiply")),
    listOf(Key("4", "4", Kind.DIGIT), Key("5", "5", Kind.DIGIT), Key("6", "6", Kind.DIGIT),
        Key("−", "-", Kind.OP, "Subtract")),
    listOf(Key("1", "1", Kind.DIGIT), Key("2", "2", Kind.DIGIT), Key("3", "3", Kind.DIGIT),
        Key("+", "+", Kind.OP, "Add")),
    listOf(Key("±", "NEG", Kind.DIGIT, "Toggle sign"), Key("0", "0", Kind.DIGIT),
        Key(".", ".", Kind.DIGIT, "Decimal point"), Key("=", "=", Kind.EQ, "Equals")),
)

@Composable
fun CalculatorScreen() {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF14201C) else Color(0xFFE9EFE6)
    val text = if (dark) Color(0xFFEAF2EC) else Color(0xFF1B2A24)
    val muted = if (dark) Color(0xFF8CA39A) else Color(0xFF5C6E65)
    val state = remember { CalcState() }
    val haptic = LocalHapticFeedback.current

    fun colors(kind: Kind): Pair<Color, Color> = when (kind) {
        Kind.DIGIT -> (if (dark) Color(0xFF1F2F29) else Color(0xFFF8FAF6)) to text
        Kind.FN -> (if (dark) Color(0xFF273A33) else Color(0xFFDDE5D8)) to text
        Kind.OP -> (if (dark) Color(0xFF2C4A40) else Color(0xFFCFE0D4)) to
            (if (dark) Color(0xFFA8DCC8) else Color(0xFF1F4A3E))
        Kind.EQ -> Color(0xFFE2A33B) to Color(0xFF2A1C00)
    }

    Column(
        Modifier.fillMaxSize().background(bg).systemBarsPadding().padding(14.dp)
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                if (state.history.isNotEmpty() && state.expr.isNotEmpty() && state.history.endsWith("="))
                    state.history else state.display,
                color = muted, fontSize = 26.sp, maxLines = 1, textAlign = TextAlign.End
            )
            Spacer(Modifier.height(6.dp))
            val result = state.result
            Text(
                result, color = text, fontWeight = FontWeight.Bold, maxLines = 1,
                fontSize = when {
                    result.length > 14 -> 36.sp
                    result.length > 10 -> 48.sp
                    else -> 68.sp
                },
                textAlign = TextAlign.End
            )
            Spacer(Modifier.height(14.dp))
        }

        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val (kbg, kfg) = colors(key.kind)
                    Box(
                        Modifier.weight(1f).height(74.dp)
                            .background(kbg, RoundedCornerShape(22.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                state.press(key.id)
                            }
                            .semantics { contentDescription = key.desc },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(key.label, color = kfg, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
