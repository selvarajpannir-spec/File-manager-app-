package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FileSystemItem
import com.example.ui.components.AutoOrganizeDialog
import com.example.ui.components.CreateFileDialog
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.FileListItem
import com.example.ui.components.FontSizeDialog
import com.example.ui.components.KeywordSearchTabContent
import com.example.ui.components.PermissionDialog
import com.example.ui.components.RenameFileDialog
import com.example.ui.components.TagChipItem
import com.example.ui.components.TagFilterHeader
import com.example.ui.components.TagSelectionBottomSheet
import com.example.ui.components.TopPreviewCard
import com.example.util.AutoOrganizeManager
import com.example.util.FileCategory
import com.example.util.FileUtil
import com.example.util.StorageLocation
import com.example.util.StorageSearchScanner
import com.example.util.ThemeMode
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSortSubMenu by remember { mutableStateOf(false) }
    var showLocationsSubMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    // Check permissions on resume
    LaunchedEffect(Unit) {
        viewModel.refreshStoragePermission(context)
    }

    // Show status messages
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    // SAF File Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris, context)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Up Button
                        IconButton(
                            onClick = { viewModel.navigateUp() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Parent directory",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Path Breadcrumbs
                        BreadcrumbPathBar(
                            currentPath = uiState.currentPath,
                            onSegmentClick = { path ->
                                viewModel.setActiveTab(FileExplorerTab.BROWSER)
                                viewModel.loadDirectory(path)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                actions = {
                    // 3-DOT MENU DRAWER (Contains all top bar items + Auto Organize)
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("top_bar_overflow_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options menu",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = {
                                showOverflowMenu = false
                                showSortSubMenu = false
                                showLocationsSubMenu = false
                            },
                            modifier = Modifier.width(260.dp)
                        ) {
                            // 1. AUTO ORGANIZE FILES (HIGHLIGHTED FEATURE)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Auto Organize",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "NEW",
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Sort files into category folders",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.openAutoOrganizeDialog()
                                },
                                modifier = Modifier.testTag("menu_auto_organize_item")
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // 2. THEME TOGGLE (☀️ / 🌙)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = if (themeMode == ThemeMode.DARK) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (themeMode == ThemeMode.DARK) "Light Theme (☀️)" else "Dark Theme (🌙)",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleTheme()
                                },
                                modifier = Modifier.testTag("menu_theme_toggle_item")
                            )

                            // 3. FONT SIZE ADJUSTMENT (🔤)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.FormatSize,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Font Size (${fontScale.displayName})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.openFontSizeDialog()
                                },
                                modifier = Modifier.testTag("menu_font_size_item")
                            )

                            // 4. TOGGLE HIDDEN FILES (👁️)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (uiState.showHiddenFiles) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = if (uiState.showHiddenFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (uiState.showHiddenFiles) "Hide Hidden Files" else "Show Hidden Files",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleShowHiddenFiles()
                                },
                                modifier = Modifier.testTag("menu_hidden_files_item")
                            )

                            // 5. SORT FILES SUBMENU
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Sort,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Sort Files", style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (showSortSubMenu) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showSortSubMenu = !showSortSubMenu
                                    showLocationsSubMenu = false
                                }
                            )

                            if (showSortSubMenu) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Column {
                                        SortMode.entries.forEach { mode ->
                                            val label = when (mode) {
                                                SortMode.NAME_ASC -> "Name (A to Z)"
                                                SortMode.NAME_DESC -> "Name (Z to A)"
                                                SortMode.DATE_DESC -> "Date (Newest first)"
                                                SortMode.DATE_ASC -> "Date (Oldest first)"
                                                SortMode.SIZE_DESC -> "Size (Largest first)"
                                                SortMode.SIZE_ASC -> "Size (Smallest first)"
                                                SortMode.TYPE -> "Type / Extension"
                                            }
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (uiState.sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (uiState.sortMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setSortMode(mode)
                                                    showOverflowMenu = false
                                                    showSortSubMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 6. STORAGE LOCATIONS SUBMENU
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Storage Partitions", style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (showLocationsSubMenu) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showLocationsSubMenu = !showLocationsSubMenu
                                    showSortSubMenu = false
                                }
                            )

                            if (showLocationsSubMenu) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Column {
                                        uiState.storageLocations.forEach { loc ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(loc.iconEmoji, fontSize = 14.sp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(loc.title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    showLocationsSubMenu = false
                                                    viewModel.setActiveTab(FileExplorerTab.BROWSER)
                                                    viewModel.loadDirectory(loc.path)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // 7. REFRESH DIRECTORY (🔄)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Refresh", style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.refreshCurrentDirectory()
                                },
                                modifier = Modifier.testTag("menu_refresh_item")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == FileExplorerTab.BROWSER,
                    onClick = { viewModel.setActiveTab(FileExplorerTab.BROWSER) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Explorer") },
                    label = { Text("Explorer") }
                )
                NavigationBarItem(
                    selected = uiState.activeTab == FileExplorerTab.KEYWORD_SEARCH,
                    onClick = { viewModel.setActiveTab(FileExplorerTab.KEYWORD_SEARCH) },
                    icon = { Icon(Icons.Default.ManageSearch, contentDescription = "Keyword Search") },
                    label = { Text("Keywords") }
                )
                NavigationBarItem(
                    selected = uiState.activeTab == FileExplorerTab.TAGGED_FILES,
                    onClick = { viewModel.setActiveTab(FileExplorerTab.TAGGED_FILES) },
                    icon = { Icon(Icons.Default.Label, contentDescription = "Tagged Files") },
                    label = { Text("Tags") }
                )
                NavigationBarItem(
                    selected = uiState.activeTab == FileExplorerTab.STORAGE_INFO,
                    onClick = { viewModel.setActiveTab(FileExplorerTab.STORAGE_INFO) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Storage Stats") },
                    label = { Text("Partitions") }
                )
            }
        },
        floatingActionButton = {
            if (uiState.activeTab == FileExplorerTab.BROWSER) {
                Column(horizontalAlignment = Alignment.End) {
                    if (showFabMenu) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                TextButton(
                                    onClick = {
                                        showFabMenu = false
                                        viewModel.openCreateFolderDialog()
                                    }
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("New Folder")
                                }
                                TextButton(
                                    onClick = {
                                        showFabMenu = false
                                        viewModel.openCreateFileDialog()
                                    }
                                ) {
                                    Icon(Icons.Default.NoteAdd, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("New Text File")
                                }
                                TextButton(
                                    onClick = {
                                        showFabMenu = false
                                        docPickerLauncher.launch(arrayOf("*/*"))
                                    }
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Import via SAF")
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("main_action_fab")
                    ) {
                        Icon(
                            imageVector = if (showFabMenu) Icons.Default.Folder else Icons.Default.Add,
                            contentDescription = "Actions"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Storage Permission Warning Banner on Android 11+
            if (!uiState.hasStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All Files Access required for system files & deep keyword search",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp)
                        }
                    }
                }
            }

            // BACKGROUND SCAN ACTIVE MINI-BANNER (Shown across other tabs if scan is running)
            if (uiState.isKeywordSearching && uiState.activeTab != FileExplorerTab.KEYWORD_SEARCH) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setActiveTab(FileExplorerTab.KEYWORD_SEARCH) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Keyword scan running in background (${uiState.keywordSearchScannedCount} files, ${uiState.keywordSearchFoundCount} found)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "View →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Tabs Content
            when (uiState.activeTab) {
                FileExplorerTab.BROWSER -> {
                    BrowserTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        allTags = allTags,
                        audioState = audioState,
                        context = context
                    )
                }
                FileExplorerTab.KEYWORD_SEARCH -> {
                    KeywordSearchTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        audioState = audioState,
                        context = context
                    )
                }
                FileExplorerTab.TAGGED_FILES -> {
                    TaggedFilesTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        allTags = allTags,
                        audioState = audioState,
                        context = context
                    )
                }
                FileExplorerTab.STORAGE_INFO -> {
                    StorageInfoTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        context = context
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Tag Selection
    if (uiState.itemForTaggingSheet != null) {
        TagSelectionBottomSheet(
            item = uiState.itemForTaggingSheet,
            allTags = allTags,
            onToggleTag = { tag -> viewModel.toggleTagForItem(tag) },
            onCreateAndAssignTag = { name, colorHex ->
                viewModel.createAndAssignTag(name, colorHex)
            },
            onDismiss = { viewModel.closeTagSheet() }
        )
    }

    // Create New Folder Dialog
    if (uiState.showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { viewModel.closeCreateFolderDialog() },
            onCreateFolder = { folderName ->
                viewModel.createFolder(folderName)
            }
        )
    }

    // Create New File Dialog
    if (uiState.showCreateFileDialog) {
        CreateFileDialog(
            onDismiss = { viewModel.closeCreateFileDialog() },
            onCreateFile = { fileName, content ->
                viewModel.createTextFile(fileName, content)
            }
        )
    }

    // Font Size Dialog
    if (uiState.showFontSizeDialog) {
        FontSizeDialog(
            currentScale = fontScale,
            onSelectScale = { scale ->
                viewModel.setFontScale(scale)
            },
            onDismiss = { viewModel.closeFontSizeDialog() }
        )
    }

    // File Access Permission Prompt Dialog
    if (uiState.showPermissionPromptDialog && !uiState.hasStoragePermission) {
        PermissionDialog(
            onDismiss = { viewModel.dismissPermissionPrompt() },
            onGrantTriggered = { viewModel.dismissPermissionPrompt() }
        )
    }

    // File Details Dialog
    if (uiState.itemForDetailsDialog != null) {
        FileDetailsDialog(
            item = uiState.itemForDetailsDialog,
            onDismiss = { viewModel.closeDetailsDialog() },
            onOpenExternal = {
                uiState.itemForDetailsDialog?.let { openFileInExternalApp(context, it.file) }
            },
            onShare = {
                uiState.itemForDetailsDialog?.let { shareFile(context, it.file) }
            }
        )
    }

    // Rename Dialog
    if (uiState.itemForRenameDialog != null) {
        RenameFileDialog(
            item = uiState.itemForRenameDialog,
            onDismiss = { viewModel.closeRenameDialog() },
            onRename = { newName ->
                viewModel.renameItem(newName)
            }
        )
    }

    // Auto Organize Files Dialog
    if (uiState.showAutoOrganizeDialog) {
        AutoOrganizeDialog(
            initialFolders = uiState.organizeFolderTargets,
            defaultCategories = AutoOrganizeManager.DEFAULT_CATEGORIES,
            progress = uiState.organizeProgress,
            onStartOrganize = { selectedFolders, selectedCategories ->
                viewModel.startAutoOrganize(selectedFolders, selectedCategories)
            },
            onCancelOrganize = {
                viewModel.cancelAutoOrganize()
            },
            onOpenOrganizedFolder = { targetPath ->
                viewModel.setActiveTab(FileExplorerTab.BROWSER)
                viewModel.loadDirectory(targetPath)
                viewModel.closeAutoOrganizeDialog()
            },
            onDismiss = {
                viewModel.closeAutoOrganizeDialog()
            }
        )
    }
}

@Composable
fun BrowserTabContent(
    viewModel: FileManagerViewModel,
    uiState: FileUiState,
    allTags: List<com.example.data.local.entity.TagEntity>,
    audioState: com.example.util.AudioPlayerState,
    context: Context
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // TOP PREVIEW CARD
        TopPreviewCard(
            item = uiState.selectedItemForPreview,
            audioState = audioState,
            onClose = { viewModel.onSelectFileForPreview(null) },
            onAddTagClick = {
                uiState.selectedItemForPreview?.let { viewModel.openTagSheet(it) }
            },
            onOpenExternal = {
                uiState.selectedItemForPreview?.let { openFileInExternalApp(context, it.file) }
            },
            onShare = {
                uiState.selectedItemForPreview?.let { shareFile(context, it.file) }
            },
            onTogglePlayAudio = { uri, id, title ->
                viewModel.audioManager.togglePlayPause(uri, id, title)
            },
            onSeekAudio = { pos ->
                viewModel.audioManager.seekTo(pos)
            }
        )

        // FAST FILE NAME & TAG FILTER HEADER
        TagFilterHeader(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
            allTags = allTags,
            selectedTagIds = uiState.selectedTagFilterIds,
            onToggleTagFilter = { viewModel.onTagFilterToggled(it) },
            onClearFilters = { viewModel.onClearTagFilters() },
            totalCount = uiState.currentItems.size,
            onOpenKeywordSearch = { viewModel.setActiveTab(FileExplorerTab.KEYWORD_SEARCH) }
        )

        // QUICK STORAGE CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            uiState.storageLocations.take(6).forEach { loc ->
                val isCurrent = uiState.currentPath == loc.path
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { viewModel.loadDirectory(loc.path) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(loc.iconEmoji, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = loc.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // DIRECTORY SUMMARY ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val count = uiState.currentItems.size
            val isSystem = FileUtil.isSystemPath(uiState.currentPath)

            Text(
                text = "$count items ${if (isSystem) "• System Partition" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSystem) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSystem) FontWeight.Bold else FontWeight.Normal
            )

            Text(
                text = uiState.sortMode.name.replace("_", " "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // LOADING OR FILE LIST
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else {
            val query = uiState.searchQuery.lowercase(Locale.ROOT)
            val filteredItems = uiState.currentItems.filter { item ->
                val matchesQuery = query.isEmpty() || item.name.lowercase(Locale.ROOT).contains(query)
                val matchesTag = uiState.selectedTagFilterIds.isEmpty() || item.tags.any { it.tagId in uiState.selectedTagFilterIds }
                matchesQuery && matchesTag
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (query.isNotEmpty() || uiState.selectedTagFilterIds.isNotEmpty()) {
                                "No items in this directory match your criteria."
                            } else {
                                "This directory is empty or protected."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("file_list_lazy_column"),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(filteredItems, key = { it.path }) { item ->
                        FileListItem(
                            item = item,
                            isSelectedForPreview = uiState.selectedItemForPreview?.path == item.path,
                            onClick = {
                                if (item.isDirectory) {
                                    viewModel.loadDirectory(item.path)
                                } else {
                                    viewModel.onSelectFileForPreview(item)
                                }
                            },
                            onAddTagClick = { viewModel.openTagSheet(item) },
                            onOpenWithSystem = { openFileInExternalApp(context, item.file) },
                            onShare = { shareFile(context, item.file) },
                            onRename = { viewModel.openRenameDialog(item) },
                            onShowDetails = { viewModel.openDetailsDialog(item) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaggedFilesTabContent(
    viewModel: FileManagerViewModel,
    uiState: FileUiState,
    allTags: List<com.example.data.local.entity.TagEntity>,
    audioState: com.example.util.AudioPlayerState,
    context: Context
) {
    val dbFilesWithTags by viewModel.allDbFilesWithTags.collectAsStateWithLifecycle()

    val taggedItems = remember(dbFilesWithTags, uiState.selectedTagFilterIds, uiState.searchQuery) {
        val query = uiState.searchQuery.lowercase(Locale.ROOT)
        dbFilesWithTags
            .filter { fileWithTags ->
                val hasTags = fileWithTags.tags.isNotEmpty()
                val matchesFilter = uiState.selectedTagFilterIds.isEmpty() || fileWithTags.tags.any { it.tagId in uiState.selectedTagFilterIds }
                val matchesQuery = query.isEmpty() || fileWithTags.file.fileName.lowercase(Locale.ROOT).contains(query)
                hasTags && matchesFilter && matchesQuery
            }
            .map { fileWithTags ->
                val file = if (fileWithTags.file.filePath != null) {
                    File(fileWithTags.file.filePath)
                } else {
                    File(context.filesDir, fileWithTags.file.fileName)
                }
                FileSystemItem(
                    file = file,
                    name = fileWithTags.file.fileName,
                    path = fileWithTags.file.filePath ?: file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = fileWithTags.file.fileSize,
                    formattedSize = FileUtil.formatFileSize(fileWithTags.file.fileSize),
                    lastModified = fileWithTags.file.lastModified,
                    category = FileUtil.getMimeTypeCategory(fileWithTags.file.fileName, fileWithTags.file.fileType),
                    tags = fileWithTags.tags,
                    dbFileId = fileWithTags.file.id
                )
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopPreviewCard(
            item = uiState.selectedItemForPreview,
            audioState = audioState,
            onClose = { viewModel.onSelectFileForPreview(null) },
            onAddTagClick = {
                uiState.selectedItemForPreview?.let { viewModel.openTagSheet(it) }
            },
            onOpenExternal = {
                uiState.selectedItemForPreview?.let { openFileInExternalApp(context, it.file) }
            },
            onShare = {
                uiState.selectedItemForPreview?.let { shareFile(context, it.file) }
            },
            onTogglePlayAudio = { uri, id, title ->
                viewModel.audioManager.togglePlayPause(uri, id, title)
            },
            onSeekAudio = { pos ->
                viewModel.audioManager.seekTo(pos)
            }
        )

        TagFilterHeader(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
            allTags = allTags,
            selectedTagIds = uiState.selectedTagFilterIds,
            onToggleTagFilter = { viewModel.onTagFilterToggled(it) },
            onClearFilters = { viewModel.onClearTagFilters() },
            totalCount = taggedItems.size,
            onOpenKeywordSearch = { viewModel.setActiveTab(FileExplorerTab.KEYWORD_SEARCH) }
        )

        if (taggedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No tagged files found.\nTap '+' on any file to assign custom color tags!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(taggedItems, key = { it.path }) { item ->
                    FileListItem(
                        item = item,
                        isSelectedForPreview = uiState.selectedItemForPreview?.path == item.path,
                        onClick = {
                            if (item.file.exists() && item.file.isDirectory) {
                                viewModel.setActiveTab(FileExplorerTab.BROWSER)
                                viewModel.loadDirectory(item.path)
                            } else {
                                viewModel.onSelectFileForPreview(item)
                            }
                        },
                        onAddTagClick = { viewModel.openTagSheet(item) },
                        onOpenWithSystem = { openFileInExternalApp(context, item.file) },
                        onShare = { shareFile(context, item.file) },
                        onRename = { viewModel.openRenameDialog(item) },
                        onShowDetails = { viewModel.openDetailsDialog(item) },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun StorageInfoTabContent(
    viewModel: FileManagerViewModel,
    uiState: FileUiState,
    context: Context
) {
    val stats = uiState.storageStats

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Storage Usage Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Internal Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stats.usedPercentage}% Used",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { stats.usedPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Used: ${FileUtil.formatFileSize(stats.usedBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Free: ${FileUtil.formatFileSize(stats.freeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total: ${FileUtil.formatFileSize(stats.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // System Partitions & Mount Points
        item {
            Text(
                text = "System Partitions & Mounts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(uiState.storageLocations) { location ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (location.isSystem) Color(0xFFDC2626).copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.setActiveTab(FileExplorerTab.BROWSER)
                        viewModel.loadDirectory(location.path)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(location.iconEmoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = location.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (location.isSystem) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFDC2626).copy(alpha = 0.15f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("SYSTEM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }
                                }
                            }
                            Text(
                                text = location.path,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun BreadcrumbPathBar(
    currentPath: String,
    onSegmentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(currentPath) {
        if (currentPath.isEmpty() || currentPath == "/") {
            listOf(PathSegment("Root (/)", "/"))
        } else {
            val parts = currentPath.split("/").filter { it.isNotEmpty() }
            val list = mutableListOf(PathSegment("Root", "/"))
            var accumulated = ""
            for (part in parts) {
                accumulated += "/$part"
                list.add(PathSegment(part, accumulated))
            }
            list
        }
    }

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, segment ->
            val isLast = index == segments.size - 1
            Text(
                text = segment.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(!isLast) { onSegmentClick(segment.fullPath) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            if (!isLast) {
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

data class PathSegment(val name: String, val fullPath: String)

fun openFileInExternalApp(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = FileUtil.getMimeTypeCategory(file.name, null)
        val intentMime = when (mimeType) {
            FileCategory.PDF -> "application/pdf"
            FileCategory.IMAGE -> "image/*"
            FileCategory.VIDEO -> "video/*"
            FileCategory.AUDIO -> "audio/*"
            FileCategory.TEXT, FileCategory.CODE -> "text/*"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, intentMime)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Share file"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot share file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
