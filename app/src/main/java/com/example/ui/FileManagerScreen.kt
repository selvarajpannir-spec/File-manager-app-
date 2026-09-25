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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import com.example.ui.components.FullScreenFileViewerDialog
import com.example.ui.components.KeywordSearchTabContent
import com.example.ui.components.OpenFilePromptDialog
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

    var showStorageMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showToolsMenu by remember { mutableStateOf(false) }

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
            // SINGLE UNIFIED NAVIGATION BAR (Consolidates 4 previous bars into 1 compact bar with dropdowns)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("single_unified_nav_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Section: Up Button + Path / Tab Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                if (uiState.activeTab != FileExplorerTab.BROWSER) {
                                    viewModel.setActiveTab(FileExplorerTab.BROWSER)
                                } else {
                                    viewModel.navigateUp()
                                }
                            },
                            modifier = Modifier.size(36.dp).testTag("nav_up_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Parent directory",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        if (uiState.activeTab == FileExplorerTab.BROWSER) {
                            BreadcrumbPathBar(
                                currentPath = uiState.currentPath,
                                onSegmentClick = { path ->
                                    viewModel.loadDirectory(path)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            val tabTitle = when (uiState.activeTab) {
                                FileExplorerTab.KEYWORD_SEARCH -> "🔍 Deep Keyword Search"
                                FileExplorerTab.TAGGED_FILES -> "🏷️ Tagged Files"
                                FileExplorerTab.STORAGE_INFO -> "📊 Storage Partitions"
                                else -> "📁 File Explorer"
                            }
                            Text(
                                text = tabTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setActiveTab(FileExplorerTab.BROWSER) }
                            )
                        }
                    }

                    // Right Section: 4 Compact Dropdown Action Menus
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 1. STORAGE DROPDOWN (📂 Storage)
                        Box {
                            IconButton(
                                onClick = { showStorageMenu = true },
                                modifier = Modifier.size(36.dp).testTag("nav_storage_dropdown_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Storage Partitions",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showStorageMenu,
                                onDismissRequest = { showStorageMenu = false },
                                modifier = Modifier.width(240.dp)
                            ) {
                                Text(
                                    text = "Select Storage Partition",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider()
                                uiState.storageLocations.forEach { loc ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(loc.iconEmoji, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = loc.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (uiState.currentPath == loc.path) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Text(
                                                        text = loc.path,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            showStorageMenu = false
                                            viewModel.setActiveTab(FileExplorerTab.BROWSER)
                                            viewModel.loadDirectory(loc.path)
                                        }
                                    )
                                }
                            }
                        }

                        // 2. FILTER & TAGS DROPDOWN (🏷️ Filter)
                        val hasActiveFilters = uiState.searchQuery.isNotEmpty() || uiState.selectedTagFilterIds.isNotEmpty()
                        Box {
                            IconButton(
                                onClick = { showFilterDialog = true },
                                modifier = Modifier.size(36.dp).testTag("nav_filter_dropdown_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Label,
                                        contentDescription = "Filter & Search",
                                        tint = if (hasActiveFilters) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (hasActiveFilters) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE11D48))
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. SORT DROPDOWN (🔃 Sort)
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.size(36.dp).testTag("nav_sort_dropdown_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort Files",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.width(220.dp)
                            ) {
                                Text(
                                    text = "Sort Order",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider()
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
                                                fontSize = 13.sp,
                                                fontWeight = if (uiState.sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                                                color = if (uiState.sortMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortMode(mode)
                                            showSortMenu = false
                                        }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (uiState.showHiddenFiles) "Hide Hidden Files" else "Show Hidden Files",
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleShowHiddenFiles()
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        // 4. TOOLS & TABS MENU (⋮ Menu)
                        Box {
                            IconButton(
                                onClick = { showToolsMenu = true },
                                modifier = Modifier.size(36.dp).testTag("nav_tools_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Tools & Navigation",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showToolsMenu,
                                onDismissRequest = { showToolsMenu = false },
                                modifier = Modifier.width(260.dp)
                            ) {
                                // Tab Switchers
                                Text(
                                    text = "Navigation & Views",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                )

                                DropdownMenuItem(
                                    text = { Text("📁 File Explorer", fontWeight = if (uiState.activeTab == FileExplorerTab.BROWSER) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.setActiveTab(FileExplorerTab.BROWSER)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔍 Deep Keyword Search", fontWeight = if (uiState.activeTab == FileExplorerTab.KEYWORD_SEARCH) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.setActiveTab(FileExplorerTab.KEYWORD_SEARCH)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🏷️ Tagged Files", fontWeight = if (uiState.activeTab == FileExplorerTab.TAGGED_FILES) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.setActiveTab(FileExplorerTab.TAGGED_FILES)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📊 Storage Partitions Stats", fontWeight = if (uiState.activeTab == FileExplorerTab.STORAGE_INFO) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.setActiveTab(FileExplorerTab.STORAGE_INFO)
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // Actions
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Auto Organize Files", fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.openAutoOrganizeDialog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("New Folder")
                                        }
                                    },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.openCreateFolderDialog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("New Text File")
                                        }
                                    },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.openCreateFileDialog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Import via SAF")
                                        }
                                    },
                                    onClick = {
                                        showToolsMenu = false
                                        docPickerLauncher.launch(arrayOf("*/*"))
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // Settings
                                DropdownMenuItem(
                                    text = {
                                        Text(if (themeMode == ThemeMode.DARK) "☀️ Light Theme" else "🌙 Dark Theme")
                                    },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.toggleTheme()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("🔤 Font Size (${fontScale.displayName})")
                                    },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.openFontSizeDialog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("🔄 Refresh Directory")
                                    },
                                    onClick = {
                                        showToolsMenu = false
                                        viewModel.refreshCurrentDirectory()
                                    }
                                )
                            }
                        }
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
            // BACKGROUND SCAN ACTIVE MINI-BANNER
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

            // Filter Dialog Popup
            if (showFilterDialog) {
                val hasActive = uiState.searchQuery.isNotEmpty() || uiState.selectedTagFilterIds.isNotEmpty()
                AlertDialog(
                    onDismissRequest = { showFilterDialog = false },
                    title = {
                        Text("Search & Tag Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            androidx.compose.material3.OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                placeholder = { Text("Filter files by name...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Filter by Tag:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allTags.forEach { tag ->
                                    val isSelected = uiState.selectedTagFilterIds.contains(tag.tagId)
                                    androidx.compose.material3.FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.onTagFilterToggled(tag.tagId) },
                                        label = { Text("#${tag.tagName}", fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFilterDialog = false }) {
                            Text("Done")
                        }
                    },
                    dismissButton = {
                        if (hasActive) {
                            TextButton(onClick = {
                                viewModel.onClearTagFilters()
                                showFilterDialog = false
                            }) {
                                Text("Clear All")
                            }
                        }
                    }
                )
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

    // Open File Choice Prompt Dialog (Internal vs External)
    if (uiState.itemForOpenFilePrompt != null) {
        val item = uiState.itemForOpenFilePrompt!!
        OpenFilePromptDialog(
            item = item,
            onOpenInternal = {
                viewModel.openFullScreenViewer(item, uiState.openPromptKeywords)
            },
            onOpenExternal = {
                openFileInExternalApp(context, item.file)
                viewModel.closeFileChoicePrompt()
            },
            onDismiss = {
                viewModel.closeFileChoicePrompt()
            }
        )
    }

    // Full Screen In-App Viewer Dialog (Supports All Formats with Yellow Keyword Highlighting)
    if (uiState.itemForFullScreenViewer != null) {
        val item = uiState.itemForFullScreenViewer!!
        FullScreenFileViewerDialog(
            item = item,
            initialKeywords = uiState.fullScreenKeywords,
            audioState = audioState,
            onTogglePlayAudio = { uri, id, title ->
                viewModel.audioManager.togglePlayPause(uri, id, title)
            },
            onSeekAudio = { pos ->
                viewModel.audioManager.seekTo(pos)
            },
            onOpenExternal = {
                openFileInExternalApp(context, item.file)
            },
            onShare = {
                shareFile(context, item.file)
            },
            onDismiss = {
                viewModel.closeFullScreenViewer()
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
        // 1. PREVIEW SPACE: 40% OF SCREEN HEIGHT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f)
                .padding(bottom = 2.dp)
                .testTag("preview_40_percent_container"),
            contentAlignment = Alignment.Center
        ) {
            TopPreviewCard(
                item = uiState.selectedItemForPreview,
                audioState = audioState,
                highlightKeywords = uiState.lastScannedKeywords,
                onClose = { viewModel.onSelectFileForPreview(null) },
                onPreviewClick = {
                    uiState.selectedItemForPreview?.let {
                        viewModel.openFileChoicePrompt(it, uiState.lastScannedKeywords)
                    }
                },
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
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. FILE LIST: 40% OF SCREEN HEIGHT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f)
                .testTag("file_list_40_percent_container")
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (query.isNotEmpty() || uiState.selectedTagFilterIds.isNotEmpty()) {
                                    "No items match your criteria."
                                } else {
                                    "Directory empty or protected."
                                },
                                style = MaterialTheme.typography.bodySmall,
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
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
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

        // 3. ADMOB BANNER AD PLACEHOLDER: 10% OF SCREEN HEIGHT (FOR FUTURE ADMOB)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.10f)
                .testTag("admob_banner_placeholder_container")
        ) {
            AdMobBannerPlaceholder()
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
            highlightKeywords = uiState.lastScannedKeywords,
            onClose = { viewModel.onSelectFileForPreview(null) },
            onPreviewClick = {
                uiState.selectedItemForPreview?.let {
                    viewModel.openFileChoicePrompt(it, uiState.lastScannedKeywords)
                }
            },
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
                        imageVector = Icons.AutoMirrored.Filled.Label,
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

/**
 * AdMob Banner placeholder (occupying ~10% screen height) reserved for future AdMob banner ads.
 */
@Composable
fun AdMobBannerPlaceholder(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF59E0B)
                ) {
                    Text(
                        text = "Ad",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Files+ High Performance Storage",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "AdMob Banner Ad Slot • 320x50 / Adaptive Ready",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Files+ ⚡",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
