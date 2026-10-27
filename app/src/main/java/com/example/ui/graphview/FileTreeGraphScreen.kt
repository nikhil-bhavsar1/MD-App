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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

enum class GraphNodeType {
    ROOT_WORKSPACE,
    FOLDER,
    DOCUMENT
}

enum class TreeViewMode {
    NETWORK_GRAPH,
    FOLDER_TREE
}

enum class EdgeType {
    TRUNK,     // Root to Sub-folder
    BRANCH,    // Folder to child Document or Sub-folder
    WIKILINK   // Cross-document reference [[...]]
}

data class GraphFolder(
    val id: String,
    val name: String,
    val parentId: String = "root",
    val color: Color
)

data class GraphNode(
    val id: String,
    val title: String,
    val type: GraphNodeType,
    var x: Float,
    var y: Float,
    val color: Color,
    val documentId: String? = null,
    val folderId: String? = null,
    val size: Float = 28f
)

data class GraphEdge(
    val sourceId: String,
    val targetId: String,
    val type: EdgeType = EdgeType.BRANCH,
    val label: String = "",
    val isDashed: Boolean = false,
    val color: Color = Color(0xFF38BDF8)
)

// Clean palette of distinct colors for custom folders
val FOLDER_PALETTE = listOf(
    Color(0xFF38BDF8), // Sky Blue
    Color(0xFFA78BFA), // Violet
    Color(0xFF34D399), // Emerald
    Color(0xFFF472B6), // Rose
    Color(0xFFFBBF24), // Amber
    Color(0xFF818CF8)  // Indigo
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
    var labelTextScale by remember { mutableFloatStateOf(1.0f) }

    // Dialog state for adding a new folder
    var isAddFolderDialogOpen by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newFolderParentId by remember { mutableStateOf("root") }
    var newFolderColor by remember { mutableStateOf(FOLDER_PALETTE[0]) }

    // Dialog state for moving a document between folders
    var movingDocument by remember { mutableStateOf<MarkdownDocument?>(null) }

    val density = LocalDensity.current.density

    // --- Dynamic Folder State ---
    // User starts with 0 folders by default! Web expands organically as folders are added.
    val folders = remember { mutableStateListOf<GraphFolder>() }

    // Mapping of document ID -> folder ID (defaults to "root" when no folder is assigned)
    val docFolderMap = remember { mutableStateMapOf<String, String>() }

    // Folder collapse states for Folder Tree view mode
    val folderExpandedMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            put("root", true)
        }
    }

    // Initialize document-to-folder mapping: if not assigned to an existing folder, document resides in "root"
    LaunchedEffect(documents, folders.size) {
        val availableFolderIds = folders.map { it.id }.toSet()
        documents.forEach { doc ->
            val currentAssigned = docFolderMap[doc.id]
            if (currentAssigned == null || !availableFolderIds.contains(currentAssigned)) {
                docFolderMap[doc.id] = "root"
            }
        }
    }

    // --- Graph Node and Edge Collections ---
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

    // --- Build Clean Hierarchical Web Graph ---
    // Center: Root Vault -> Sub-folders (if added) -> Documents & [[WikiLinks]]
    fun buildGraph() {
        nodes.clear()
        edges.clear()

        val rootX = 500f
        val rootY = 420f

        // 1. Root Vault Anchor Node (Center Beacon)
        val rootNode = GraphNode(
            id = "root",
            title = "Root Vault",
            type = GraphNodeType.ROOT_WORKSPACE,
            x = rootX,
            y = rootY,
            color = Color(0xFF00E5FF),
            size = 38f
        )
        nodes.add(rootNode)

        val folderPositionMap = mutableMapOf<String, Pair<Float, Float>>()

        // 2. Distribute direct sub-folders around Root (ONLY if folders exist)
        val directSubFolders = folders.filter { it.parentId == "root" }
        if (directSubFolders.isNotEmpty()) {
            val numSubFolders = directSubFolders.size
            val folderRadius = 260f

            directSubFolders.forEachIndexed { i, folder ->
                val angle = (2.0 * Math.PI * i / numSubFolders) - (Math.PI / 2.0)
                val fx = rootX + (folderRadius * cos(angle)).toFloat()
                val fy = rootY + (folderRadius * sin(angle)).toFloat()

                folderPositionMap[folder.id] = Pair(fx, fy)

                val folderNode = GraphNode(
                    id = folder.id,
                    title = "📁 ${folder.name}",
                    type = GraphNodeType.FOLDER,
                    x = fx,
                    y = fy,
                    color = folder.color,
                    folderId = folder.id,
                    size = 30f
                )
                nodes.add(folderNode)

                // Trunk chain from Root to Sub-folder
                edges.add(
                    GraphEdge(
                        sourceId = rootNode.id,
                        targetId = folder.id,
                        type = EdgeType.TRUNK,
                        label = "root",
                        color = Color(0xFF00E5FF)
                    )
                )
            }
        }

        // 3. Nested sub-folders (sub-folders branching off other sub-folders)
        val nestedFolders = folders.filter { it.parentId != "root" }
        nestedFolders.forEachIndexed { idx, folder ->
            val parentPos = folderPositionMap[folder.parentId] ?: Pair(rootX, rootY)
            val parentAngle = atan2(parentPos.second - rootY, parentPos.first - rootX)
            val subDist = 180f
            val subAngle = parentAngle + ((idx % 3) - 1) * 0.45f
            val sfx = parentPos.first + (subDist * cos(subAngle)).toFloat()
            val sfy = parentPos.second + (subDist * sin(subAngle)).toFloat()

            folderPositionMap[folder.id] = Pair(sfx, sfy)

            val folderNode = GraphNode(
                id = folder.id,
                title = "📁 ${folder.name}",
                type = GraphNodeType.FOLDER,
                x = sfx,
                y = sfy,
                color = folder.color,
                folderId = folder.id,
                size = 28f
            )
            nodes.add(folderNode)

            edges.add(
                GraphEdge(
                    sourceId = folder.parentId,
                    targetId = folder.id,
                    type = EdgeType.BRANCH,
                    color = folder.color
                )
            )
        }

        // 4. Position Documents directly in Root Vault
        val availableFolderIds = folders.map { it.id }.toSet()
        val rootDocs = documents.filter { doc ->
            val fId = docFolderMap[doc.id]
            fId == null || fId == "root" || !availableFolderIds.contains(fId)
        }

        if (rootDocs.isNotEmpty()) {
            val count = rootDocs.size
            val docBaseRadius = if (directSubFolders.isEmpty()) 185f else 140f

            rootDocs.forEachIndexed { j, doc ->
                val angle = (2.0 * Math.PI * j / count) - (Math.PI / 2.0)
                val stagger = if (j % 2 == 1 && count > 2) 32f else 0f
                val dx = rootX + ((docBaseRadius + stagger) * cos(angle)).toFloat()
                val dy = rootY + ((docBaseRadius + stagger) * sin(angle)).toFloat()

                val isDocActive = (doc.id == activeDocumentId)
                val docNode = GraphNode(
                    id = doc.id,
                    title = doc.title,
                    type = GraphNodeType.DOCUMENT,
                    x = dx,
                    y = dy,
                    color = Color(0xFF38BDF8),
                    documentId = doc.id,
                    size = if (isDocActive) 30f else 26f
                )
                nodes.add(docNode)

                // Branch connection from Root Vault to Document
                edges.add(
                    GraphEdge(
                        sourceId = rootNode.id,
                        targetId = doc.id,
                        type = EdgeType.BRANCH,
                        color = Color(0xFF38BDF8).copy(alpha = 0.55f)
                    )
                )
            }
        }

        // 5. Position Documents inside each Sub-folder (Clean outward radial fan)
        folders.forEach { folder ->
            val folderPos = folderPositionMap[folder.id] ?: Pair(rootX, rootY)
            val fx = folderPos.first
            val fy = folderPos.second

            val outwardAngle = atan2(fy - rootY, fx - rootX)
            val folderDocs = documents.filter { docFolderMap[it.id] == folder.id }
            val count = folderDocs.size

            if (count > 0) {
                val fanSpan = min(Math.PI.toFloat() * 0.85f, (count - 1).coerceAtLeast(1) * 0.38f)
                val docBaseRadius = 155f

                folderDocs.forEachIndexed { j, doc ->
                    val docAngle = if (count == 1) {
                        outwardAngle
                    } else {
                        outwardAngle - (fanSpan / 2f) + (fanSpan * j / (count - 1).toFloat())
                    }

                    val stagger = if (j % 2 == 1) 34f else 0f
                    val dx = fx + ((docBaseRadius + stagger) * cos(docAngle)).toFloat()
                    val dy = fy + ((docBaseRadius + stagger) * sin(docAngle)).toFloat()

                    val isDocActive = (doc.id == activeDocumentId)
                    val docNode = GraphNode(
                        id = doc.id,
                        title = doc.title,
                        type = GraphNodeType.DOCUMENT,
                        x = dx,
                        y = dy,
                        color = folder.color,
                        documentId = doc.id,
                        size = if (isDocActive) 30f else 26f
                    )
                    nodes.add(docNode)

                    edges.add(
                        GraphEdge(
                            sourceId = folder.id,
                            targetId = doc.id,
                            type = EdgeType.BRANCH,
                            color = folder.color.copy(alpha = 0.55f)
                        )
                    )
                }
            }
        }

        // 6. Cross-document [[WikiLinks]] & backlinks (Knowledge Web)
        documents.forEach { doc ->
            documents.forEach { otherDoc ->
                if (otherDoc.id != doc.id) {
                    val mentionsLink = doc.content.contains("[[${otherDoc.title}]]", ignoreCase = true) ||
                        (otherDoc.title.length >= 4 && doc.content.contains(otherDoc.title, ignoreCase = true))
                    if (mentionsLink) {
                        val alreadyHasEdge = edges.any {
                            it.type == EdgeType.WIKILINK &&
                                ((it.sourceId == doc.id && it.targetId == otherDoc.id) ||
                                    (it.sourceId == otherDoc.id && it.targetId == doc.id))
                        }
                        if (!alreadyHasEdge) {
                            edges.add(
                                GraphEdge(
                                    sourceId = doc.id,
                                    targetId = otherDoc.id,
                                    type = EdgeType.WIKILINK,
                                    isDashed = true,
                                    color = Color(0xFFF472B6)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(documents, folders.size, docFolderMap.toMap()) {
        buildGraph()
    }

    // Fast set of connected node IDs for highlighting connected chains
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

    // Pulsing animations for selected node
    val infiniteTransition = rememberInfiniteTransition(label = "selectedNodePulse")
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
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlowAlpha"
    )
    val pulseNodeScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseNodeScale"
    )

    // Gesture handling for pinch-to-zoom
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.35f, 4.0f)
        offset += panChange
    }

    // Selected document or folder for inspector
    val selectedDoc = remember(selectedNodeId, documents) {
        documents.find { it.id == selectedNodeId }
    }
    val selectedFolder = remember(selectedNodeId, folders) {
        folders.find { it.id == selectedNodeId }
    }

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
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val canvasX = (tapOffset.x - offset.x) / scale
                                    val canvasY = (tapOffset.y - offset.y) / scale

                                    val hit = nodes.findLast { node ->
                                        val dx = node.x - canvasX
                                        val dy = node.y - canvasY
                                        hypot(dx, dy) <= node.size + 22f
                                    }
                                    selectedNodeId = hit?.id
                                }
                            )
                        }
                        .pointerInput(nodes) {
                            detectDragGestures(
                                onDragStart = { pointerOffset ->
                                    val canvasX = (pointerOffset.x - offset.x) / scale
                                    val canvasY = (pointerOffset.y - offset.y) / scale

                                    val hit = nodes.findLast { node ->
                                        val dx = node.x - canvasX
                                        val dy = node.y - canvasY
                                        hypot(dx, dy) <= node.size + 22f
                                    }

                                    if (hit != null) {
                                        draggedNodeId = hit.id
                                        selectedNodeId = hit.id
                                    } else {
                                        draggedNodeId = null
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val activeDragId = draggedNodeId
                                    if (activeDragId != null) {
                                        val node = nodes.find { it.id == activeDragId }
                                        if (node != null) {
                                            node.x += dragAmount.x / scale
                                            node.y += dragAmount.y / scale
                                        }
                                    } else {
                                        // Pan background smoothly
                                        offset += dragAmount
                                    }
                                },
                                onDragEnd = { draggedNodeId = null },
                                onDragCancel = { draggedNodeId = null }
                            )
                        }
                ) {
                    // Minimal-overhead background grid pattern
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

                    val nodeMap = nodes.associateBy { it.id }

                    // --- Draw Chains & Connections ---
                    edges.forEach { edge ->
                        val src = nodeMap[edge.sourceId]
                        val dst = nodeMap[edge.targetId]
                        if (src != null && dst != null) {
                            val isDirectLink = (selectedNodeId == edge.sourceId || selectedNodeId == edge.targetId)
                            val isChain = highlightedNodeIds.contains(edge.sourceId) && highlightedNodeIds.contains(edge.targetId)
                            val isDimmed = selectedNodeId != null && !isChain

                            val p1 = Offset(src.x * scale + offset.x, src.y * scale + offset.y)
                            val p2 = Offset(dst.x * scale + offset.x, dst.y * scale + offset.y)

                            when (edge.type) {
                                EdgeType.TRUNK -> {
                                    val glowAlpha = if (isDirectLink || isChain) 0.50f else if (isDimmed) 0.05f else 0.22f
                                    val coreAlpha = if (isDirectLink || isChain) 1.0f else if (isDimmed) 0.15f else 0.85f

                                    drawLine(
                                        color = Color(0xFF00E5FF).copy(alpha = glowAlpha),
                                        start = p1,
                                        end = p2,
                                        strokeWidth = 8f * scale,
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = Color(0xFF00E5FF).copy(alpha = coreAlpha),
                                        start = p1,
                                        end = p2,
                                        strokeWidth = if (isDirectLink || isChain) 3.5f * scale else 2.2f * scale,
                                        cap = StrokeCap.Round
                                    )
                                }
                                EdgeType.BRANCH -> {
                                    val branchAlpha = if (isDirectLink || isChain) 0.95f else if (isDimmed) 0.06f else 0.45f
                                    val strokeWidth = if (isDirectLink || isChain) 3.0f * scale else 1.8f * scale

                                    val midX = (p1.x + p2.x) / 2f
                                    val midY = (p1.y + p2.y) / 2f
                                    val dx = p2.x - p1.x
                                    val dy = p2.y - p1.y
                                    val ctrlX = midX - dy * 0.04f
                                    val ctrlY = midY + dx * 0.04f

                                    bezierPath.reset()
                                    bezierPath.moveTo(p1.x, p1.y)
                                    bezierPath.quadraticTo(ctrlX, ctrlY, p2.x, p2.y)

                                    drawPath(
                                        path = bezierPath,
                                        color = edge.color.copy(alpha = branchAlpha),
                                        style = Stroke(
                                            width = strokeWidth,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                }
                                EdgeType.WIKILINK -> {
                                    val linkAlpha = if (isDirectLink) 0.95f else if (isDimmed) 0.05f else 0.45f
                                    val strokeWidth = if (isDirectLink) 2.8f * scale else 1.6f * scale

                                    val midX = (p1.x + p2.x) / 2f
                                    val midY = (p1.y + p2.y) / 2f
                                    val dx = p2.x - p1.x
                                    val dy = p2.y - p1.y
                                    val ctrlX = midX - dy * 0.15f
                                    val ctrlY = midY + dx * 0.15f

                                    bezierPath.reset()
                                    bezierPath.moveTo(p1.x, p1.y)
                                    bezierPath.quadraticTo(ctrlX, ctrlY, p2.x, p2.y)

                                    drawPath(
                                        path = bezierPath,
                                        color = Color(0xFFF472B6).copy(alpha = linkAlpha),
                                        style = Stroke(
                                            width = strokeWidth,
                                            cap = StrokeCap.Round,
                                            pathEffect = dashEffect
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // --- Draw Nodes ---
                    nodes.forEach { node ->
                        val screenPos = Offset(node.x * scale + offset.x, node.y * scale + offset.y)
                        val isSelected = node.id == selectedNodeId
                        val isConnected = highlightedNodeIds.contains(node.id)
                        val isDimmed = selectedNodeId != null && !isConnected
                        val isActiveDoc = node.documentId == activeDocumentId

                        val baseRadius = (node.size * scale).coerceAtLeast(10f)
                        val radius = if (isSelected) baseRadius * pulseNodeScale else baseRadius
                        val alpha = if (isDimmed) 0.16f else 1f

                        if (isSelected) {
                            val rippleRadius = baseRadius * pulseRingScale
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = pulseRingAlpha),
                                radius = rippleRadius,
                                center = screenPos,
                                style = Stroke(width = 2.5f * scale)
                            )

                            val secondaryRingScale = 1.0f + (pulseRingScale - 1.0f) * 0.6f
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = pulseRingAlpha * 0.5f),
                                radius = baseRadius * secondaryRingScale,
                                center = screenPos,
                                style = Stroke(width = 1.8f * scale)
                            )

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

                        if (node.type == GraphNodeType.ROOT_WORKSPACE) {
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.35f * alpha),
                                radius = radius * 1.55f,
                                center = screenPos,
                                style = Stroke(width = 1.5f * scale, pathEffect = dashEffect)
                            )
                        }

                        drawCircle(
                            color = if (isSelected) Color(0xFF16233B) else Color(0xFF0D1424).copy(alpha = alpha),
                            radius = radius,
                            center = screenPos
                        )

                        drawCircle(
                            color = if (isSelected) Color(0xFF00E5FF) else node.color.copy(alpha = alpha),
                            radius = radius,
                            center = screenPos,
                            style = Stroke(width = if (isSelected) 3.8f * scale else 2.2f * scale)
                        )

                        val coreRadius = if (isSelected) radius * 0.48f else radius * 0.38f
                        drawCircle(
                            color = if (isSelected) Color(0xFF00E5FF) else node.color.copy(alpha = alpha),
                            radius = coreRadius,
                            center = screenPos
                        )

                        val matchesSearch = searchQuery.isNotBlank() &&
                            node.title.contains(searchQuery, ignoreCase = true)

                        val baseTextPx = 13.5f * density * labelTextScale
                        val scaledTextSize = (baseTextPx * (0.85f + (scale - 1f) * 0.35f))
                            .coerceIn(12f * density * labelTextScale, 28f * density * labelTextScale)

                        textPaint.textSize = scaledTextSize
                        textPaint.isFakeBoldText = isSelected || matchesSearch || isActiveDoc || node.type == GraphNodeType.ROOT_WORKSPACE

                        val textWidth = textPaint.measureText(node.title)
                        val textHeight = scaledTextSize

                        val pillPaddingX = 8f * density * labelTextScale
                        val pillPaddingY = 4f * density * labelTextScale
                        val textCenterY = screenPos.y + radius + (18f * scale)

                        val pillLeft = screenPos.x - (textWidth / 2f) - pillPaddingX
                        val pillTop = textCenterY - textHeight
                        val pillRight = screenPos.x + (textWidth / 2f) + pillPaddingX
                        val pillBottom = textCenterY + (pillPaddingY * 1.5f)

                        val pillAlpha = if (isDimmed) 0.3f else 0.94f
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

                        val textColor = when {
                            matchesSearch -> android.graphics.Color.parseColor("#00E5FF")
                            isSelected -> android.graphics.Color.WHITE
                            isDimmed -> android.graphics.Color.argb(55, 180, 190, 205)
                            isActiveDoc -> android.graphics.Color.parseColor("#00E5FF")
                            node.type == GraphNodeType.ROOT_WORKSPACE -> android.graphics.Color.parseColor("#00E5FF")
                            node.type == GraphNodeType.FOLDER -> android.graphics.Color.parseColor("#F1F5F9")
                            else -> android.graphics.Color.parseColor("#E2E8F0")
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
                val rootDocs = documents.filter { docFolderMap[it.id] == null || docFolderMap[it.id] == "root" }
                val filteredRootDocs = if (searchQuery.isBlank()) rootDocs else {
                    rootDocs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 130.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Root Vault Header Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xCC0B162A)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Hub,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Root Vault",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${folders.size} sub-folders • ${documents.size} documents",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { isAddFolderDialogOpen = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                            contentColor = Color(0xFF00E5FF)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (filteredRootDocs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Files in Root:", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        filteredRootDocs.forEach { doc ->
                                            FolderTreeFileItem(
                                                document = doc,
                                                isActive = doc.id == activeDocumentId,
                                                accentColor = Color(0xFF00E5FF),
                                                onOpenInEditor = {
                                                    onSelectDocument(doc.id)
                                                    onClose()
                                                },
                                                onMoveFolder = {
                                                    movingDocument = doc
                                                }
                                            )
                                        }
                                    }
                                } else if (folders.isEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No sub-folders created yet. Tap '+ Folder' to organize your notes.",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // List of Sub-folders (if user created any)
                    items(folders, key = { it.id }) { folder ->
                        val isExpanded = folderExpandedMap[folder.id] ?: true
                        val folderDocs = documents.filter { docFolderMap[it.id] == folder.id }
                        val filteredDocs = if (searchQuery.isBlank()) folderDocs else {
                            folderDocs.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.content.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xCC0D1526)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A3F))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            folderExpandedMap[folder.id] = !isExpanded
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val rotation by animateFloatAsState(
                                            targetValue = if (isExpanded) 90f else 0f,
                                            label = "arrow"
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = folder.color,
                                            modifier = Modifier.size(20.dp).rotate(rotation)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = folder.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = folder.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = folder.color.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${folderDocs.size} files",
                                            color = folder.color,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (filteredDocs.isEmpty()) {
                                            Text(
                                                text = if (folderDocs.isEmpty()) "Empty folder" else "No matching files",
                                                color = Color(0xFF64748B),
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 4.dp)
                                            )
                                        } else {
                                            filteredDocs.forEach { doc ->
                                                FolderTreeFileItem(
                                                    document = doc,
                                                    isActive = doc.id == activeDocumentId,
                                                    accentColor = folder.color,
                                                    onOpenInEditor = {
                                                        onSelectDocument(doc.id)
                                                        onClose()
                                                    },
                                                    onMoveFolder = {
                                                        movingDocument = doc
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

            // --- Top Bar HUD ---
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
                        placeholder = { Text("Filter files & folders...", fontSize = 12.sp, color = Color(0xFF64748B)) },
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

                    // + New Folder Button
                    IconButton(
                        onClick = { isAddFolderDialogOpen = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Add New Folder",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x990D131F),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A3F))
                        ) {
                            Text(
                                text = "${folders.size} Folders • ${documents.size} Notes",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // --- Floating Zoom & Action Controls ---
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
                        icon = Icons.Default.CreateNewFolder,
                        desc = "New Folder",
                        onClick = { isAddFolderDialogOpen = true }
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

            // --- Clean Bottom Inspector Card for Selected Node ---
            AnimatedVisibility(
                visible = viewMode == TreeViewMode.NETWORK_GRAPH && (selectedDoc != null || selectedFolder != null),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (selectedDoc != null) {
                    val assignedFolderId = docFolderMap[selectedDoc.id]
                    val currentFolder = folders.find { it.id == assignedFolderId }
                    val connectedDocs = remember(selectedDoc.id, edges, documents) {
                        val ids = edges.filter { it.sourceId == selectedDoc.id || it.targetId == selectedDoc.id }
                            .map { if (it.sourceId == selectedDoc.id) it.targetId else it.sourceId }
                        documents.filter { ids.contains(it.id) }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1E2A38), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF50D1526)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Row
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
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = currentFolder?.color ?: Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
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

                                Button(
                                    onClick = {
                                        onSelectDocument(selectedDoc.id)
                                        onClose()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0284C7),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Open in Editor",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Metadata Badges Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentFolder != null) currentFolder.color.copy(alpha = 0.15f) else Color(0x3300E5FF),
                                    modifier = Modifier.clickable { movingDocument = selectedDoc }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            null,
                                            tint = currentFolder?.color ?: Color(0xFF00E5FF),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = currentFolder?.name ?: "Root Vault",
                                            fontSize = 11.sp,
                                            color = currentFolder?.color ?: Color(0xFF00E5FF),
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (folders.isNotEmpty()) {
                                            Icon(
                                                Icons.Default.DriveFileMove,
                                                "Move",
                                                tint = (currentFolder?.color ?: Color(0xFF00E5FF)).copy(alpha = 0.7f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = "${selectedDoc.content.length} chars",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }

                                if (connectedDocs.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0x33F472B6)
                                    ) {
                                        Text(
                                            text = "Linked: ${connectedDocs.size} notes",
                                            fontSize = 11.sp,
                                            color = Color(0xFFF472B6),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (connectedDocs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(connectedDocs) { conn ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
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
                } else if (selectedFolder != null) {
                    val folderDocs = documents.filter { docFolderMap[it.id] == selectedFolder.id }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF1E2A38), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF50D1526)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Folder, null, tint = selectedFolder.color, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = selectedFolder.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = selectedFolder.color.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${folderDocs.size} files",
                                        color = selectedFolder.color,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Location: ${if (selectedFolder.parentId == "root") "Root Vault" else selectedFolder.parentId}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // --- Add New Folder Dialog ---
            if (isAddFolderDialogOpen) {
                AlertDialog(
                    onDismissRequest = { isAddFolderDialogOpen = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color(0xFF00E5FF))
                            Text("Create New Folder", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            OutlinedTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                label = { Text("Folder Name") },
                                placeholder = { Text("e.g. Research, Projects, Notes") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Text("Color Accent", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FOLDER_PALETTE.forEach { color ->
                                    val isSelectedColor = (newFolderColor == color)
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelectedColor) 2.5.dp else 1.dp,
                                                color = if (isSelectedColor) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { newFolderColor = color }
                                    )
                                }
                            }

                            if (folders.isNotEmpty()) {
                                Text("Parent Location", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { newFolderParentId = "root" }) {
                                        RadioButton(
                                            selected = newFolderParentId == "root",
                                            onClick = { newFolderParentId = "root" },
                                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                                        )
                                        Text("Root Vault", color = Color.White, fontSize = 13.sp)
                                    }

                                    folders.forEach { f ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { newFolderParentId = f.id }) {
                                            RadioButton(
                                                selected = newFolderParentId == f.id,
                                                onClick = { newFolderParentId = f.id },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00E5FF))
                                            )
                                            Text(f.name, color = Color.White, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newFolderName.isNotBlank()) {
                                    val newId = "folder_${System.currentTimeMillis()}"
                                    folders.add(
                                        GraphFolder(
                                            id = newId,
                                            name = newFolderName.trim(),
                                            parentId = newFolderParentId,
                                            color = newFolderColor
                                        )
                                    )
                                    folderExpandedMap[newId] = true
                                    newFolderName = ""
                                    isAddFolderDialogOpen = false
                                }
                            },
                            enabled = newFolderName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF050B14)
                            )
                        ) {
                            Text("Create Folder", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isAddFolderDialogOpen = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    },
                    containerColor = Color(0xFF0D1424),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // --- Move Document to Folder Dialog ---
            movingDocument?.let { docToMove ->
                AlertDialog(
                    onDismissRequest = { movingDocument = null },
                    title = {
                        Text("Move \"${docToMove.title}\"", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select target location:", color = Color(0xFF94A3B8), fontSize = 12.sp)

                            // Option to move to Root Vault
                            val isRootCurrent = (docFolderMap[docToMove.id] == null || docFolderMap[docToMove.id] == "root")
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        docFolderMap[docToMove.id] = "root"
                                        movingDocument = null
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isRootCurrent) Color(0x3300E5FF) else Color(0xFF16233B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isRootCurrent) Color(0xFF00E5FF) else Color(0xFF24324D)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Hub, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    Text("Root Vault", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    if (isRootCurrent) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("CURRENT", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (folders.isEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No custom sub-folders exist yet. Use '+ Folder' to create one.", color = Color(0xFF64748B), fontSize = 11.sp)
                            } else {
                                folders.forEach { targetFolder ->
                                    val isCurrent = (docFolderMap[docToMove.id] == targetFolder.id)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                docFolderMap[docToMove.id] = targetFolder.id
                                                movingDocument = null
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) targetFolder.color.copy(alpha = 0.2f) else Color(0xFF16233B),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isCurrent) targetFolder.color else Color(0xFF24324D)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Default.Folder, null, tint = targetFolder.color, modifier = Modifier.size(18.dp))
                                            Text(targetFolder.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            if (isCurrent) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text("CURRENT", color = targetFolder.color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { movingDocument = null }) {
                            Text("Close", color = Color(0xFF94A3B8))
                        }
                    },
                    containerColor = Color(0xFF0D1424),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FolderTreeFileItem(
    document: MarkdownDocument,
    isActive: Boolean,
    accentColor: Color,
    onOpenInEditor: () -> Unit,
    onMoveFolder: () -> Unit
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

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveFolder,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DriveFileMove,
                        contentDescription = "Move file to folder",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = onOpenInEditor,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color(0xFF0284C7) else Color(0xFF1E293B),
                        contentColor = if (isActive) Color.White else Color(0xFF00E5FF)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Open",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
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
