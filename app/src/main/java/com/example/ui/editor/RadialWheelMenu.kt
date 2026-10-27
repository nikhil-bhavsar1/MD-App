package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Sub-option item representing an action in the outer weapon-wheel ring.
 */
data class SubOptionWedge(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector? = null,
    val badgeText: String? = null,
    val accentColor: Color,
    val onClick: () -> Unit
)

/**
 * Main category wedge block on the primary GTA-style weapon wheel.
 */
data class CategoryWedge(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val subOptions: List<SubOptionWedge>
)

/**
 * GTA 5 Weapon Wheel-style Segmented Radial Interface:
 * - Annular segmented wedge blocks with gap dividers.
 * - Initial state shows ONLY the 6 primary category blocks (completely uncluttered).
 * - Dragging/hovering over a block highlights it, pops it outward, and displays its title/details in the center HUD.
 * - The second level (outer sub-option wedge blocks) is HIDDEN until that category is hovered/selected.
 * - Sliding into an outer sub-wedge highlights it, and releasing executes that option immediately.
 */
@Composable
fun RadialWheelMenu(
    touchPosition: Offset,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onInsertText: (prefix: String, suffix: String) -> Unit,
    onOpenVault: () -> Unit,
    onToggleFind: () -> Unit,
    onOpenSyntaxCatalog: () -> Unit,
    onOpenUnicodePicker: () -> Unit,
    onOpenCtan: () -> Unit,
    onOpenGraphs: () -> Unit,
    onOpenVaultSheet: () -> Unit,
    onCreateNewDocument: () -> Unit,
    onOpenRename: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val categories = remember(
        onInsertText,
        onOpenVault,
        onToggleFind,
        onOpenSyntaxCatalog,
        onOpenUnicodePicker,
        onOpenCtan,
        onOpenGraphs,
        onOpenVaultSheet,
        onCreateNewDocument,
        onOpenRename
    ) {
        listOf(
            CategoryWedge(
                id = "headings",
                title = "Headings",
                description = "Header levels H1 through H6",
                icon = Icons.Default.Title,
                accentColor = Color(0xFF38BDF8),
                subOptions = listOf(
                    SubOptionWedge("h1", "H1", "Level 1 Heading (# )", badgeText = "H1", accentColor = Color(0xFF00E5FF)) { onInsertText("# ", "") },
                    SubOptionWedge("h2", "H2", "Level 2 Heading (## )", badgeText = "H2", accentColor = Color(0xFF38BDF8)) { onInsertText("## ", "") },
                    SubOptionWedge("h3", "H3", "Level 3 Heading (### )", badgeText = "H3", accentColor = Color(0xFF818CF8)) { onInsertText("### ", "") },
                    SubOptionWedge("h4", "H4", "Level 4 Heading (#### )", badgeText = "H4", accentColor = Color(0xFFA78BFA)) { onInsertText("#### ", "") },
                    SubOptionWedge("h5", "H5", "Level 5 Heading (##### )", badgeText = "H5", accentColor = Color(0xFFC084FC)) { onInsertText("##### ", "") },
                    SubOptionWedge("h6", "H6", "Level 6 Heading (###### )", badgeText = "H6", accentColor = Color(0xFFE879F9)) { onInsertText("###### ", "") }
                )
            ),
            CategoryWedge(
                id = "style",
                title = "Format",
                description = "Text typography and formatting",
                icon = Icons.Default.FormatBold,
                accentColor = Color(0xFF00E5FF),
                subOptions = listOf(
                    SubOptionWedge("bold", "Bold", "Bold weight (**text**)", icon = Icons.Default.FormatBold, accentColor = Color(0xFF00E5FF)) { onInsertText("**", "**") },
                    SubOptionWedge("italic", "Italic", "Italic slant (*text*)", icon = Icons.Default.FormatItalic, accentColor = Color(0xFF38BDF8)) { onInsertText("*", "*") },
                    SubOptionWedge("strike", "Strike", "Strikethrough (~~text~~)", badgeText = "~~", accentColor = Color(0xFFF472B6)) { onInsertText("~~", "~~") },
                    SubOptionWedge("highlight", "Highlight", "Neon highlight (==text==)", icon = Icons.Default.FormatPaint, accentColor = Color(0xFFFEF08A)) { onInsertText("==", "==") },
                    SubOptionWedge("quote", "Quote", "Blockquote (> )", icon = Icons.Default.FormatQuote, accentColor = Color(0xFFFB7185)) { onInsertText("> ", "") },
                    SubOptionWedge("underline", "Underline", "HTML underline (<u>)", icon = Icons.Default.FormatUnderlined, accentColor = Color(0xFFFCD34D)) { onInsertText("<u>", "</u>") }
                )
            ),
            CategoryWedge(
                id = "math_code",
                title = "Math & Code",
                description = "LaTeX math formulas and code blocks",
                icon = Icons.Default.Functions,
                accentColor = Color(0xFFC084FC),
                subOptions = listOf(
                    SubOptionWedge("inline_code", "Inline Code", "Inline monospace (`code`)", icon = Icons.Default.Code, accentColor = Color(0xFF34D399)) { onInsertText("`", "`") },
                    SubOptionWedge("code_block", "Code Block", "Fenced code block (```)", icon = Icons.Default.DataObject, accentColor = Color(0xFF10B981)) { onInsertText("```\n", "\n```") },
                    SubOptionWedge("inline_math", "\$ Math", "Inline LaTeX formula (\$x\$)", badgeText = "\$x\$", accentColor = Color(0xFFC084FC)) { onInsertText("$", "$") },
                    SubOptionWedge("display_math", "\$\$ Display", "Display LaTeX block (\$\$\n\$\$)", badgeText = "\$\$", accentColor = Color(0xFFA855F7)) { onInsertText("$$\n", "\n$$") },
                    SubOptionWedge("ctan", "CTAN DB", "LaTeX Package Manager", icon = Icons.Default.Functions, accentColor = Color(0xFFEC4899), onClick = onOpenCtan),
                    SubOptionWedge("diagrams", "Diagrams", "Mermaid & Cytoscape visualizer", icon = Icons.Default.Hub, accentColor = Color(0xFF8B5CF6), onClick = onOpenGraphs)
                )
            ),
            CategoryWedge(
                id = "structure",
                title = "Lists & Tables",
                description = "Task lists, tables, and callouts",
                icon = Icons.Default.TableChart,
                accentColor = Color(0xFF4ADE80),
                subOptions = listOf(
                    SubOptionWedge("task", "Task", "Checklist item (- [ ] )", icon = Icons.Default.TaskAlt, accentColor = Color(0xFFFBBF24)) { onInsertText("- [ ] ", "") },
                    SubOptionWedge("bullet", "Bullets", "Unordered list (- )", icon = Icons.AutoMirrored.Filled.FormatListBulleted, accentColor = Color(0xFF34D399)) { onInsertText("- ", "") },
                    SubOptionWedge("number", "Numbered", "Ordered list (1. )", icon = Icons.Default.FormatListNumbered, accentColor = Color(0xFF2DD4BF)) { onInsertText("1. ", "") },
                    SubOptionWedge("table", "Table", "Markdown grid table", icon = Icons.Default.TableChart, accentColor = Color(0xFF4ADE80)) { onInsertText("| Col 1 | Col 2 |\n| --- | --- |\n| Val 1 | Val 2 |\n", "") },
                    SubOptionWedge("callout", "Callout", "Obsidian / GitHub alert note", icon = Icons.Default.Info, accentColor = Color(0xFF38BDF8)) { onInsertText("> [!NOTE]\n> ", "") },
                    SubOptionWedge("divider", "Divider", "Horizontal rule (---)", badgeText = "---", accentColor = Color(0xFF94A3B8)) { onInsertText("\n---\n", "") }
                )
            ),
            CategoryWedge(
                id = "insert",
                title = "Insert",
                description = "Links, images, and symbol palettes",
                icon = Icons.Default.Link,
                accentColor = Color(0xFFFBBF24),
                subOptions = listOf(
                    SubOptionWedge("wiki_link", "Wiki Link", "Internal wiki link ([[ ]])", icon = Icons.Default.Link, accentColor = Color(0xFF818CF8)) { onInsertText("[[", "]]") },
                    SubOptionWedge("web_link", "Web Link", "Markdown hyperlink ([ ]( ))", icon = Icons.Default.OpenInNew, accentColor = Color(0xFF60A5FA)) { onInsertText("[", "](url)") },
                    SubOptionWedge("image", "Image", "Embedded image (![ ]( ))", icon = Icons.Default.Image, accentColor = Color(0xFFF59E0B)) { onInsertText("![Alt text](", ")") },
                    SubOptionWedge("emoji", "Emoji", "Unicode symbols palette", icon = Icons.Default.Mood, accentColor = Color(0xFFFBBF24), onClick = onOpenUnicodePicker),
                    SubOptionWedge("syntax", "Syntax Guide", "Markdown syntax cheat sheet", icon = Icons.Default.MenuBook, accentColor = Color(0xFFE879F9), onClick = onOpenSyntaxCatalog)
                )
            ),
            CategoryWedge(
                id = "vault",
                title = "Vault",
                description = "Document drawer, search, and storage",
                icon = Icons.Default.Storage,
                accentColor = Color(0xFF06B6D4),
                subOptions = listOf(
                    SubOptionWedge("docs_drawer", "Documents", "Open documents drawer", icon = Icons.Default.Menu, accentColor = Color(0xFF06B6D4), onClick = onOpenVault),
                    SubOptionWedge("new_doc", "New Doc", "Create fresh markdown document", icon = Icons.Default.Add, accentColor = Color(0xFF00E5FF), onClick = onCreateNewDocument),
                    SubOptionWedge("find_replace", "Find", "Find & Replace bar", icon = Icons.Default.Search, accentColor = Color(0xFFFB923C), onClick = onToggleFind),
                    SubOptionWedge("vault_files", "Vault Files", "Browse local disk files (.md)", icon = Icons.Default.Storage, accentColor = Color(0xFF10B981), onClick = onOpenVaultSheet),
                    SubOptionWedge("rename", "Rename", "Rename active document", icon = Icons.Default.DriveFileRenameOutline, accentColor = Color(0xFF38BDF8), onClick = onOpenRename)
                )
            )
        )
    }

    // Hover state: Initially null! (Zero clutter - outer wheel hidden till hovered)
    var hoveredCategoryIndex by remember { mutableStateOf<Int?>(null) }
    var hoveredSubOptionIndex by remember { mutableStateOf<Int?>(null) }

    // Mild pulsing animation for the selected hovered wedge
    val infiniteTransition = rememberInfiniteTransition(label = "wedgePulse")
    val glowPulseAlpha = infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulseAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Tapping anywhere dismisses if not on an action
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            }
    ) {
        val density = LocalDensity.current
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Generous, large GTA-style weapon wheel dimensions for outstanding clarity and readability
        val availableMinDimension = minOf(screenWidth, screenHeight)
        val isCompact = availableMinDimension < 520.dp

        val totalMaxRadius = if (isCompact) 188.dp else 225.dp
        val hubRadius = if (isCompact) 48.dp else 58.dp
        val primaryInnerRadius = if (isCompact) 54.dp else 66.dp
        val primaryOuterRadius = if (isCompact) 120.dp else 145.dp
        val outerInnerRadius = if (isCompact) 128.dp else 155.dp
        val outerOuterRadius = if (isCompact) 188.dp else 225.dp
        val popDistancePx = with(density) { 7.dp.toPx() }

        val safeMargin = 4.dp

        val rawTouchXDp = with(density) { touchPosition.x.toDp() }
        val rawTouchYDp = with(density) { touchPosition.y.toDp() }

        val minX = totalMaxRadius + safeMargin
        val maxX = screenWidth - totalMaxRadius - safeMargin
        val centerXDp = if (minX >= maxX || isCompact) {
            screenWidth / 2
        } else {
            rawTouchXDp.coerceIn(minX, maxX)
        }

        val minY = totalMaxRadius + safeMargin
        val maxY = screenHeight - totalMaxRadius - safeMargin
        val centerYDp = if (minY >= maxY) {
            screenHeight / 2
        } else {
            rawTouchYDp.coerceIn(minY, maxY)
        }

        val centerPx = with(density) { Offset(centerXDp.toPx(), centerYDp.toPx()) }
        val primaryInnerPx = with(density) { primaryInnerRadius.toPx() }
        val primaryOuterPx = with(density) { primaryOuterRadius.toPx() }
        val outerInnerPx = with(density) { outerInnerRadius.toPx() }
        val outerOuterPx = with(density) { outerOuterRadius.toPx() }

        // Helper to resolve hovered wedge from touch coordinate
        fun processTouch(offset: Offset, isRelease: Boolean = false) {
            val dx = offset.x - centerPx.x
            val dy = offset.y - centerPx.y
            val dist = sqrt(dx * dx + dy * dy)

            // Angle in degrees from 0 to 360, where 0 deg is top (12 o'clock)
            val rad = atan2(dy, dx)
            val deg = (Math.toDegrees(rad.toDouble()).toFloat() + 90f + 360f) % 360f

            val catCount = categories.size
            if (catCount == 0) return
            val catArc = 360f / catCount // 60 deg each

            // Check if touch is inside the primary wheel ring
            if (dist in primaryInnerPx..primaryOuterPx) {
                val catIdx = (deg / catArc).toInt().coerceIn(0, catCount - 1)
                hoveredCategoryIndex = catIdx
                hoveredSubOptionIndex = null
                return
            }

            // Check if touch is inside the outer sub-wheel ring
            val currentCatIdx = hoveredCategoryIndex
            if (currentCatIdx != null && dist in outerInnerPx..(outerOuterPx + popDistancePx * 2)) {
                val activeCat = categories.getOrNull(currentCatIdx)
                if (activeCat != null && activeCat.subOptions.isNotEmpty()) {
                    val subCount = activeCat.subOptions.size
                    val catCenterDeg = currentCatIdx * catArc + (catArc / 2)
                    val fanSpanDeg = 110f
                    val subStartDeg = catCenterDeg - fanSpanDeg / 2

                    // Calculate relative angle within sub-fan
                    val deltaDeg = ((deg - subStartDeg + 360f) % 360f)
                    if (deltaDeg in 0f..fanSpanDeg) {
                        val subIdx = ((deltaDeg / fanSpanDeg) * subCount).toInt().coerceIn(0, subCount - 1)
                        hoveredSubOptionIndex = subIdx

                        if (isRelease) {
                            try {
                                activeCat.subOptions.getOrNull(subIdx)?.onClick?.invoke()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            onDismiss()
                        }
                        return
                    }
                }
            }

            // Inside center hub or outside wheel
            if (dist < primaryInnerPx || dist > (outerOuterPx + popDistancePx * 2)) {
                if (isRelease) {
                    onDismiss()
                }
            }
        }

        // Dark translucent glass backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD403060E))
                // Gesture handling: smooth drag hover and release to execute
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            processTouch(offset, isRelease = false)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            processTouch(change.position, isRelease = false)
                        },
                        onDragEnd = {
                            val catIdx = hoveredCategoryIndex
                            val subIdx = hoveredSubOptionIndex
                            if (catIdx != null && subIdx != null) {
                                val cat = categories.getOrNull(catIdx)
                                val sub = cat?.subOptions?.getOrNull(subIdx)
                                try {
                                    sub?.onClick?.invoke()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                onDismiss()
                            }
                        },
                        onDragCancel = {
                            hoveredSubOptionIndex = null
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val dx = offset.x - centerPx.x
                            val dy = offset.y - centerPx.y
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist in primaryInnerPx..primaryOuterPx) {
                                processTouch(offset, isRelease = false)
                            } else if (dist in outerInnerPx..(outerOuterPx + popDistancePx * 2) && hoveredCategoryIndex != null) {
                                processTouch(offset, isRelease = true)
                            } else {
                                onDismiss()
                            }
                        }
                    )
                }
        )

        // Main Container for Canvas and Wedges
        Box(modifier = Modifier.fillMaxSize()) {

            // --- 1. CANVAS: DRAW GTA 5 ANNULAR WEDGE BLOCKS ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                val catCount = categories.size
                val catArcDeg = 360f / catCount
                val gapDeg = 3.0f // Angular gap between blocks for tactile segmented look

                // A. Draw Primary Wheel Blocks
                categories.forEachIndexed { i, category ->
                    val isHovered = hoveredCategoryIndex == i

                    val startDeg = (i * catArcDeg - 90f) + (gapDeg / 2)
                    val sweepDeg = catArcDeg - gapDeg
                    val endDeg = startDeg + sweepDeg

                    val startRad = Math.toRadians(startDeg.toDouble())
                    val endRad = Math.toRadians(endDeg.toDouble())

                    val pop = if (isHovered) popDistancePx else 0f
                    val path = createAnnulusWedgePath(
                        center = centerPx,
                        startRad = startRad,
                        endRad = endRad,
                        innerR = primaryInnerPx,
                        outerR = primaryOuterPx,
                        pop = pop
                    )

                    // Fill wedge block
                    val fillBrush = if (isHovered) {
                        Brush.radialGradient(
                            colors = listOf(
                                category.accentColor.copy(alpha = 0.40f),
                                category.accentColor.copy(alpha = 0.18f)
                            ),
                            center = centerPx,
                            radius = primaryOuterPx
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color(0xF2090F1C), Color(0xEB060A14))
                        )
                    }
                    drawPath(path, brush = fillBrush)

                    // Border stroke
                    val borderColor = if (isHovered) {
                        category.accentColor
                    } else {
                        Color(0x3338BDF8)
                    }
                    val strokeWidth = if (isHovered) 2.2.dp.toPx() else 1.2.dp.toPx()
                    drawPath(path, color = borderColor, style = Stroke(width = strokeWidth))
                }

                // B. Draw Outer 2nd Level Blocks (ONLY when a category is hovered!)
                val activeCatIdx = hoveredCategoryIndex
                if (activeCatIdx != null) {
                    val activeCat = categories.getOrNull(activeCatIdx)
                    if (activeCat != null && activeCat.subOptions.isNotEmpty()) {
                        val subCount = activeCat.subOptions.size

                        val catCenterDeg = (activeCatIdx * catArcDeg - 90f) + (catArcDeg / 2)
                        val fanSpanDeg = 110f
                        val subStartBaseDeg = catCenterDeg - fanSpanDeg / 2
                        val subArcDeg = fanSpanDeg / subCount
                        val subGapDeg = 2.5f

                        activeCat.subOptions.forEachIndexed { j, subOption ->
                            val isSubHovered = hoveredSubOptionIndex == j

                            val subStartDeg = subStartBaseDeg + j * subArcDeg + (subGapDeg / 2)
                            val subSweepDeg = subArcDeg - subGapDeg
                            val subEndDeg = subStartDeg + subSweepDeg

                            val subStartRad = Math.toRadians(subStartDeg.toDouble())
                            val subEndRad = Math.toRadians(subEndDeg.toDouble())

                            val subPop = if (isSubHovered) popDistancePx else 0f
                            val subPath = createAnnulusWedgePath(
                                center = centerPx,
                                startRad = subStartRad,
                                endRad = subEndRad,
                                innerR = outerInnerPx,
                                outerR = outerOuterPx,
                                pop = subPop
                            )

                            // Fill sub-wedge block
                            val subFillBrush = if (isSubHovered) {
                                Brush.radialGradient(
                                    colors = listOf(
                                        subOption.accentColor.copy(alpha = 0.48f),
                                        subOption.accentColor.copy(alpha = 0.22f)
                                    ),
                                    center = centerPx,
                                    radius = outerOuterPx
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xF50D1526), Color(0xED070C18))
                                )
                            }
                            drawPath(subPath, brush = subFillBrush)

                            val subBorderColor = if (isSubHovered) {
                                subOption.accentColor
                            } else {
                                subOption.accentColor.copy(alpha = 0.45f)
                            }
                            val subStrokeWidth = if (isSubHovered) 2.4.dp.toPx() else 1.2.dp.toPx()
                            drawPath(subPath, color = subBorderColor, style = Stroke(width = subStrokeWidth))
                        }
                    }
                }
            }

            // --- 2. COMPOSE OVERLAY: ICONS & LABELS ON WEDGE BLOCKS ---
            val catCount = categories.size
            val catArcDeg = 360f / catCount
            val primaryMidR = (primaryInnerRadius + primaryOuterRadius) / 2

            categories.forEachIndexed { i, category ->
                val isHovered = hoveredCategoryIndex == i
                val catCenterDeg = (i * catArcDeg - 90f) + (catArcDeg / 2)
                val catCenterRad = Math.toRadians(catCenterDeg.toDouble())

                val pop = if (isHovered) 7.dp else 0.dp
                val iconR = primaryMidR + pop

                val nodeCenterX = centerXDp + iconR * cos(catCenterRad).toFloat()
                val nodeCenterY = centerYDp + iconR * sin(catCenterRad).toFloat()

                val catBoxWidth = 76.dp
                val catBoxHeight = 56.dp

                Box(
                    modifier = Modifier
                        .offset(x = nodeCenterX - (catBoxWidth / 2), y = nodeCenterY - (catBoxHeight / 2))
                        .size(catBoxWidth, catBoxHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            hoveredCategoryIndex = i
                            hoveredSubOptionIndex = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.title,
                            tint = if (isHovered) category.accentColor else Color(0xFFE2E8F0),
                            modifier = Modifier.size(if (isHovered) 28.dp else 24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = category.title,
                            fontSize = if (isHovered) 12.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHovered) Color.White else Color(0xFFE2E8F0),
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // --- 3. COMPOSE OVERLAY: OUTER SUB-OPTION BLOCKS (ONLY WHEN HOVERED) ---
            val activeCatIdx = hoveredCategoryIndex
            if (activeCatIdx != null) {
                val activeCat = categories.getOrNull(activeCatIdx)
                if (activeCat != null && activeCat.subOptions.isNotEmpty()) {
                    val subCount = activeCat.subOptions.size

                    val catCenterDeg = (activeCatIdx * catArcDeg - 90f) + (catArcDeg / 2)
                    val fanSpanDeg = 110f
                    val subStartBaseDeg = catCenterDeg - fanSpanDeg / 2
                    val subArcDeg = fanSpanDeg / subCount

                    val outerMidR = (outerInnerRadius + outerOuterRadius) / 2

                    activeCat.subOptions.forEachIndexed { j, subOption ->
                        val isSubHovered = hoveredSubOptionIndex == j
                        val subMidDeg = subStartBaseDeg + j * subArcDeg + (subArcDeg / 2)
                        val subMidRad = Math.toRadians(subMidDeg.toDouble())

                        val subPop = if (isSubHovered) 7.dp else 0.dp
                        val subR = outerMidR + subPop

                        val subCenterX = centerXDp + subR * cos(subMidRad).toFloat()
                        val subCenterY = centerYDp + subR * sin(subMidRad).toFloat()

                        val subBoxWidth = 70.dp
                        val subBoxHeight = 54.dp

                        Box(
                            modifier = Modifier
                                .offset(x = subCenterX - (subBoxWidth / 2), y = subCenterY - (subBoxHeight / 2))
                                .size(subBoxWidth, subBoxHeight)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    try {
                                        subOption.onClick()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (subOption.icon != null) {
                                    Icon(
                                        imageVector = subOption.icon,
                                        contentDescription = subOption.label,
                                        tint = if (isSubHovered) Color.White else subOption.accentColor,
                                        modifier = Modifier.size(if (isSubHovered) 24.dp else 21.dp)
                                    )
                                } else if (subOption.badgeText != null) {
                                    Text(
                                        text = subOption.badgeText,
                                        fontSize = if (isSubHovered) 15.sp else 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSubHovered) Color.White else subOption.accentColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subOption.label,
                                    fontSize = if (isSubHovered) 10.5.sp else 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubHovered) Color.White else Color(0xFFCBD5E1),
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // --- 4. GTA 5 WEAPON WHEEL CENTER HUD ---
            val activeCat = hoveredCategoryIndex?.let { categories.getOrNull(it) }
            val activeSub = if (activeCat != null && hoveredSubOptionIndex != null) {
                activeCat.subOptions.getOrNull(hoveredSubOptionIndex!!)
            } else null

            Surface(
                modifier = Modifier
                    .offset(x = centerXDp - hubRadius, y = centerYDp - hubRadius)
                    .size(hubRadius * 2)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDismiss()
                    },
                shape = CircleShape,
                color = Color(0xF7070B14),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    activeSub?.accentColor ?: activeCat?.accentColor ?: Color(0xFF00E5FF)
                ),
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeSub != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = activeSub.label.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeSub.accentColor,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeSub.description,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE2E8F0),
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Center,
                                lineHeight = 12.sp,
                                maxLines = 3
                            )
                        }
                    } else if (activeCat != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = activeCat.icon,
                                contentDescription = activeCat.title,
                                tint = activeCat.accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeCat.title.uppercase(),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${activeCat.subOptions.size} OPTIONS",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeCat.accentColor,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "HYPERMARK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00E5FF),
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "TAP OR DRAG",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates a GTA 5 weapon wheel segmented annulus sector (curved wedge block).
 */
private fun createAnnulusWedgePath(
    center: Offset,
    startRad: Double,
    endRad: Double,
    innerR: Float,
    outerR: Float,
    pop: Float
): Path {
    val midRad = (startRad + endRad) / 2
    val popX = (pop * cos(midRad)).toFloat()
    val popY = (pop * sin(midRad)).toFloat()
    val shiftedCenter = Offset(center.x + popX, center.y + popY)

    val path = Path()

    // 1. Point at inner radius, start angle
    val innerStartX = shiftedCenter.x + innerR * cos(startRad).toFloat()
    val innerStartY = shiftedCenter.y + innerR * sin(startRad).toFloat()
    path.moveTo(innerStartX, innerStartY)

    // 2. Line to outer radius, start angle
    val outerStartX = shiftedCenter.x + outerR * cos(startRad).toFloat()
    val outerStartY = shiftedCenter.y + outerR * sin(startRad).toFloat()
    path.lineTo(outerStartX, outerStartY)

    // 3. Outer arc from start to end angle
    val outerRect = Rect(
        shiftedCenter.x - outerR,
        shiftedCenter.y - outerR,
        shiftedCenter.x + outerR,
        shiftedCenter.y + outerR
    )
    val startDeg = Math.toDegrees(startRad).toFloat()
    val sweepDeg = Math.toDegrees(endRad - startRad).toFloat()
    path.arcTo(outerRect, startDeg, sweepDeg, false)

    // 4. Line to inner radius, end angle
    val innerEndX = shiftedCenter.x + innerR * cos(endRad).toFloat()
    val innerEndY = shiftedCenter.y + innerR * sin(endRad).toFloat()
    path.lineTo(innerEndX, innerEndY)

    // 5. Inner arc back to start angle
    val innerRect = Rect(
        shiftedCenter.x - innerR,
        shiftedCenter.y - innerR,
        shiftedCenter.x + innerR,
        shiftedCenter.y + innerR
    )
    val endDeg = Math.toDegrees(endRad).toFloat()
    path.arcTo(innerRect, endDeg, -sweepDeg, false)

    path.close()
    return path
}
