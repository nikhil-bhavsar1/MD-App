package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

class MarkdownVisualTransformation(
    private val selectedRange: TextRange = TextRange.Zero
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)

        // Syntax highlighting with precompiled static regexes
        highlightHeaders(raw, builder)
        highlightCodeBlocks(raw, builder)
        highlightInlineCode(raw, builder)
        highlightMath(raw, builder)
        highlightBoldItalicStrike(raw, builder)
        highlightMarks(raw, builder)
        highlightWikiLinks(raw, builder)
        highlightCalloutsAndQuotes(raw, builder)
        highlightCtanCommands(raw, builder)
        highlightLinks(raw, builder)
        highlightTasks(raw, builder)
        highlightTables(raw, builder)

        // Highlight selected code range if present and non-collapsed
        if (!selectedRange.collapsed && selectedRange.min >= 0 && selectedRange.max <= raw.length) {
            builder.addStyle(
                SpanStyle(
                    background = Color(0xFF00E5FF).copy(alpha = 0.35f),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                selectedRange.min,
                selectedRange.max
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun highlightHeaders(raw: String, builder: AnnotatedString.Builder) {
        for (match in HEADER_REGEX.findAll(raw)) {
            val level = match.groupValues[1].length
            val color = when (level) {
                1 -> Color(0xFF00E5FF) // Cyan
                2 -> Color(0xFF38BDF8) // Light blue
                3 -> Color(0xFF818CF8) // Indigo
                4 -> Color(0xFFA78BFA) // Purple
                5 -> Color(0xFFC084FC) // Violet
                else -> Color(0xFFF472B6) // Pink
            }
            builder.addStyle(
                SpanStyle(
                    color = color,
                    fontWeight = FontWeight.Bold
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightCodeBlocks(raw: String, builder: AnnotatedString.Builder) {
        for (match in CODE_BLOCK_REGEX.findAll(raw)) {
            val lang = match.groupValues[1].lowercase()
            val (bgColor, borderColor) = when {
                lang == "mermaid" -> Pair(Color(0xFF1E2415), Color(0xFFF59E0B)) // Amber/gold
                lang == "cytoscape" -> Pair(Color(0xFF26192E), Color(0xFFC084FC)) // Violet
                lang == "chart" || lang == "chartjs" -> Pair(Color(0xFF13222B), Color(0xFF00E5FF)) // Cyan chart
                lang in listOf("latex", "tex") -> Pair(Color(0xFF132420), Color(0xFF34D399)) // Emerald
                else -> Pair(Color(0xFF111827), Color(0xFF38BDF8)) // Standard code
            }

            builder.addStyle(
                SpanStyle(
                    background = bgColor,
                    color = Color(0xFFE2E8F0),
                    fontFamily = FontFamily.Monospace
                ),
                match.range.first,
                match.range.last + 1
            )

            val firstLineEnd = raw.indexOf('\n', match.range.first)
            if (firstLineEnd in match.range.first..match.range.last) {
                builder.addStyle(
                    SpanStyle(
                        color = borderColor,
                        fontWeight = FontWeight.Bold
                    ),
                    match.range.first,
                    firstLineEnd
                )
            }
        }
    }

    private fun highlightInlineCode(raw: String, builder: AnnotatedString.Builder) {
        for (match in INLINE_CODE_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFFF472B6),
                    background = Color(0xFF231B2A),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightMath(raw: String, builder: AnnotatedString.Builder) {
        // Display Math $$ ... $$
        for (match in DISPLAY_MATH_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFF34D399), // Emerald
                    background = Color(0xFF0D231A),
                    fontWeight = FontWeight.Medium
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        // Inline Math $ ... $
        for (match in INLINE_MATH_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.Medium
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightBoldItalicStrike(raw: String, builder: AnnotatedString.Builder) {
        // Bold **text**
        for (match in BOLD_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFFFFFFFF),
                    fontWeight = FontWeight.ExtraBold
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        // Italic *text*
        for (match in ITALIC_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFFDDD6FE),
                    fontStyle = FontStyle.Italic
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        // Strikethrough ~~text~~
        for (match in STRIKE_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFF94A3B8),
                    textDecoration = TextDecoration.LineThrough
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightMarks(raw: String, builder: AnnotatedString.Builder) {
        for (match in MARK_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    background = Color(0xFFFEF08A).copy(alpha = 0.35f),
                    color = Color(0xFFFEF08A),
                    fontWeight = FontWeight.SemiBold
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightWikiLinks(raw: String, builder: AnnotatedString.Builder) {
        for (match in WIKI_LINK_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFFC084FC),
                    background = Color(0xFF281C38),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightCalloutsAndQuotes(raw: String, builder: AnnotatedString.Builder) {
        for (match in CALLOUT_REGEX.findAll(raw)) {
            val type = match.groupValues[1]
            val color = when (type) {
                "NOTE", "INFO" -> Color(0xFF38BDF8)
                "TIP", "SUCCESS" -> Color(0xFF34D399)
                "WARNING", "QUESTION" -> Color(0xFFFBBF24)
                "CAUTION", "DANGER", "BUG" -> Color(0xFFF87171)
                "EXAMPLE" -> Color(0xFFA78BFA)
                else -> Color(0xFFE2E8F0)
            }
            builder.addStyle(
                SpanStyle(
                    color = color,
                    fontWeight = FontWeight.Bold
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightCtanCommands(raw: String, builder: AnnotatedString.Builder) {
        for (match in CTAN_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    background = Color(0xFF0B2135)
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightLinks(raw: String, builder: AnnotatedString.Builder) {
        for (match in LINK_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFF60A5FA),
                    fontWeight = FontWeight.SemiBold
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        for (match in IMG_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.SemiBold
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightTasks(raw: String, builder: AnnotatedString.Builder) {
        for (match in TASK_REGEX.findAll(raw)) {
            val isChecked = match.groupValues[1].contains("x", ignoreCase = true)
            builder.addStyle(
                SpanStyle(
                    color = if (isChecked) Color(0xFF34D399) else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    private fun highlightTables(raw: String, builder: AnnotatedString.Builder) {
        for (match in TABLE_DELIM_REGEX.findAll(raw)) {
            builder.addStyle(
                SpanStyle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                ),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    companion object {
        private val HEADER_REGEX = Regex("^(#{1,6})\\s+(.*)$", RegexOption.MULTILINE)
        private val CODE_BLOCK_REGEX = Regex("```([a-zA-Z0-9_-]*)\\n([\\s\\S]*?)```")
        private val INLINE_CODE_REGEX = Regex("`([^`\\n]+)`")
        private val DISPLAY_MATH_REGEX = Regex("\\$\\$([\\s\\S]*?)\\$\\$")
        private val INLINE_MATH_REGEX = Regex("(?<!\\$)\\$([^$\\n]+)\\$(?!\\$)")
        private val BOLD_REGEX = Regex("\\*\\*([^\\*\\n]+)\\*\\*")
        private val ITALIC_REGEX = Regex("(?<!\\*)\\*([^\\*\\n]+)\\*(?!\\*)")
        private val STRIKE_REGEX = Regex("~~([^~\\n]+)~~")
        private val MARK_REGEX = Regex("(?:==([^=\\n]+)==|<mark>([\\s\\S]*?)</mark>)")
        private val WIKI_LINK_REGEX = Regex("\\[\\[([^\\]]+)\\]\\]")
        private val CALLOUT_REGEX = Regex("^>\\s*\\[!(NOTE|TIP|WARNING|CAUTION|DANGER|INFO|QUESTION|SUCCESS|EXAMPLE|BUG|QUOTE)\\]", RegexOption.MULTILINE)
        private val CTAN_REGEX = Regex("\\\\(usepackage|ce|SI|si|bra|ket|braket|cancel)\\{([^}]+)\\}")
        private val LINK_REGEX = Regex("(?<!\\!)\\[([^\\]]+)\\]\\(([^\\)]+)\\)")
        private val IMG_REGEX = Regex("!\\[([^\\]]*)\\]\\(([^\\)]+)\\)")
        private val TASK_REGEX = Regex("^\\s*-\\s*(\\[[ xX]\\])", RegexOption.MULTILINE)
        private val TABLE_DELIM_REGEX = Regex("^\\s*\\|\\s*(?:[-:]+\\s*\\|)+\\s*$", RegexOption.MULTILINE)
    }
}
