package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FindReplaceBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    matchCount: Int,
    currentMatchIndex: Int,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color(0xF20B1220),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D00E5FF))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Find Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Find in markdown...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = if (matchCount > 0) "${currentMatchIndex + 1}/$matchCount" else "0",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (matchCount > 0) Color(0xFF38BDF8) else Color(0xFFEF4444),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    },
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF070A10),
                        unfocusedContainerColor = Color(0xFF070A10)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                IconButton(
                    onClick = onFindPrevious,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous",
                        tint = if (matchCount > 0) Color(0xFF00E5FF) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onFindNext,
                    enabled = matchCount > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next",
                        tint = if (matchCount > 0) Color(0xFF00E5FF) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Replace Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Replace with...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFC084FC),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF070A10),
                        unfocusedContainerColor = Color(0xFF070A10)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = onReplaceCurrent,
                    enabled = matchCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFFC084FC),
                        disabledContainerColor = Color(0xFF0B101B),
                        disabledContentColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Replace", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onReplaceAll,
                    enabled = matchCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E1065),
                        contentColor = Color(0xFFE879F9),
                        disabledContainerColor = Color(0xFF0B101B),
                        disabledContentColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
