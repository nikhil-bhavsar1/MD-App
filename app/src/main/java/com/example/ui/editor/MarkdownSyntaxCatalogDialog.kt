package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SyntaxItem(
    val title: String,
    val category: String,
    val description: String,
    val previewLabel: String,
    val snippet: String,
    val accentColor: Color = Color(0xFF38BDF8)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownSyntaxCatalogDialog(
    onInsertSnippet: (snippet: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val allSyntaxItems = remember {
        listOf(
            // 1. Headings & Dividers
            SyntaxItem("Heading 1 (#)", "Headings", "Top level document title", "# Title", "# ", Color(0xFF00E5FF)),
            SyntaxItem("Heading 2 (##)", "Headings", "Major section title", "## Section", "## ", Color(0xFF38BDF8)),
            SyntaxItem("Heading 3 (###)", "Headings", "Subsection title", "### Subsection", "### ", Color(0xFF818CF8)),
            SyntaxItem("Heading 4 (####)", "Headings", "Minor section title", "#### Minor", "#### ", Color(0xFFA78BFA)),
            SyntaxItem("Heading 5 (#####)", "Headings", "Deep section header", "##### Sub-minor", "##### ", Color(0xFFC084FC)),
            SyntaxItem("Heading 6 (######)", "Headings", "Smallest heading level", "###### Tiny", "###### ", Color(0xFFF472B6)),
            SyntaxItem("Thematic Divider (---)", "Headings", "Horizontal glowing divider", "---", "\n---\n\n", Color(0xFF94A3B8)),
            SyntaxItem("Collapsible Details", "Headings", "Expandable accordion disclosure block", "<details><summary>", "<details>\n<summary>Click to Expand</summary>\n\nHidden detailed content goes here...\n</details>\n", Color(0xFFFBBF24)),

            // 2. Text Formatting
            SyntaxItem("Bold (**text**)", "Formatting", "Strong font weight emphasis", "**Bold**", "**bold text**", Color(0xFFFFFFFF)),
            SyntaxItem("Italic (*text*)", "Formatting", "Slanted font style", "*Italic*", "*italic text*", Color(0xFFDDD6FE)),
            SyntaxItem("Strikethrough (~~text~~)", "Formatting", "Crossed out strikethrough line", "~~Strikethrough~~", "~~deleted text~~", Color(0xFF94A3B8)),
            SyntaxItem("Neon Highlight (==text==)", "Formatting", "Luminous highlighter marker", "==Highlighted==", "==neon highlight==", Color(0xFFFEF08A)),
            SyntaxItem("Underline (<u>text</u>)", "Formatting", "Underlined text markup", "<u>Underline</u>", "<u>underlined text</u>", Color(0xFF38BDF8)),
            SyntaxItem("Superscript (^2^)", "Formatting", "Exponential or power character", "x^2^", "x^2^", Color(0xFFC084FC)),
            SyntaxItem("Subscript (~2~)", "Formatting", "Subscript chemical/matrix index", "H~2~O", "H~2~O", Color(0xFF34D399)),
            SyntaxItem("Keyboard Key (<kbd>)", "Formatting", "Hardware keyboard button cap", "<kbd>Ctrl</kbd>", "<kbd>Ctrl</kbd> + <kbd>C</kbd>", Color(0xFFE2E8F0)),
            SyntaxItem("Inline Code (`code`)", "Formatting", "Monospace inline code span", "`code`", "`val x = 42`", Color(0xFFF472B6)),

            // 3. Lists, Tasks & Quotes
            SyntaxItem("Bullet List (-)", "Lists & Tasks", "Unordered bullet point", "- Point", "- First bullet\n- Second bullet\n- Third bullet\n", Color(0xFF38BDF8)),
            SyntaxItem("Numbered List (1.)", "Lists & Tasks", "Sequential ordered list", "1. Item", "1. Initial step\n2. Next action\n3. Completion\n", Color(0xFF818CF8)),
            SyntaxItem("Task Item (- [ ])", "Lists & Tasks", "Unchecked actionable checklist item", "- [ ] Task", "- [ ] Pending task item\n", Color(0xFFFBBF24)),
            SyntaxItem("Completed Task (- [x])", "Lists & Tasks", "Checked completed task item", "- [x] Done", "- [x] Finished task item\n", Color(0xFF34D399)),
            SyntaxItem("Standard Blockquote (>)", "Lists & Tasks", "Indented blockquote banner", "> Quote", "> \"Simplicity is prerequisite for reliability.\"\n> — Edsger Dijkstra\n", Color(0xFFC084FC)),

            // 4. Obsidian Callouts
            SyntaxItem("Callout [!NOTE]", "Callouts", "Informative context box", "> [!NOTE]", "> [!NOTE]\n> Important architectural context and notes here.\n\n", Color(0xFF38BDF8)),
            SyntaxItem("Callout [!TIP]", "Callouts", "Pro-tip and best practices", "> [!TIP]", "> [!TIP]\n> Use side-by-side mode on wide screens for peak productivity!\n\n", Color(0xFF34D399)),
            SyntaxItem("Callout [!WARNING]", "Callouts", "Cautionary warning notice", "> [!WARNING]", "> [!WARNING]\n> Modifying database schemas directly may require a migration.\n\n", Color(0xFFFBBF24)),
            SyntaxItem("Callout [!DANGER]", "Callouts", "Critical hazardous warning", "> [!DANGER]", "> [!DANGER]\n> Permanent deletion cannot be undone!\n\n", Color(0xFFF87171)),
            SyntaxItem("Callout [!QUESTION]", "Callouts", "Inquiry and clarification card", "> [!QUESTION]", "> [!QUESTION]\n> Which database engine best suits multi-device sync?\n\n", Color(0xFFA78BFA)),
            SyntaxItem("Callout [!SUCCESS]", "Callouts", "Completed operation celebration", "> [!SUCCESS]", "> [!SUCCESS]\n> Unit test suite passed with 100% test coverage.\n\n", Color(0xFF10B981)),

            // 5. Visual Charts (Bar, Line, Radar, Pie, Doughnut)
            SyntaxItem(
                "Chart.js: Bar Chart",
                "Charts & Graphics",
                "Canvas-rendered bar chart with custom colors and labels",
                "```chart bar",
                """
```chart bar
labels: ["Jan", "Feb", "Mar", "Apr", "May"]
dataset: "Throughput (K req/s)", [42, 68, 95, 110, 145], #00E5FF
```
""".trimIndent() + "\n\n",
                Color(0xFF00E5FF)
            ),
            SyntaxItem(
                "Chart.js: Line Chart",
                "Charts & Graphics",
                "Time series line chart with area fill and smooth tension",
                "```chart line",
                """
```chart line
labels: ["00:00", "04:00", "08:00", "12:00", "16:00", "20:00"]
dataset: "Active Nodes", [14, 18, 35, 62, 58, 44], #C084FC
```
""".trimIndent() + "\n\n",
                Color(0xFFC084FC)
            ),
            SyntaxItem(
                "Chart.js: Pie Chart",
                "Charts & Graphics",
                "Multi-slice distribution pie chart",
                "```chart pie",
                """
```chart pie
labels: ["Kotlin", "Rust", "TypeScript", "Python"]
data: [45, 25, 20, 10]
```
""".trimIndent() + "\n\n",
                Color(0xFFFBBF24)
            ),
            SyntaxItem(
                "Chart.js: Radar Chart",
                "Charts & Graphics",
                "Spider radar comparison across multiple axes",
                "```chart radar",
                """
```chart radar
labels: ["Performance", "Memory", "Throughput", "Security", "Scalability"]
dataset: "HyperMark Engine", [95, 88, 92, 98, 90], #34D399
```
""".trimIndent() + "\n\n",
                Color(0xFF34D399)
            ),

            // 6. Graphs: Cytoscape & Mermaid
            SyntaxItem(
                "Cytoscape Dagre Directed Graph",
                "Charts & Graphics",
                "Interactive hierarchical directed acyclic graph layout",
                "```cytoscape",
                """
```cytoscape
layout: dagre
nodes:
  - id: input, label: "Input Buffer", color: "#38BDF8"
  - id: parser, label: "AST Engine", color: "#C084FC"
  - id: render, label: "Canvas / SVG", color: "#00E5FF"
edges:
  - source: input, target: parser, label: "UTF-8"
  - source: parser, target: render, label: "DOM"
```
""".trimIndent() + "\n\n",
                Color(0xFFC084FC)
            ),
            SyntaxItem(
                "Mermaid Flowchart",
                "Charts & Graphics",
                "Top-down flowchart with decision diamonds and styles",
                "```mermaid",
                """
```mermaid
graph TD
    A[Start] --> B{Valid?}
    B -->|Yes| C[Execute Pipeline]
    B -->|No| D[Log Diagnostic]
    C --> E[Done]
```
""".trimIndent() + "\n\n",
                Color(0xFFF59E0B)
            ),
            SyntaxItem(
                "Mermaid Sequence Diagram",
                "Charts & Graphics",
                "Message interaction timeline with participants",
                "```mermaid",
                """
```mermaid
sequenceDiagram
    autonumber
    Client->>Server: POST /documents
    Server-->>Client: 200 OK (JSON AST)
```
""".trimIndent() + "\n\n",
                Color(0xFF38BDF8)
            ),

            // 7. Tables & Grids
            SyntaxItem(
                "GFM Data Table",
                "Tables & Links",
                "Styled table with left, center, and right alignments",
                "| Col 1 | Col 2 |",
                """
| Component | Status | Performance |
| :--- | :---: | ---: |
| KaTeX Math | Loaded | 120 FPS |
| Dagre Graph | Active | Native GPU |
| Chart.js | Ready | Canvas 2D |
""".trimIndent() + "\n\n",
                Color(0xFF38BDF8)
            ),
            SyntaxItem("WikiLink ([[Doc]])", "Tables & Links", "Bi-directional cross-document link", "[[Document]]", "[[⚡ Cyberpunk Maximalist Showcase]]", Color(0xFFC084FC)),
            SyntaxItem("Web Link ([text](url))", "Tables & Links", "Hyperlink to external URL", "[Title](url)", "[HyperMark Portal](https://ctan.org)", Color(0xFF60A5FA)),
            SyntaxItem("Image Embed (![alt](url))", "Tables & Links", "Inline image with caption", "![alt](url)", "![HyperMark Icon](https://placehold.co/600x200/090D14/00E5FF?text=HyperMark+IDE)", Color(0xFFFBBF24)),
            SyntaxItem("Footnote ([^1])", "Tables & Links", "Numbered footnote marker and citation", "[^1]", "Refer to footnote citation[^1].\n\n[^1]: Comprehensive CTAN package registry archive.\n", Color(0xFF94A3B8)),

            // 8. Math & CTAN
            SyntaxItem("Display Math Block ($$)", "Math & CTAN", "Centered multiline KaTeX equation", "$$ ... $$", "$$\n\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}\n$$\n\n", Color(0xFF34D399)),
            SyntaxItem("Dirac Braket Notation", "Math & CTAN", "Physics package quantum expectation", "\\braket{...}", "$$ \\braket{\\psi|\\hat{H}|\\psi} = E $$ \n\n", Color(0xFFC084FC)),
            SyntaxItem("Chemical Formula (mhchem)", "Math & CTAN", "Chemical reaction formula with arrows", "\\ce{...}", "$$ \\ce{6CO2 + 6H2O ->[light] C6H12O6 + 6O2} $$ \n\n", Color(0xFF34D399)),
            SyntaxItem("Matrix Equation", "Math & CTAN", "Parenthesized matrix algebra", "\\begin{pmatrix}", "$$\n\\begin{pmatrix}\n  \\cos\\theta & -\\sin\\theta \\\\\n  \\sin\\theta & \\cos\\theta\n\\end{pmatrix}\n$$\n\n", Color(0xFF00E5FF)),

            // 9. Multi-Language Code Blocks
            SyntaxItem("Kotlin Code Block", "Code Blocks", "Syntax-highlighted Kotlin snippet", "```kotlin", "```kotlin\nval flow = repository.observeActiveDocument()\n```\n\n", Color(0xFF818CF8)),
            SyntaxItem("Rust Code Block", "Code Blocks", "Syntax-highlighted Rust snippet", "```rust", "```rust\npub fn parse_ast(input: &str) -> Result<Ast, Error> {\n    Ok(Ast::new())\n}\n```\n\n", Color(0xFFF59E0B)),
            SyntaxItem("Python Code Block", "Code Blocks", "Syntax-highlighted Python snippet", "```python", "```python\ndef compute_dagre(nodes, edges):\n    return layout_hierarchical(nodes, edges)\n```\n\n", Color(0xFF38BDF8)),
            SyntaxItem("SQL Query Block", "Code Blocks", "Syntax-highlighted SQL query", "```sql", "```sql\nSELECT id, title, updated_at FROM documents ORDER BY updated_at DESC;\n```\n\n", Color(0xFF34D399))
        )
    }

    val categories = remember(allSyntaxItems) {
        listOf("All") + allSyntaxItems.map { it.category }.distinct()
    }

    val filteredItems = remember(searchQuery, selectedCategory, allSyntaxItems) {
        allSyntaxItems.filter { item ->
            val matchCategory = selectedCategory == null || selectedCategory == "All" || item.category == selectedCategory
            val matchQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1523),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(16.dp)
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
                            .background(Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFC084FC)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatPaint,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Markdown Flavors & Visual Syntax",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "GFM, Charts, Math, Cytoscape, Mermaid & Callouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search syntax (e.g. chart, callout, table, math)...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedContainerColor = Color(0xFF090D14),
                    unfocusedContainerColor = Color(0xFF090D14),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    val isSelected = (selectedCategory == null && cat == "All") || selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = if (cat == "All") null else cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E293B),
                            selectedLabelColor = Color(0xFF00E5FF),
                            containerColor = Color(0xFF0B101A),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Syntax Item List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .clickable {
                                onInsertSnippet(item.snippet)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF090E17)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = item.accentColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = item.category,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = item.accentColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = item.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    onInsertSnippet(item.snippet)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = item.accentColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
