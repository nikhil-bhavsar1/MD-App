package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorFont
import com.example.model.EditorSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsDialog(
    settings: EditorSettings,
    onSettingsChanged: (EditorSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1523),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF38BDF8)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Typography & Layout Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Editor Font Size
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B101A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Editor Code Font Size",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${settings.editorFontSizeSp} sp",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 14.sp
                        )
                    }

                    Slider(
                        value = settings.editorFontSizeSp.toFloat(),
                        onValueChange = { onSettingsChanged(settings.copy(editorFontSizeSp = it.toInt())) },
                        valueRange = 11f..26f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Preview Font Size
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B101A))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preview Body Font Size",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${settings.previewFontSizeSp} px",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC084FC),
                            fontSize = 14.sp
                        )
                    }

                    Slider(
                        value = settings.previewFontSizeSp.toFloat(),
                        onValueChange = { onSettingsChanged(settings.copy(previewFontSizeSp = it.toInt())) },
                        valueRange = 12f..28f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFC084FC),
                            activeTrackColor = Color(0xFFC084FC),
                            inactiveTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Font Family Selection
            Text(
                text = "Font Family",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorFont.values().forEach { font ->
                    FilterChip(
                        selected = settings.fontChoice == font,
                        onClick = { onSettingsChanged(settings.copy(fontChoice = font)) },
                        label = { Text(font.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E293B),
                            selectedLabelColor = Color(0xFF38BDF8),
                            containerColor = Color(0xFF0B101A),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Split orientation & toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Side-by-Side Split View",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (settings.isHorizontalSplit) "Horizontal panes (Left / Right)" else "Vertical panes (Top / Bottom)",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = settings.isHorizontalSplit,
                    onCheckedChange = { onSettingsChanged(settings.copy(isHorizontalSplit = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00E5FF),
                        checkedTrackColor = Color(0xFF0C4A6E),
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Line Numbers in Editor",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Display code line numbers in gutter",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = settings.showLineNumbers,
                    onCheckedChange = { onSettingsChanged(settings.copy(showLineNumbers = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF34D399),
                        checkedTrackColor = Color(0xFF064E3B),
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply & Close", fontWeight = FontWeight.Bold)
            }
        }
    }
}
