package com.example.ui.ctan

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CtanCatalog
import com.example.data.CtanRepository
import com.example.model.CtanCategory
import kotlinx.coroutines.launch
import com.example.model.CtanPackage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CtanManagerSheet(
    installedPackageIds: List<String>,
    onTogglePackageInstall: (packageId: String) -> Unit,
    onInsertSnippet: (snippet: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ctanRepository = remember { CtanRepository() }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CtanCategory?>(null) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    var packagesList by remember { mutableStateOf(CtanCatalog.packages) }

    fun refreshList() {
        val searched = CtanCatalog.search(searchQuery)
        packagesList = if (selectedCategory != null) {
            searched.filter { it.category == selectedCategory }
        } else {
            searched
        }
    }

    LaunchedEffect(searchQuery, selectedCategory) {
        refreshList()
    }

    fun checkCtanUpdatesOnline() {
        if (isCheckingUpdates) return
        isCheckingUpdates = true
        syncStatusMessage = "Checking CTAN.org 2.0 API live..."
        scope.launch {
            val result = ctanRepository.checkAllPackagesForUpdates()
            isCheckingUpdates = false
            syncStatusMessage = result.message
            refreshList()
        }
    }

    fun searchCtanOnline() {
        if (isSearchingOnline || searchQuery.isBlank()) return
        isSearchingOnline = true
        syncStatusMessage = "Querying CTAN.org database for '$searchQuery'..."
        scope.launch {
            val onlineResults = ctanRepository.searchCtanOnline(searchQuery)
            isSearchingOnline = false
            packagesList = if (selectedCategory != null) {
                onlineResults.filter { it.category == selectedCategory }
            } else {
                onlineResults
            }
            syncStatusMessage = "CTAN.org search returned ${onlineResults.size} packages"
        }
    }

    val allPackages = packagesList

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1523),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
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
                                Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFC084FC)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = "CTAN TeX Database",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CTAN Package Database",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${installedPackageIds.size} packages loaded in active document",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF38BDF8)
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

            Spacer(modifier = Modifier.height(10.dp))

            // CTAN.org Live API Status & Update Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF131D2E),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = syncStatusMessage ?: "Official Source: CTAN.org JSON 2.0 API",
                            fontSize = 11.sp,
                            color = if (syncStatusMessage != null) Color(0xFF34D399) else Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = { checkCtanUpdatesOnline() },
                        enabled = !isCheckingUpdates,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Color(0xFF00E5FF),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Checking...", fontSize = 10.sp, color = Color(0xFF00E5FF))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check CTAN", fontSize = 10.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar & CTAN Online Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Search 6,000+ CTAN packages (amsmath, tikz)...",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, "Clear", tint = Color.Gray)
                            }
                        }
                    },
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

                // Search CTAN Online Button
                Button(
                    onClick = { searchCtanOnline() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF38BDF8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    if (isSearchingOnline) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Online Search", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Online", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All Packages (${CtanCatalog.packages.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1E293B),
                            selectedLabelColor = Color(0xFF00E5FF),
                            containerColor = Color(0xFF111827),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
                items(CtanCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                        },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(cat.badgeColor).copy(alpha = 0.2f),
                            selectedLabelColor = Color(cat.badgeColor),
                            containerColor = Color(0xFF111827),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Package List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allPackages, key = { it.id }) { pkg ->
                    val isInstalled = installedPackageIds.contains(pkg.id)

                    CtanPackageCard(
                        pkg = pkg,
                        isInstalled = isInstalled,
                        onToggleInstall = { onTogglePackageInstall(pkg.id) },
                        onInsertSnippet = {
                            onInsertSnippet(pkg.sampleSnippet)
                            onDismiss()
                        },
                        onOpenDocs = {
                            try {
                                uriHandler.openUri(pkg.documentationUrl)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CtanPackageCard(
    pkg: CtanPackage,
    isInstalled: Boolean,
    onToggleInstall: () -> Unit,
    onInsertSnippet: () -> Unit,
    onOpenDocs: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isInstalled) Color(0xFF00E5FF).copy(alpha = 0.4f) else Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInstalled) Color(0xFF111D2D) else Color(0xFF0B101A)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "\\usepackage{${pkg.name}}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isInstalled) Color(0xFF00E5FF) else Color(0xFFE2E8F0)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "CTAN: v${pkg.version}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(pkg.category.badgeColor).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = pkg.category.label,
                        color = Color(pkg.category.badgeColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = pkg.caption,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = pkg.description,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )

            if (pkg.macros.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Macros (${pkg.macros.size}):",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = pkg.macros.keys.take(4).joinToString(", ") + if (pkg.macros.size > 4) "..." else "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Install/Remove Button
                Button(
                    onClick = onToggleInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInstalled) Color(0xFF065F46) else Color(0xFF1E293B),
                        contentColor = if (isInstalled) Color(0xFF34D399) else Color(0xFF38BDF8)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isInstalled) Icons.Default.Check else Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInstalled) "Loaded" else "Load CTAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Insert Equation / LaTeX Segment
                OutlinedButton(
                    onClick = onInsertSnippet,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFC084FC)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFC084FC), Color(0xFF818CF8)))
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Insert LaTeX Segment",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onOpenDocs,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "CTAN Portal",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
