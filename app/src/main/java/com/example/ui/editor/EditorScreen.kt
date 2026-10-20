package com.example.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorFont
import com.example.model.ViewMode
import com.example.ui.components.EmojiSuggestionStrip
import com.example.ui.components.UnicodePickerSheet
import com.example.ui.ctan.CtanManagerSheet
import com.example.ui.graphs.GraphHelperDialog
import com.example.ui.graphview.FileTreeGraphScreen
import com.example.ui.preview.MarkdownPreviewWebView
import com.example.util.MarkdownVisualTransformation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFromUri(uri)
            Toast.makeText(context, "Imported markdown from storage", Toast.LENGTH_SHORT).show()
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportActiveToUri(uri)
            Toast.makeText(context, "Saved .md file to device storage", Toast.LENGTH_SHORT).show()
        }
    }

    // If File Tree Graph view is opened, show it full screen
    if (uiState.isFileTreeGraphOpen) {
        FileTreeGraphScreen(
            documents = allDocs,
            activeDocumentId = uiState.activeDocument.id,
            onSelectDocument = { viewModel.selectDocument(it) },
            onClose = { viewModel.toggleFileTreeGraph(false) }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DocumentDrawerContent(
                documents = allDocs,
                activeDocumentId = uiState.activeDocument.id,
                vaultFilesCount = uiState.vaultFiles.size,
                onSelectDocument = { viewModel.selectDocument(it) },
                onCreateDocument = { viewModel.createNewDocument() },
                onImportFile = {
                    importFileLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*"))
                },
                onOpenVaultSheet = { viewModel.toggleVaultSheet(true) },
                onDeleteDocument = { viewModel.deleteDocument(it) },
                onTogglePin = { viewModel.togglePin(it) },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF090D14))
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = Color(0xFF090D14),
            topBar = {
                EditorTopBar(
                    title = uiState.activeDocument.title,
                    viewMode = uiState.viewMode,
                    installedCtanCount = uiState.activeDocument.getInstalledPackageList().size,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onSetViewMode = { viewModel.setViewMode(it) },
                    onOpenFileTreeGraph = { viewModel.toggleFileTreeGraph(true) },
                    onOpenSyntaxCatalog = { viewModel.toggleSyntaxCatalog(true) },
                    onOpenUnicodePicker = { viewModel.toggleUnicodePicker(true) },
                    onOpenCtan = { viewModel.toggleCtanSheet(true) },
                    onOpenGraphs = { viewModel.toggleGraphDialog(true) },
                    onOpenSettings = { viewModel.toggleSettingsDialog(true) },
                    onOpenRename = { viewModel.toggleRenameDialog(true) },
                    onOpenVault = { viewModel.toggleVaultSheet(true) },
                    onToggleFindReplace = { viewModel.toggleFindReplace(!uiState.isFindReplaceOpen) },
                    onShare = {
                        shareDocument(context, uiState.activeDocument.title, uiState.editorText)
                    },
                    onCopyRaw = {
                        copyToClipboard(context, "Markdown", uiState.editorText)
                    }
                )
            },
            bottomBar = {
                Column {
                    // Live Unicode & Emoji suggestions bar
                    EmojiSuggestionStrip(
                        suggestions = uiState.emojiSuggestions,
                        onSelectSuggestion = { viewModel.selectEmojiSuggestion(it) }
                    )

                    EditorFormattingToolbar(
                        onInsertText = { prefix, suffix -> viewModel.insertWrappingText(prefix, suffix) },
                        onHighlightSelection = { viewModel.highlightSelection() },
                        onOpenSyntaxCatalog = { viewModel.toggleSyntaxCatalog(true) },
                        onOpenUnicodePicker = { viewModel.toggleUnicodePicker(true) },
                        onOpenCtan = { viewModel.toggleCtanSheet(true) },
                        onOpenGraphs = { viewModel.toggleGraphDialog(true) }
                    )
                    EditorTelemetryBar(
                        content = uiState.editorText,
                        installedCtanCount = uiState.activeDocument.getInstalledPackageList().size,
                        viewMode = uiState.viewMode,
                        settings = uiState.settings
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isFindReplaceOpen) {
                        FindReplaceBar(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { viewModel.setFindQuery(it) },
                            replaceQuery = uiState.replaceQuery,
                            onReplaceQueryChange = { viewModel.setReplaceQuery(it) },
                            matchCount = uiState.searchMatchCount,
                            currentMatchIndex = uiState.currentMatchIndex,
                            onFindNext = { viewModel.findNext() },
                            onFindPrevious = { viewModel.findPrevious() },
                            onReplaceCurrent = { viewModel.replaceCurrent() },
                            onReplaceAll = { viewModel.replaceAll() },
                            onClose = { viewModel.toggleFindReplace(false) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (uiState.viewMode) {
                    ViewMode.EDITOR_ONLY -> {
                        // 100% Fullscreen Editor
                        EditorPane(
                            textFieldValue = uiState.textFieldValue,
                            onValueChange = { viewModel.onTextFieldValueChange(it) },
                            fontSizeSp = uiState.settings.editorFontSizeSp,
                            fontChoice = uiState.settings.fontChoice,
                            showLineNumbers = uiState.settings.showLineNumbers,
                            onHighlightSelection = { viewModel.highlightSelection() },
                            onToggleFullscreen = { viewModel.setViewMode(ViewMode.SPLIT) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    ViewMode.PREVIEW_ONLY -> {
                        // 100% Fullscreen Preview
                        Box(modifier = Modifier.fillMaxSize()) {
                            MarkdownPreviewWebView(
                                markdown = uiState.previewText,
                                installedPackageIds = uiState.activeDocument.getInstalledPackageList(),
                                previewFontSizeSp = uiState.settings.previewFontSizeSp,
                                fontChoice = uiState.settings.fontChoice,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { viewModel.setViewMode(ViewMode.SPLIT) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Exit Fullscreen",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    ViewMode.SPLIT -> {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isWide = maxWidth >= 600.dp || uiState.settings.isHorizontalSplit

                            if (isWide) {
                                // Side-by-Side: Editor on Left, Preview on Right
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(uiState.splitRatio)
                                            .fillMaxHeight()
                                    ) {
                                        EditorPane(
                                            textFieldValue = uiState.textFieldValue,
                                            onValueChange = { viewModel.onTextFieldValueChange(it) },
                                            fontSizeSp = uiState.settings.editorFontSizeSp,
                                            fontChoice = uiState.settings.fontChoice,
                                            showLineNumbers = uiState.settings.showLineNumbers,
                                            onHighlightSelection = { viewModel.highlightSelection() },
                                            onToggleFullscreen = { viewModel.setViewMode(ViewMode.EDITOR_ONLY) },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Glowing Split Divider
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF00E5FF), Color(0xFFC084FC), Color(0xFF1E293B))
                                                )
                                            )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f - uiState.splitRatio)
                                            .fillMaxHeight()
                                    ) {
                                        MarkdownPreviewWebView(
                                            markdown = uiState.previewText,
                                            installedPackageIds = uiState.activeDocument.getInstalledPackageList(),
                                            previewFontSizeSp = uiState.settings.previewFontSizeSp,
                                            fontChoice = uiState.settings.fontChoice,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            } else {
                                // Stacked Vertical: Editor on Top, Preview on Bottom
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(uiState.splitRatio)
                                            .fillMaxWidth()
                                    ) {
                                        EditorPane(
                                            textFieldValue = uiState.textFieldValue,
                                            onValueChange = { viewModel.onTextFieldValueChange(it) },
                                            fontSizeSp = uiState.settings.editorFontSizeSp,
                                            fontChoice = uiState.settings.fontChoice,
                                            showLineNumbers = uiState.settings.showLineNumbers,
                                            onHighlightSelection = { viewModel.highlightSelection() },
                                            onToggleFullscreen = { viewModel.setViewMode(ViewMode.EDITOR_ONLY) },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(2.dp)
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF00E5FF), Color(0xFFC084FC), Color(0xFF1E293B))
                                                )
                                            )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f - uiState.splitRatio)
                                            .fillMaxWidth()
                                    ) {
                                        MarkdownPreviewWebView(
                                            markdown = uiState.previewText,
                                            installedPackageIds = uiState.activeDocument.getInstalledPackageList(),
                                            previewFontSizeSp = uiState.settings.previewFontSizeSp,
                                            fontChoice = uiState.settings.fontChoice,
                                            modifier = Modifier.fillMaxSize()
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

    // Local Storage Vault Sheet
    if (uiState.isVaultSheetOpen) {
        LocalVaultSheet(
            vaultDirectoryPath = uiState.vaultDirectoryPath,
            vaultFiles = uiState.vaultFiles,
            activeDocumentTitle = uiState.activeDocument.title,
            onImportFromStorage = {
                importFileLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*"))
            },
            onExportCurrentDocument = {
                val filename = viewModel.localStorageManager.sanitizeFilename(uiState.activeDocument.title)
                exportFileLauncher.launch(filename)
            },
            onSyncAllToVault = {
                viewModel.syncAllToVault()
                Toast.makeText(context, "All documents synced to local storage vault", Toast.LENGTH_SHORT).show()
            },
            onReloadActiveFromVault = {
                viewModel.reloadActiveFromVault()
                Toast.makeText(context, "Reloaded active document from disk", Toast.LENGTH_SHORT).show()
            },
            onOpenVaultFile = { fileInfo ->
                viewModel.openVaultFile(fileInfo)
            },
            onShareVaultFile = { fileInfo ->
                shareDocument(context, fileInfo.name, uiState.editorText)
            },
            onDismiss = { viewModel.toggleVaultSheet(false) }
        )
    }

    // Unicode & Emoji Picker Sheet
    if (uiState.isUnicodePickerOpen) {
        UnicodePickerSheet(
            unicodeRepository = viewModel.unicodeRepository,
            onSelectUnicode = { viewModel.insertText(it) },
            onDismiss = { viewModel.toggleUnicodePicker(false) }
        )
    }

    // Syntax Palette Sheet
    if (uiState.isSyntaxCatalogOpen) {
        MarkdownSyntaxCatalogDialog(
            onInsertSnippet = { viewModel.insertText(it) },
            onDismiss = { viewModel.toggleSyntaxCatalog(false) }
        )
    }

    // CTAN Sheet Dialog
    if (uiState.isCtanSheetOpen) {
        CtanManagerSheet(
            installedPackageIds = uiState.activeDocument.getInstalledPackageList(),
            onTogglePackageInstall = { viewModel.toggleCtanPackage(it) },
            onInsertSnippet = { viewModel.insertText(it) },
            onDismiss = { viewModel.toggleCtanSheet(false) }
        )
    }

    // Graph Template Dialog
    if (uiState.isGraphDialogOpen) {
        GraphHelperDialog(
            onInsertGraphSnippet = { viewModel.insertText(it) },
            onDismiss = { viewModel.toggleGraphDialog(false) }
        )
    }

    // Settings Dialog
    if (uiState.isSettingsDialogOpen) {
        EditorSettingsDialog(
            settings = uiState.settings,
            onSettingsChanged = { viewModel.updateSettings(it) },
            onDismiss = { viewModel.toggleSettingsDialog(false) }
        )
    }

    // Rename Document Dialog
    if (uiState.isRenameDialogOpen) {
        RenameDocumentDialog(
            initialTitle = uiState.activeDocument.title,
            onConfirm = {
                viewModel.renameDocument(it)
                viewModel.toggleRenameDialog(false)
            },
            onDismiss = { viewModel.toggleRenameDialog(false) }
        )
    }
}

@Composable
fun EditorPane(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSizeSp: Int,
    fontChoice: EditorFont,
    showLineNumbers: Boolean,
    onHighlightSelection: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualTransformation = remember(textFieldValue.selection) {
        MarkdownVisualTransformation(selectedRange = textFieldValue.selection)
    }
    val fontFamily = when (fontChoice) {
        EditorFont.MONOSPACE -> FontFamily.Monospace
        EditorFont.SANS_SERIF -> FontFamily.SansSerif
        EditorFont.SERIF -> FontFamily.Serif
    }

    val linesCount = remember(textFieldValue.text) {
        textFieldValue.text.count { it == '\n' } + 1
    }
    val currentLineIndex = remember(textFieldValue.text, textFieldValue.selection) {
        val cursor = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
        textFieldValue.text.take(cursor).count { it == '\n' } + 1
    }

    // Performance optimization: Generate all line numbers in a single AnnotatedString instead of hundreds of Composable nodes
    val lineNumbersAnnotated = remember(linesCount, currentLineIndex) {
        AnnotatedString.Builder().apply {
            for (i in 1..linesCount) {
                val isCurrent = i == currentLineIndex
                val color = if (isCurrent) Color(0xFF00E5FF) else Color(0xFF334155)
                val weight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                pushStyle(SpanStyle(color = color, fontWeight = weight))
                append(i.toString())
                pop()
                if (i < linesCount) append("\n")
            }
        }.toAnnotatedString()
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .background(Color(0xFF090D14))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Line numbers gutter
            if (showLineNumbers) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .background(Color(0xFF070A10))
                        .drawBehind {
                            drawLine(
                                color = Color(0xFF1E293B),
                                start = Offset(size.width, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(top = 16.dp, end = 8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = lineNumbersAnnotated,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (fontSizeSp * 0.85f).sp,
                        lineHeight = (fontSizeSp * 1.5f).sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Text input editor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("markdown_editor_input"),
                    textStyle = TextStyle(
                        color = Color(0xFFF1F5F9),
                        fontSize = fontSizeSp.sp,
                        fontFamily = fontFamily,
                        lineHeight = (fontSizeSp * 1.5f).sp
                    ),
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(Color(0xFF00E5FF))
                )
            }
        }

        // Floating quick action when text / code is selected
        if (!textFieldValue.selection.collapsed) {
            Surface(
                onClick = onHighlightSelection,
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFFFEF08A)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatPaint,
                        contentDescription = "Highlight Code",
                        tint = Color(0xFFFEF08A),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Highlight Selection",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFEF08A)
                    )
                }
            }
        }
    }
}

@Composable
fun EditorTopBar(
    title: String,
    viewMode: ViewMode,
    installedCtanCount: Int,
    onOpenDrawer: () -> Unit,
    onSetViewMode: (ViewMode) -> Unit,
    onOpenFileTreeGraph: () -> Unit,
    onOpenSyntaxCatalog: () -> Unit,
    onOpenUnicodePicker: () -> Unit,
    onOpenCtan: () -> Unit,
    onOpenGraphs: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRename: () -> Unit,
    onOpenVault: () -> Unit,
    onToggleFindReplace: () -> Unit,
    onShare: () -> Unit,
    onCopyRaw: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = Color(0xF2090E1A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2238BDF8)),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Drawer toggle and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Document Drawer",
                        tint = Color(0xFF00E5FF)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenRename() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = "Rename",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Center: Interactive File Tree Graph view & ViewMode toggles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Interactive File Tree Graph button
                IconButton(
                    onClick = onOpenFileTreeGraph,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = "Interactive File Tree & Connections Graph",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Editor Only Fullscreen
                IconButton(
                    onClick = { onSetViewMode(ViewMode.EDITOR_ONLY) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (viewMode == ViewMode.EDITOR_ONLY) Color(0xFF1E293B) else Color.Transparent
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editor Fullscreen",
                        tint = if (viewMode == ViewMode.EDITOR_ONLY) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Split 50/50
                IconButton(
                    onClick = { onSetViewMode(ViewMode.SPLIT) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (viewMode == ViewMode.SPLIT) Color(0xFF1E293B) else Color.Transparent
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.VerticalSplit,
                        contentDescription = "Split View",
                        tint = if (viewMode == ViewMode.SPLIT) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Preview Only Fullscreen
                IconButton(
                    onClick = { onSetViewMode(ViewMode.PREVIEW_ONLY) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (viewMode == ViewMode.PREVIEW_ONLY) Color(0xFF1E293B) else Color.Transparent
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview Fullscreen",
                        tint = if (viewMode == ViewMode.PREVIEW_ONLY) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Right Actions: Emoji, Syntax, CTAN, Graphs, Settings, Overflow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Unicode & Emoji Palette
                IconButton(onClick = onOpenUnicodePicker) {
                    Icon(
                        imageVector = Icons.Default.Mood,
                        contentDescription = "Unicode & Emoji Palette",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Syntax Palette
                IconButton(onClick = onOpenSyntaxCatalog) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Syntax Palette",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // CTAN DB
                IconButton(onClick = onOpenCtan) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = "CTAN Packages",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Graphs / Diagrams
                IconButton(onClick = onOpenGraphs) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "Graphs & Diagrams",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Find & Replace
                IconButton(onClick = onToggleFindReplace) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Find & Replace",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Local Storage Vault
                IconButton(onClick = onOpenVault) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Local Storage Vault",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Settings
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Typography Settings",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Overflow Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Color(0xFF0F1523))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unicode & Emoji Palette", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Mood, null, tint = Color(0xFFFBBF24)) },
                            onClick = {
                                menuExpanded = false
                                onOpenUnicodePicker()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Interactive File Tree Graph", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AccountTree, null, tint = Color(0xFF00E5FF)) },
                            onClick = {
                                menuExpanded = false
                                onOpenFileTreeGraph()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Local Storage Vault (.md)", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Storage, null, tint = Color(0xFF00E5FF)) },
                            onClick = {
                                menuExpanded = false
                                onOpenVault()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Find & Replace", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                menuExpanded = false
                                onToggleFindReplace()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Syntax Palette Catalog", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.MenuBook, null, tint = Color(0xFFC084FC)) },
                            onClick = {
                                menuExpanded = false
                                onOpenSyntaxCatalog()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Document", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Share, null, tint = Color(0xFF38BDF8)) },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Markdown", color = Color.White) },
                            onClick = {
                                menuExpanded = false
                                onCopyRaw()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename Document", color = Color.White) },
                            onClick = {
                                menuExpanded = false
                                onOpenRename()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDocumentDialog(
    initialTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1523),
        title = {
            Text(
                text = "Rename Document",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedContainerColor = Color(0xFF090D14),
                    unfocusedContainerColor = Color(0xFF090D14),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black
                )
            ) {
                Text("Rename", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareDocument(context: Context, title: String, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Document via"))
}
