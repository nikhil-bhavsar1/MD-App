package com.example.ui.graphs

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Schema
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

data class GraphTemplate(
    val title: String,
    val type: String, // "cytoscape" or "mermaid"
    val description: String,
    val snippet: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphHelperDialog(
    onInsertGraphSnippet: (snippet: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by remember { mutableStateOf<String?>(null) } // null = All, "cytoscape", "mermaid"

    val templates = remember {
        listOf(
            GraphTemplate(
                title = "Cytoscape Dagre: Microservice Topology",
                type = "cytoscape",
                description = "Hierarchical directed acyclic graph computed by the Dagre layout engine.",
                snippet = """
```cytoscape
layout: dagre
nodes:
  - id: ingress, label: "Cloud Ingress", color: "#38BDF8"
  - id: auth, label: "Auth Validator", color: "#C084FC"
  - id: worker_a, label: "Worker Alpha", color: "#34D399"
  - id: worker_b, label: "Worker Beta", color: "#FBBF24"
  - id: db, label: "Distributed DB", color: "#00E5FF"
edges:
  - source: ingress, target: auth, label: "HTTPS"
  - source: auth, target: worker_a, label: "Dispatch"
  - source: auth, target: worker_b, label: "Dispatch"
  - source: worker_a, target: db, label: "Write"
  - source: worker_b, target: db, label: "Read"
```
""".trimIndent()
            ),
            GraphTemplate(
                title = "Cytoscape Dagre: ML Pipeline",
                type = "cytoscape",
                description = "Multi-stage machine learning inference and ETL DAG.",
                snippet = """
```cytoscape
layout: dagre
nodes:
  - id: raw, label: "Raw Dataset", color: "#F472B6"
  - id: etl, label: "Token Preprocess", color: "#818CF8"
  - id: emb, label: "Vector Embed", color: "#38BDF8"
  - id: model, label: "Transformer LLM", color: "#00E5FF"
  - id: loss, label: "Softmax Loss", color: "#34D399"
edges:
  - source: raw, target: etl, label: "ETL"
  - source: etl, target: emb, label: "Tokens"
  - source: emb, target: model, label: "Embeddings"
  - source: model, target: loss, label: "Logits"
```
""".trimIndent()
            ),
            GraphTemplate(
                title = "Mermaid: Flowchart (TD)",
                type = "mermaid",
                description = "Standard top-down flowchart with logic decisions and styles.",
                snippet = """
```mermaid
graph TD
    Start([User Input]) --> Parse{Valid?}
    Parse -->|Yes| Exec[Execute Core Task]
    Parse -->|No| Alert[Show Warning Alert]
    Exec --> Cache[(Store Cache)]
    Cache --> Finish([Render Output])
    Alert --> Start
    style Start fill:#1E293B,stroke:#38BDF8,stroke-width:2px,color:#FFF
    style Exec fill:#064E3B,stroke:#34D399,stroke-width:2px,color:#FFF
    style Finish fill:#312E81,stroke:#C084FC,stroke-width:2px,color:#FFF
```
""".trimIndent()
            ),
            GraphTemplate(
                title = "Mermaid: Sequence Diagram",
                type = "mermaid",
                description = "Actor and component messaging over time with auto-numbering.",
                snippet = """
```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant IDE as HyperMark Editor
    participant Engine as Dagre Engine
    participant Screen as Canvas Display

    Dev->>IDE: Update Cytoscape Block
    IDE->>Engine: Run Dagre Layout
    Engine-->>Screen: Reposition Nodes & Edges
    Screen-->>Dev: Interactive Graph
```
""".trimIndent()
            ),
            GraphTemplate(
                title = "Mermaid: Mindmap",
                type = "mermaid",
                description = "Radial mindmap diagram for brainstorming and knowledge trees.",
                snippet = """
```mermaid
mindmap
  root((HyperMark))
    Markdown
      GFM
      CommonMark
      Admonitions
    Math Engine
      KaTeX
      CTAN Packages
      AMS Symbols
    Visuals
      Cytoscape + Dagre
      Mermaid Flowcharts
      Prism Highlighting
```
""".trimIndent()
            ),
            GraphTemplate(
                title = "Mermaid: Git Graph",
                type = "mermaid",
                description = "Interactive branch and commit visualization.",
                snippet = """
```mermaid
gitGraph
   commit id: "Init Base"
   commit id: "Split View"
   branch feature-ctan
   checkout feature-ctan
   commit id: "CTAN Loader"
   commit id: "Math Macros"
   checkout main
   merge feature-ctan
   commit id: "Release 1.0"
```
""".trimIndent()
            )
        )
    }

    val filteredTemplates = remember(selectedType) {
        if (selectedType == null) templates else templates.filter { it.type == selectedType }
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
                .fillMaxHeight(0.85f)
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
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFFF472B6)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Graph Engine",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Graph & Diagram Templates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Mermaid + Cytoscape Dagre Interactive Graphs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC084FC)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All Diagrams (${templates.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E293B),
                            selectedLabelColor = Color(0xFF00E5FF),
                            containerColor = Color(0xFF111827),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedType == "cytoscape",
                        onClick = { selectedType = if (selectedType == "cytoscape") null else "cytoscape" },
                        label = { Text("🕸️ Cytoscape + Dagre") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFC084FC).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFC084FC),
                            containerColor = Color(0xFF111827),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedType == "mermaid",
                        onClick = { selectedType = if (selectedType == "mermaid") null else "mermaid" },
                        label = { Text("📊 Mermaid JS") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFF59E0B),
                            containerColor = Color(0xFF111827),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Templates List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTemplates) { tmpl ->
                    val isCyto = tmpl.type == "cytoscape"
                    val accentColor = if (isCyto) Color(0xFFC084FC) else Color(0xFFF59E0B)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B101A))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tmpl.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isCyto) "Cytoscape + Dagre" else "Mermaid",
                                        color = accentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = tmpl.description,
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Preview Code Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF060910), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF172033), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = tmpl.snippet.lines().take(5).joinToString("\n") + "\n...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    onInsertGraphSnippet("\n\n" + tmpl.snippet + "\n\n")
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert into Document", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
