package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.FileWithTags
import com.example.ui.components.CreateFileDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.FileListItem
import com.example.ui.components.TagFilterHeader
import com.example.ui.components.TagSelectionBottomSheet
import com.example.ui.components.TopPreviewCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val filteredFiles by viewModel.filteredFiles.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()

    // Scoped Storage: Storage Access Framework (SAF) Document Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris, context)
            Toast.makeText(context, "Imported ${uris.size} file(s) via Scoped Storage", Toast.LENGTH_SHORT).show()
        }
    }

    // Scoped Storage: Android Photo Picker (zero-permission media picker)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris, context)
            Toast.makeText(context, "Imported ${uris.size} media file(s)", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "File Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Scoped Storage & SQLite FTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Pick Media
                    IconButton(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier.testTag("pick_media_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Pick Media"
                        )
                    }

                    // Import Files via SAF
                    IconButton(
                        onClick = {
                            docPickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.testTag("import_saf_files_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Import Files (SAF)"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openCreateFileDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = "Create Document") },
                text = { Text("New Doc") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("create_document_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Indexing progress indicator banner
            AnimatedVisibility(visible = uiState.isIndexing) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Indexing file metadata & content for FTS search...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // TOP SECTION: Floating Instant Preview Card
            TopPreviewCard(
                fileWithTags = uiState.selectedFileForPreview,
                audioState = audioState,
                onClose = { viewModel.onSelectFileForPreview(null) },
                onAddTagClick = {
                    uiState.selectedFileForPreview?.let { viewModel.openTagSheet(it) }
                },
                onOpenExternal = {
                    uiState.selectedFileForPreview?.let { openFileInSystem(context, it) }
                },
                onShare = {
                    uiState.selectedFileForPreview?.let { shareFile(context, it) }
                },
                onTogglePlayAudio = { uri, id, title ->
                    viewModel.audioManager.togglePlayPause(uri, id, title)
                },
                onSeekAudio = { pos ->
                    viewModel.audioManager.seekTo(pos)
                }
            )

            // MIDDLE SECTION: Search Keywords & Tag Filter Chips
            TagFilterHeader(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                allTags = allTags,
                selectedTagIds = uiState.selectedTagFilterIds,
                onToggleTagFilter = { viewModel.onTagFilterToggled(it) },
                onClearFilters = { viewModel.onClearTagFilters() },
                totalCount = filteredFiles.size
            )

            // BOTTOM SECTION: File List (LazyColumn with Left-Right Item layout)
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty() || uiState.selectedTagFilterIds.isNotEmpty()) {
                                "No files match your search or filter."
                            } else {
                                "No files found. Import files via SAF or create a new note."
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
                    contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredFiles, key = { it.file.id }) { fileWithTags ->
                        FileListItem(
                            fileWithTags = fileWithTags,
                            isSelectedForPreview = uiState.selectedFileForPreview?.file?.id == fileWithTags.file.id,
                            onClick = { viewModel.onSelectFileForPreview(fileWithTags) },
                            onAddTagClick = { viewModel.openTagSheet(fileWithTags) },
                            onOpenWithSystem = { openFileInSystem(context, fileWithTags) },
                            onShare = { shareFile(context, fileWithTags) },
                            onShowDetails = { viewModel.openFileDetails(fileWithTags) },
                            onDelete = { viewModel.deleteFile(fileWithTags, context) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Tag Selection & Creation Workflow
    if (uiState.fileForTaggingSheet != null) {
        TagSelectionBottomSheet(
            fileWithTags = uiState.fileForTaggingSheet,
            allTags = allTags,
            onToggleTag = { tag -> viewModel.toggleTagForFile(tag) },
            onCreateAndAssignTag = { name, colorHex ->
                viewModel.createAndAssignTag(name, colorHex)
            },
            onDismiss = { viewModel.closeTagSheet() }
        )
    }

    // Create New File / Note Dialog
    if (uiState.showCreateFileDialog) {
        CreateFileDialog(
            onDismiss = { viewModel.closeCreateFileDialog() },
            onCreateFile = { name, content, tags ->
                viewModel.createNewNote(name, content, tags, context)
            }
        )
    }

    // File Details Dialog
    if (uiState.fileForDetailsDialog != null) {
        FileDetailsDialog(
            fileWithTags = uiState.fileForDetailsDialog,
            onDismiss = { viewModel.closeFileDetails() },
            onOpenExternal = {
                uiState.fileForDetailsDialog?.let { openFileInSystem(context, it) }
            },
            onShare = {
                uiState.fileForDetailsDialog?.let { shareFile(context, it) }
            }
        )
    }
}

private fun openFileInSystem(context: Context, fileWithTags: FileWithTags) {
    try {
        val uri = Uri.parse(fileWithTags.file.fileUri)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, fileWithTags.file.fileType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Open with..."))
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to open this file type: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareFile(context: Context, fileWithTags: FileWithTags) {
    try {
        val uri = Uri.parse(fileWithTags.file.fileUri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = fileWithTags.file.fileType
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Share File"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot share file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
