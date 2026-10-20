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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorFormattingToolbar(
    onInsertText: (prefix: String, suffix: String) -> Unit,
    onHighlightSelection: () -> Unit,
    onOpenSyntaxCatalog: () -> Unit,
    onOpenUnicodePicker: () -> Unit,
    onOpenCtan: () -> Unit,
    onOpenGraphs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        color = Color(0xFF0D131F),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Full Syntax Palette launcher
            ToolbarAction(
                label = "📚 ALL",
                desc = "Open Complete Markdown Syntax Palette",
                color = Color(0xFF00E5FF),
                onClick = onOpenSyntaxCatalog
            )
            ToolbarAction(
                label = "😀",
                desc = "Unicode Icons & Emojis",
                color = Color(0xFFFBBF24),
                onClick = onOpenUnicodePicker
            )

            DividerItem()

            // Heading H1, H2, H3, H4
            ToolbarAction(label = "H1", onClick = { onInsertText("# ", "") })
            ToolbarAction(label = "H2", onClick = { onInsertText("## ", "") })
            ToolbarAction(label = "H3", onClick = { onInsertText("### ", "") })
            ToolbarAction(label = "H4", onClick = { onInsertText("#### ", "") })

            DividerItem()

            // Text Styles: Bold, Italic, Strikethrough, Neon Highlight, Underline
            ToolbarIconButton(icon = Icons.Default.FormatBold, desc = "Bold", onClick = { onInsertText("**", "**") })
            ToolbarIconButton(icon = Icons.Default.FormatItalic, desc = "Italic", onClick = { onInsertText("*", "*") })
            ToolbarAction(label = "~~", desc = "Strikethrough", onClick = { onInsertText("~~", "~~") })

            // Highlight selected code
            ToolbarIconButton(
                icon = Icons.Default.FormatPaint,
                desc = "Highlight Selection (==...==)",
                tint = Color(0xFFFEF08A),
                onClick = onHighlightSelection
            )
            ToolbarIconButton(
                icon = Icons.Default.FormatUnderlined,
                desc = "Underline",
                onClick = { onInsertText("<u>", "</u>") }
            )

            DividerItem()

            // Code & Code Block
            ToolbarIconButton(icon = Icons.Default.Code, desc = "Inline Code", onClick = { onInsertText("`", "`") })
            ToolbarIconButton(
                icon = Icons.Default.DataObject,
                desc = "Code Block",
                onClick = { onInsertText("```kotlin\n", "\n```\n") }
            )

            DividerItem()

            // Charts (Bar, Line, Pie, Radar)
            ToolbarIconButton(
                icon = Icons.Default.BarChart,
                desc = "Chart.js Visual Charts",
                tint = Color(0xFF00E5FF),
                onClick = {
                    onInsertText(
                        "```chart bar\nlabels: [\"Q1\", \"Q2\", \"Q3\", \"Q4\"]\ndataset: \"Metric\", [35, 59, 80, 120], #00E5FF\n```\n",
                        ""
                    )
                }
            )

            // LaTeX Math & CTAN
            ToolbarIconButton(
                icon = Icons.Default.Functions,
                desc = "Display Math $$",
                tint = Color(0xFF34D399),
                onClick = { onInsertText("$$\n", "\n$$\n") }
            )
            ToolbarAction(
                label = "CTAN",
                desc = "CTAN Database & Macros",
                color = Color(0xFF00E5FF),
                onClick = onOpenCtan
            )

            DividerItem()

            // Graph Diagrams (Mermaid & Cytoscape + Dagre)
            ToolbarIconButton(
                icon = Icons.Default.Hub,
                desc = "Cytoscape Dagre & Mermaid Graphs",
                tint = Color(0xFFC084FC),
                onClick = onOpenGraphs
            )

            DividerItem()

            // Quotes & Obsidian Callout
            ToolbarIconButton(icon = Icons.Default.FormatQuote, desc = "Blockquote", onClick = { onInsertText("> ", "") })
            ToolbarAction(
                label = "[!NOTE]",
                desc = "Obsidian Callout",
                color = Color(0xFF38BDF8),
                onClick = { onInsertText("> [!NOTE]\n> ", "\n") }
            )

            DividerItem()

            // Lists & Tasks & Tables & WikiLinks
            ToolbarIconButton(icon = Icons.AutoMirrored.Filled.FormatListBulleted, desc = "List", onClick = { onInsertText("- ", "") })
            ToolbarIconButton(icon = Icons.Default.TaskAlt, desc = "Task", onClick = { onInsertText("- [ ] ", "") })
            ToolbarIconButton(
                icon = Icons.Default.TableChart,
                desc = "Table",
                onClick = {
                    onInsertText(
                        "| Header 1 | Header 2 | Header 3 |\n| :--- | :---: | ---: |\n| Item A | Item B | Item C |\n",
                        ""
                    )
                }
            )
            ToolbarAction(
                label = "[[ ]]",
                desc = "WikiLink Cross-reference",
                color = Color(0xFFC084FC),
                onClick = { onInsertText("[[", "]]") }
            )
            ToolbarIconButton(icon = Icons.Default.Link, desc = "Web Link", onClick = { onInsertText("[", "](https://)") })
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    desc: String,
    tint: Color = Color(0xFF94A3B8),
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        colors = IconButtonDefaults.iconButtonColors(contentColor = tint)
    ) {
        Icon(imageVector = icon, contentDescription = desc, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun ToolbarAction(
    label: String,
    desc: String = label,
    color: Color = Color(0xFFE2E8F0),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(width = 46.dp, height = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DividerItem() {
    VerticalDivider(
        modifier = Modifier
            .height(20.dp)
            .padding(horizontal = 2.dp),
        color = Color(0xFF1E293B)
    )
}
