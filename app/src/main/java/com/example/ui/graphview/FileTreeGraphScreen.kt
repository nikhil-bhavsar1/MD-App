package com.example.ui.graphview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarkdownDocument
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class GraphNodeType {
    ROOT_WORKSPACE,
    FOLDER,
    DOCUMENT,
    TAG
}

enum class TreeViewMode {
    NETWORK_GRAPH,
    FOLDER_TREE
}

data class GraphNode(
    val id: String,
    val title: String,
    val type: GraphNodeType,
    var x: Float,
    var y: Float,
    val color: Color,
    val documentId: String? = null,
    val size: Float = 28f
)

data class GraphEdge(
    val sourceId: String,
    val targetId: String,
    val label: String = "",
    val isDashed: Boolean = false,
    val color: Color = Color(0xFF38BDF8)
)

data class FolderCategory(
    val id: String,
    val name: String,
    val iconColor: Color,
    val docs: List<MarkdownDocument>
)

@Composable
fun FileTreeGraphScreen(
    documents: List<MarkdownDocument>,
    activeDocumentId: String,
    onSelectDocument: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(TreeViewMode.NETWORK_GRAPH) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var selectedNodeId by remember { mutableStateOf<String?>(activeDocumentId) }
    var draggedNodeId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showTagsFilter by remember { mutableStateOf(true) }
    var showFoldersFilter by remember { mutableStateOf(true) }
    var labelTextScale by remember { mutableFloatStateOf(1.0f) } // 1.0x, 1.3x, 1.6x

    val density = LocalDensity.current.density

    // --- Pulsing animation for the selected node ---
    val infiniteTransition = rememberInfiniteTransition(label = "selectedNodePulse")

    // 1. Expanding radar pulse ripple wave
    val pulseRingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRingScale"
    )
    val pulseRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRingAlpha"
    )

    // 2. Breathing halo radial glow
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlowAlpha"
    )

    // 3. Subtle breathing scale factor for node border and beacon dot
    val pulseNodeScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseNodeScale"
    )

    // Folder collapse states for Folder Tree mode
    val folderExpandedMap = remember {
        mutableStateMapOf(
            "notes" to true,
            "graphs" to true,
            "science" to true
        )
    }

    val nodes = remember { mutableStateListOf<GraphNode>() }
    val edges = remember { mutableStateListOf<GraphEdge>() }

    // Reusable single text paint object to prevent garbage collection churn during rendering
    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val dashEffect = remember {
        PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    }

    // Categorize documents for Folder Tree hierarchy
    val folderCategories = remember(documents) {
        val notes = mutableListOf<MarkdownDocument>()
        val graphs = mutableListOf<MarkdownDocument>()
        val science = mutableListOf<MarkdownDocument>()

        documents.forEach { doc ->
            when {
                doc.content.contains("cytoscape", ignoreCase = true) || doc.content.contains("mermaid", ignoreCase = true) ->
                    graphs.add(doc)
                doc.content.contains("amsmath", ignoreCase = true) || doc.content.contains("physics", ignoreCase = true) || doc.content.contains("\\usepackage", ignoreCase = true) ->
                    science.add(doc)
                else ->
                    notes.add(doc)
            }
        }

        listOf(
            FolderCategory("notes", "Notes & General Docs", Color(0xFF38BDF8), notes),
            FolderCategory("graphs", "Topology & Graph Specs", Color(0xFFC084FC), graphs),
            FolderCategory("science", "CTAN & LaTeX Math Lab", Color(0xFF34D399), science)
        )
    }

    fun buildGraph() {
        nodes.clear()
        edges.clear()

        // 1. Root Workspace node (Cyber Beacon)
        val rootNode = GraphNode(
            id = "workspace_root",
            title = "HyperMark Vault",
            type = GraphNodeType.ROOT_WORKSPACE,
            x = 450f,
            y = 220f,
            color = Color(0xFF00E5FF),
            size = 36f
        )
        nodes.add(rootNode)

        // 2. Folder Category nodes
        val folderNotes = GraphNode(
            id = "folder_notes",
            title = "📁 Notes & Docs",
            type = GraphNodeType.FOLDER,
            x = 220f,
            y = 390f,
            color = Color(0xFF38BDF8),
            size = 30f
        )
        val folderGraphs = GraphNode(
            id = "folder_graphs",
            title = "📁 Graphs & Topology",
            type = GraphNodeType.FOLDER,
            x = 450f,
            y = 430f,
            color = Color(0xFFC084FC),
            size = 30f
        )
        val folderScience = GraphNode(
            id = "folder_science",
            title = "📁 CTAN & Math Lab",
            type = GraphNodeType.FOLDER,
            x = 680f,
            y = 390f,
            color = Color(0xFF34D399),
            size = 30f
        )

        if (showFoldersFilter) {
            nodes.addAll(listOf(folderNotes, folderGraphs, folderScience))
            edges.add(GraphEdge(rootNode.id, folderNotes.id, "contains", color = Color(0xFF38BDF8).copy(alpha = 0.5f)))
            edges.add(GraphEdge(rootNode.id, folderGraphs.id, "contains", color = Color(0xFFC084FC).copy(alpha = 0.5f)))
            edges.add(GraphEdge(rootNode.id, folderScience.id, "contains", color = Color(0xFF34D399).copy(alpha = 0.5f)))
        }

        // 3. Document Nodes positioned in orbital rings with clean separation
        documents.forEachIndexed { idx, doc ->
            val angle = (idx.toFloat() / documents.size.coerceAtLeast(1)) * 2 * Math.PI.toFloat()
            val dist = 330f + (idx % 2) * 90f
            val px = 450f + dist * cos(angle)
            val py = 540f + dist * sin(angle)

            val docNode = GraphNode(
                id = doc.id,
                title = doc.title,
                type = GraphNodeType.DOCUMENT,
                x = px,
                y = py,
                color = when {
                    doc.content.contains("cytoscape", ignoreCase = true) || doc.content.contains("mermaid", ignoreCase = true) -> Color(0xFFC084FC)
                    doc.content.contains("amsmath", ignoreCase = true) || doc.content.contains("physics", ignoreCase = true) -> Color(0xFF34D399)
                    else -> Color(0xFF38BDF8)
                },
                documentId = doc.id,
                size = if (doc.id == activeDocumentId) 32f else 28f
            )
            nodes.add(docNode)

            if (showFoldersFilter) {
                val targetFolder = when {
                    doc.content.contains("cytoscape", ignoreCase = true) || doc.content.contains("mermaid", ignoreCase = true) -> folderGraphs.id
                    doc.content.contains("amsmath", ignoreCase = true) || doc.content.contains("physics", ignoreCase = true) -> folderScience.id
                    else -> folderNotes.id
                }
                edges.add(GraphEdge(targetFolder, doc.id, "file", color = docNode.color.copy(alpha = 0.4f)))
            } else {
                edges.add(GraphEdge(rootNode.id, doc.id, "file", color = docNode.color.copy(alpha = 0.4f)))
            }

            // Cross-document links detection (wiki links or direct name mentions)
            documents.forEach { otherDoc ->
                if (otherDoc.id != doc.id) {
                    if (doc.content.contains(otherDoc.title, ignoreCase = true) || doc.content.contains("[[${otherDoc.title}]]", ignoreCase = true)) {
                        edges.add(GraphEdge(doc.id, otherDoc.id, "links", isDashed = false, color = Color(0xFFF472B6)))
                    }
                }
            }
        }

        // 4. Tag nodes
        if (showTagsFilter) {
            val allTags = setOf("latex", "cytoscape", "mermaid", "ctan", "gfm")
            allTags.forEachIndexed { i, tag ->
                val tx = 180f + i * 140f
                val ty = 770f + (i % 2) * 50f
                val tagNode = GraphNode(
                    id = "tag_$tag",
                    title = "#$tag",
                    type = GraphNodeType.TAG,
                    x = tx,
                    y = ty,
                    color = Color(0xFFFBBF24),
                    size = 22f
                )
                nodes.add(tagNode)

                documents.forEach { doc ->
                    if (doc.content.contains(tag, ignoreCase = true)) {
                        edges.add(GraphEdge(doc.id, tagNode.id, "tag", isDashed = true, color = Color(0xFFFBBF24).copy(alpha = 0.5f)))
                    }
                }
            }
        }
    }

    LaunchedEffect(documents, showTagsFilter, showFoldersFilter) {
        buildGraph()
    }

    // Fast set of connected node IDs for highlighting
    val highlightedNodeIds = remember(selectedNodeId, edges) {
        val set = mutableSetOf<String>()
        selectedNodeId?.let { selId ->
            set.add(selId)
            edges.forEach { edge ->
                if (edge.sourceId == selId) set.add(edge.targetId)
                if (edge.targetId == selId) set.add(edge.sourceId)
            }
        }
        set
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.35f, 3.8f)
        offset += panChange
    }

    // Dark translucent background container
    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B13))
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFF070B13)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (viewMode == TreeViewMode.NETWORK_GRAPH) {
                // Interactive 2D Hardware Canvas for Topology Graph
                val bezierPath = remember { Path() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = transformableState)
                        .pointerInput(nodes) {
                            detectDragGestures(
                                onDragStart = { pointerOffset ->
                                    val canvasX = (pointerOffset.x - offset.x) / scale
                                    val canvasY = (pointerOffset.y - offset.y) / scale

                                    val hit = nodes.findLast { node ->
                                        val dx = node.x - canvasX
                                        val dy = node.y - canvasY
                                        hypot(dx, dy) <= node.size + 18f
                                    }

                                    if (hit != null) {
                                        draggedNodeId = hit.id
                                        selectedNodeId = hit.id
                                    } else {
                                        selectedNodeId = null
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggedNodeId?.let { id ->
                                        val node = nodes.find { it.id == id }
                                        if (node != null) {
                                            node.x += dragAmount.x / scale
                                            node.y += dragAmount.y / scale
                                        }
                                    }
                                },
                                onDragEnd = { draggedNodeId = null },
                                onDragCancel = { draggedNodeId = null }
                            )
                        }
                ) {
                    // Minimal-overhead grid pattern
                    val gridSpacing = 60f * scale
                    val gridOffsetX = offset.x % gridSpacing
                    val gridOffsetY = offset.y % gridSpacing

                    var x = gridOffsetX
                    while (x < size.width) {
                        drawLine(
                            color = Color(0xFF0E1626),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += gridSpacing
                    }

                    var y = gridOffsetY
                    while (y < size.height) {
                        drawLine(
                            color = Color(0xFF0E1626),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += gridSpacing
                    }

                    // Fast map for O(1) node lookup per edge
                    val nodeMap = nodes.associateBy { it.id }

                    // Draw Smooth Curved Bézier Edges (Obsidian / Cosmograph style)
                    edges.forEach { edge ->
                        val src = nodeMap[edge.sourceId]
                        val dst = nodeMap[edge.targetId]
                        if (src != null && dst != null) {
                            val isHighlighted = selectedNodeId != null &&
                                (edge.sourceId == selectedNodeId || edge.targetId == selectedNodeId)
                            val isDimmed = selectedNodeId != null && !isHighlighted

                            val p1 = Offset(src.x * scale + offset.x, src.y * scale + offset.y)
                            val p2 = Offset(dst.x * scale + offset.x, dst.y * scale + offset.y)

                            val edgeColor = when {
                                isHighlighted -> Color(0xFF00E5FF)
                                isDimmed -> edge.color.copy(alpha = 0.06f)
                                else -> edge.color.copy(alpha = 0.35f)
                            }

                            val strokeWidth = if (isHighlighted) 3.5f * scale else 1.8f * scale

                            // Curvature offset for organic graph curves
                            val midX = (p1.x + p2.x) / 2f
                            val midY = (p1.y + p2.y) / 2f
                            val dx = p2.x - p1.x
                            val dy = p2.y - p1.y
                            val normalX = -dy * 0.12f
                            val normalY = dx * 0.12f
                            val ctrlX = midX + normalX
                            val ctrlY = midY + normalY

                            bezierPath.reset()
                            bezierPath.moveTo(p1.x, p1.y)
                            bezierPath.quadraticTo(ctrlX, ctrlY, p2.x, p2.y)

                            drawPath(
                                path = bezierPath,
                                color = edgeColor,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    pathEffect = if (edge.isDashed) dashEffect else null
                                )
                            )
                        }
                    }

                    // Draw Nodes with Soft Radial Halos & Inner Glyphs
                    nodes.forEach { node ->
                        val screenPos = Offset(node.x * scale + offset.x, node.y * scale + offset.y)
                        val isSelected = node.id == selectedNodeId
                        val isConnected = highlightedNodeIds.contains(node.id)
                        val isDimmed = selectedNodeId != null && !isConnected
                        val isActiveDoc = node.documentId == activeDocumentId

                        val baseRadius = (node.size * scale).coerceAtLeast(10f)
                        val radius = if (isSelected) baseRadius * pulseNodeScale else baseRadius
                        val alpha = if (isDimmed) 0.18f else 1f

                        // --- Pulsing animation on selected node ---
                        if (isSelected) {
                            // 1. Expanding radar pulse ripple wave
                            val rippleRadius = baseRadius * pulseRingScale
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = pulseRingAlpha),
                                radius = rippleRadius,
                                center = screenPos,
                                style = Stroke(width = 2.5f * scale)
                            )

                            // 2. Secondary softer ripple
                            val secondaryRingScale = 1.0f + (pulseRingScale - 1.0f) * 0.6f
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = pulseRingAlpha * 0.5f),
                                radius = baseRadius * secondaryRingScale,
                                center = screenPos,
                                style = Stroke(width = 1.8f * scale)
                            )

                            // 3. Breathing outer radial glow
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(
                                        Color(0xFF00E5FF).copy(alpha = pulseGlowAlpha),
                                        Color(0xFF00E5FF).copy(alpha = pulseGlowAlpha * 0.25f),
                                        Color.Transparent
                                    ),
                                    center = screenPos,
                                    radius = baseRadius * 3.0f * pulseNodeScale
                                ),
                                radius = baseRadius * 3.0f * pulseNodeScale,
                                center = screenPos
                            )
                        } else if (isActiveDoc) {
                            // Subtle steady cyan glow for current active document
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.45f * alpha),
                                        Color.Transparent
                                    ),
                                    center = screenPos,
                                    radius = radius * 2.2f
                                ),
                                radius = radius * 2.2f,
                                center = screenPos
                            )
                        }

                        // Node solid background
                        drawCircle(
                            color = if (isSelected) Color(0xFF16233B) else Color(0xFF0D1424).copy(alpha = alpha),
                            radius = radius,
                            center = screenPos
                        )

                        // Node glowing border stroke
                        drawCircle(
                            color = if (isSelected) Color(0xFF00E5FF) else node.color.copy(alpha = alpha),
                            radius = radius,
                            center = screenPos,
                            style = Stroke(width = if (isSelected) 4f * scale else 2.2f * scale)
                        )

                        // Core beacon dot (pulsing slightly if selected)
                        val coreRadius = if (isSelected) radius * 0.45f else radius * 0.38f
                        drawCircle(
                            color = if (isSelected) Color(0xFF00E5FF) else node.color.copy(alpha = alpha),
                            radius = coreRadius,
                            center = screenPos
                        )

                        // --- Improved Density-Aware Font Sizing & High-Contrast Readability Capsule ---
                        val matchesSearch = searchQuery.isNotBlank() &&
                            node.title.contains(searchQuery, ignoreCase = true)

                        // Density-aware font size calculation with user scale multiplier
                        val baseTextPx = 13.5f * density * labelTextScale
                        val scaledTextSize = (baseTextPx * (0.85f + (scale - 1f) * 0.35f))
                            .coerceIn(12f * density * labelTextScale, 28f * density * labelTextScale)

                        textPaint.textSize = scaledTextSize
                        textPaint.isFakeBoldText = isSelected || matchesSearch || isActiveDoc

                        val textWidth = textPaint.measureText(node.title)
                        val textHeight = scaledTextSize

                        val pillPaddingX = 8f * density * labelTextScale
                        val pillPaddingY = 4f * density * labelTextScale
                        val textCenterY = screenPos.y + radius + (18f * scale)

                        val pillLeft = screenPos.x - (textWidth / 2f) - pillPaddingX
                        val pillTop = textCenterY - textHeight
                        val pillRight = screenPos.x + (textWidth / 2f) + pillPaddingX
                        val pillBottom = textCenterY + (pillPaddingY * 1.5f)

                        // 1. Draw rounded capsule pill background for flawless readability over grid & edges
                        val pillAlpha = if (isDimmed) 0.3f else 0.92f
                        val pillBgColor = when {
                            isSelected -> Color(0xF2091428)
                            matchesSearch -> Color(0xF20F1E38)
                            else -> Color(0xE6080E1A).copy(alpha = pillAlpha)
                        }

                        drawRoundRect(
                            color = pillBgColor,
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(pillRight - pillLeft, pillBottom - pillTop),
                            cornerRadius = CornerRadius(6f * density, 6f * density)
                        )

                        // 2. Capsule pill border stroke
                        val pillBorderColor = when {
                            isSelected -> Color(0xFF00E5FF)
                            matchesSearch -> Color(0xFF38BDF8)
                            isActiveDoc -> Color(0xFF00E5FF).copy(alpha = 0.5f)
                            isDimmed -> Color(0xFF1E2A3F).copy(alpha = 0.2f)
                            else -> Color(0xFF1E2A3F).copy(alpha = 0.85f)
                        }

                        drawRoundRect(
                            color = pillBorderColor,
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(pillRight - pillLeft, pillBottom - pillTop),
                            cornerRadius = CornerRadius(6f * density, 6f * density),
                            style = Stroke(width = if (isSelected || matchesSearch) 1.5f * density else 1f * density)
                        )

                        // 3. Draw text with high contrast color
                        val textColor = when {
                            matchesSearch -> android.graphics.Color.parseColor("#00E5FF")
                            isSelected -> android.graphics.Color.WHITE
                            isDimmed -> android.graphics.Color.argb(55, 180, 190, 205)
                            isActiveDoc -> android.graphics.Color.parseColor("#00E5FF")
                            node.type == GraphNodeType.TAG -> android.graphics.Color.parseColor("#FBBF24")
                            else -> android.graphics.Color.parseColor("#F1F5F9")
                        }

                        textPaint.color = textColor

                        drawContext.canvas.nativeCanvas.drawText(
                            node.title,
                            screenPos.x,
                            textCenterY,
                            textPaint
                        )
                    }
                }
            } else {
                // Collapsible Hierarchical Folder Tree View
                val filteredCategories = remember(folderCategories, searchQuery) {
                    if (searchQuery.isBlank()) folderCategories else {
                        folderCategories.map { cat ->
                            cat.copy(docs = cat.docs.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.content.contains(searchQuery, ignoreCase = true)
                            })
                        }.filter { it.docs.isNotEmpty() }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 130.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { category ->
                        val isExpanded = folderExpandedMap[category.id] ?: true

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xCC0D1526)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A3F))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                // Folder Header Row (Expandable)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            folderExpandedMap[category.id] = !isExpanded
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "arrow")
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = category.iconColor,
                                            modifier = Modifier.size(20.dp).rotate(rotation)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = category.iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = category.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = category.iconColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${category.docs.size} files",
                                            color = category.iconColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (category.docs.isEmpty()) {
                                            Text(
                                                text = "No files in this folder",
                                                color = Color(0xFF64748B),
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 4.dp)
                                            )
                                        } else {
                                            category.docs.forEach { doc ->
                                                FolderTreeFileItem(
                                                    document = doc,
                                                    isActive = doc.id == activeDocumentId,
                                                    accentColor = category.iconColor,
                                                    onOpenInEditor = {
                                                        onSelectDocument(doc.id)
                                                        onClose()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Bar HUD (Dark Translucent Glassmorphism)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xF0070B13), Color(0xCC070B13), Color.Transparent)
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC111827))
                            .border(1.dp, Color(0xFF1F2A3F), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Editor",
                            tint = Color(0xFF00E5FF)
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Filter files & tags...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF1E2A3F),
                            focusedContainerColor = Color(0xCC0D1424),
                            unfocusedContainerColor = Color(0xCC0D1424),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // View Mode Switcher Pill (Graph vs Tree)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xCC0D1424),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A3F))
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            IconButton(
                                onClick = { viewMode = TreeViewMode.NETWORK_GRAPH },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewMode == TreeViewMode.NETWORK_GRAPH) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = "Network Graph",
                                    tint = if (viewMode == TreeViewMode.NETWORK_GRAPH) Color(0xFF00E5FF) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewMode = TreeViewMode.FOLDER_TREE },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewMode == TreeViewMode.FOLDER_TREE) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = "Folder Tree",
                                    tint = if (viewMode == TreeViewMode.FOLDER_TREE) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (viewMode == TreeViewMode.NETWORK_GRAPH) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showFoldersFilter,
                            onClick = { showFoldersFilter = !showFoldersFilter },
                            label = { Text("📁 Folder Nodes", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xCC1E293B),
                                selectedLabelColor = Color(0xFF38BDF8),
                                containerColor = Color(0x990D131F),
                                labelColor = Color(0xFF64748B)
                            )
                        )

                        FilterChip(
                            selected = showTagsFilter,
                            onClick = { showTagsFilter = !showTagsFilter },
                            label = { Text("#️⃣ Tag Nodes", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xCC261D10),
                                selectedLabelColor = Color(0xFFFBBF24),
                                containerColor = Color(0x990D131F),
                                labelColor = Color(0xFF64748B)
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x990D131F),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A3F)),
                            modifier = Modifier.clickable {
                                labelTextScale = when (labelTextScale) {
                                    1.0f -> 1.3f
                                    1.3f -> 1.6f
                                    else -> 1.0f
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Text: ${(labelTextScale * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Floating Zoom & Layout Action Controls (for Graph mode)
            if (viewMode == TreeViewMode.NETWORK_GRAPH) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingGraphAction(
                        icon = Icons.Default.Add,
                        desc = "Zoom In",
                        onClick = { scale = (scale * 1.25f).coerceAtMost(3.8f) }
                    )
                    FloatingGraphAction(
                        icon = Icons.Default.Remove,
                        desc = "Zoom Out",
                        onClick = { scale = (scale / 1.25f).coerceAtLeast(0.35f) }
                    )
                    FloatingGraphAction(
                        icon = Icons.Default.FormatSize,
                        desc = "Font Size: ${(labelTextScale * 100).toInt()}%",
                        onClick = {
                            labelTextScale = when (labelTextScale) {
                                1.0f -> 1.3f
                                1.3f -> 1.6f
                                else -> 1.0f
                            }
                        }
                    )
                    FloatingGraphAction(
                        icon = Icons.Default.CenterFocusStrong,
                        desc = "Fit View",
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )
                    FloatingGraphAction(
                        icon = Icons.Default.Refresh,
                        desc = "Reset Layout",
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                            buildGraph()
                        }
                    )
                }
            }

            // Bottom Inspector Card for selected node in Graph Mode
            val selectedDoc = remember(selectedNodeId, documents) {
                documents.find { it.id == selectedNodeId }
            }

            AnimatedVisibility(
                visible = viewMode == TreeViewMode.NETWORK_GRAPH && selectedDoc != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (selectedDoc != null) {
                    val connectedDocs = remember(selectedDoc.id, edges, documents) {
                        val ids = edges.filter { it.sourceId == selectedDoc.id || it.targetId == selectedDoc.id }
                            .map { if (it.sourceId == selectedDoc.id) it.targetId else it.sourceId }
                        documents.filter { ids.contains(it.id) }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00E5FF).copy(alpha = 0.7f), Color(0xFFC084FC).copy(alpha = 0.7f))
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF20B111F)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Row with Title and dedicated non-garbled Open in Editor Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E5FF))
                                    )
                                    Text(
                                        text = selectedDoc.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Sleek, non-wrapping Open in Editor action button
                                Button(
                                    onClick = {
                                        onSelectDocument(selectedDoc.id)
                                        onClose()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00E5FF),
                                        contentColor = Color(0xFF050B14)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Open in Editor",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Metadata Badges Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0x991E293B)
                                ) {
                                    Text(
                                        text = "${selectedDoc.content.length} chars",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                val ctanList = selectedDoc.getInstalledPackageList()
                                if (ctanList.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0x3338BDF8)
                                    ) {
                                        Text(
                                            text = "CTAN: ${ctanList.joinToString(", ")}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0x33C084FC)
                                ) {
                                    Text(
                                        text = "Linked: ${connectedDocs.size} docs",
                                        fontSize = 11.sp,
                                        color = Color(0xFFC084FC),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Quick preview snippet
                            val snippet = remember(selectedDoc.content) {
                                selectedDoc.content.lines().firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                                    ?: selectedDoc.content.take(80)
                            }
                            if (snippet.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = snippet,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (connectedDocs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(connectedDocs) { conn ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xCC1E293B),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier.clickable {
                                                selectedNodeId = conn.id
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(text = "🔗", fontSize = 10.sp)
                                                Text(
                                                    text = conn.title,
                                                    color = Color(0xFFCBD5E1),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTreeFileItem(
    document: MarkdownDocument,
    isActive: Boolean,
    accentColor: Color,
    onOpenInEditor: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xCC162238) else Color(0x990F1626)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = document.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val words = if (document.content.isBlank()) 0 else document.content.trim().split(Regex("\\s+")).size
                    Text(
                        text = "${document.content.length} chars • $words words",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Unambiguous, elegant Open in Editor Button
            Button(
                onClick = onOpenInEditor,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color(0xFF00E5FF) else Color(0xFF1E293B),
                    contentColor = if (isActive) Color(0xFF050B14) else Color(0xFF00E5FF)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Open in Editor",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun FloatingGraphAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xCC0E1726))
            .border(1.dp, Color(0xFF1F2A3F), CircleShape)
    ) {
        Icon(imageVector = icon, contentDescription = desc, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
    }
}
