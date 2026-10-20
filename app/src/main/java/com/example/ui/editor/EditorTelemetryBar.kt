package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorSettings
import com.example.model.ViewMode

@Composable
fun EditorTelemetryBar(
    content: String,
    installedCtanCount: Int,
    viewMode: ViewMode,
    settings: EditorSettings,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val stats = remember(content) {
        val chars = content.length
        val words = if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).size
        val lines = if (content.isEmpty()) 1 else content.lines().size
        val mathCount = Regex("(\\\$\$[\\s\\S]*?\\\$|(?<!\\\$)\\\$[^\\$\\n]+\\\$(?!\\\$))").findAll(content).count()
        val graphCount = Regex("```(cytoscape|mermaid)").findAll(content).count()
        val readMinutes = (words / 200).coerceAtLeast(1)
        DocumentStats(chars, words, lines, mathCount, graphCount, readMinutes)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp),
        color = Color(0xF2080D18),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1A38BDF8)),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                )
                Text(
                    text = "LIVE AST",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            }

            TelemetryItem(label = "LINES", value = stats.lines.toString(), valueColor = Color(0xFFE2E8F0))
            TelemetryItem(label = "WORDS", value = stats.words.toString(), valueColor = Color(0xFFE2E8F0))
            TelemetryItem(label = "CHARS", value = stats.chars.toString(), valueColor = Color(0xFF94A3B8))
            TelemetryItem(label = "READ", value = "~${stats.readingMinutes}m", valueColor = Color(0xFFFBBF24))
            TelemetryItem(label = "LATEX", value = "${stats.mathCount} eq", valueColor = Color(0xFF34D399))
            TelemetryItem(label = "CTAN", value = "$installedCtanCount pkg", valueColor = Color(0xFF38BDF8))
            TelemetryItem(label = "GRAPHS", value = "${stats.graphCount} diag", valueColor = Color(0xFFC084FC))
            TelemetryItem(label = "FONT", value = "${settings.editorFontSizeSp}sp", valueColor = Color(0xFF64748B))
            TelemetryItem(label = "VAULT", value = "SYNCED .MD", valueColor = Color(0xFF34D399))
            TelemetryItem(label = "MODE", value = viewMode.name, valueColor = Color(0xFFF472B6))
        }
    }
}

@Composable
private fun TelemetryItem(
    label: String,
    value: String,
    valueColor: Color = Color(0xFFE2E8F0)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class DocumentStats(
    val chars: Int,
    val words: Int,
    val lines: Int,
    val mathCount: Int,
    val graphCount: Int,
    val readingMinutes: Int
)
