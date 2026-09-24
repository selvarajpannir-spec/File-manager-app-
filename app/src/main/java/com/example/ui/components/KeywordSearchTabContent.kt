package com.example.ui.components

import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FileExplorerTab
import com.example.ui.FileManagerViewModel
import com.example.ui.FileUiState
import com.example.ui.openFileInExternalApp
import com.example.ui.shareFile

@Composable
fun KeywordSearchTabContent(
    viewModel: FileManagerViewModel,
    uiState: FileUiState,
    audioState: com.example.util.AudioPlayerState,
    context: Context
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // MINIMIZED TOP PREVIEW CARD
        TopPreviewCard(
            item = uiState.selectedItemForPreview,
            audioState = audioState,
            highlightKeywords = uiState.lastScannedKeywords,
            onClose = { viewModel.onSelectFileForPreview(null) },
            onFullScreen = {
                uiState.selectedItemForPreview?.let {
                    viewModel.openFullScreenViewer(it, uiState.lastScannedKeywords)
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

        // KEYWORD SEARCH CONTROLLER CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Multi-Format Keyword Search",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Background notification tag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Background enabled",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Keyword Input Field
                OutlinedTextField(
                    value = uiState.keywordSearchInput,
                    onValueChange = { viewModel.onKeywordSearchInputChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("keyword_search_input_field"),
                    placeholder = {
                        Text(
                            text = "Search numbers, names, keywords (e.g. 9876543210, invoice)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (uiState.keywordSearchInput.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onKeywordSearchInputChanged("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear input", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.startBackgroundKeywordSearch() }),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Common Presets / Sample Keywords
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Presets:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val presets = listOf("phone, backup", "pdf, doc", "report, 2024", "contact, name", "invoice, statement")
                    presets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable {
                                viewModel.onKeywordSearchInputChanged(preset)
                            }
                        ) {
                            Text(
                                text = preset,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Storage Scope Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.keywordSearchRootPath == null,
                        onClick = { viewModel.setKeywordSearchRoot(null) },
                        label = { Text("Whole Storage", fontSize = 10.sp) }
                    )

                    uiState.storageLocations.take(5).forEach { loc ->
                        FilterChip(
                            selected = uiState.keywordSearchRootPath == loc.path,
                            onClick = { viewModel.setKeywordSearchRoot(loc.path) },
                            label = { Text("${loc.iconEmoji} ${loc.title}", fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Mode switch and Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Match Mode toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.toggleKeywordMatchAll() }
                    ) {
                        Switch(
                            checked = uiState.keywordSearchMatchAll,
                            onCheckedChange = { viewModel.toggleKeywordMatchAll() },
                            modifier = Modifier.size(width = 36.dp, height = 22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.keywordSearchMatchAll) "Match ALL" else "Match ANY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }

                    // Action button
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (uiState.isKeywordSearching) {
                            OutlinedButton(
                                onClick = { viewModel.cancelKeywordSearch() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop Scan", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startBackgroundKeywordSearch() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("start_keyword_search_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Scan", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // BACKGROUND PROGRESS CARD (When scanning)
        AnimatedVisibility(visible = uiState.isKeywordSearching) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Deep scanning storage: ${uiState.keywordSearchProgressPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${uiState.keywordSearchFoundCount} found",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (uiState.keywordSearchProgressPercent / 100f).coerceIn(0.01f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // RESULTS HEADER
        if (uiState.keywordSearchResults.isNotEmpty() || (!uiState.isKeywordSearching && uiState.lastScannedKeywords.isNotEmpty())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${uiState.keywordSearchResults.size} matching file(s) found (Streaming live)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )

                if (uiState.keywordSearchResults.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearKeywordSearchResults() },
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Clear", fontSize = 10.sp)
                    }
                }
            }
        }

        // RESULTS LIST (STREAMED LIVE BELOW INPUT)
        if (uiState.keywordSearchResults.isEmpty()) {
            if (!uiState.isKeywordSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (uiState.lastScannedKeywords.isNotEmpty()) {
                                "No matches found for: \"${uiState.lastScannedKeywords.joinToString(", ")}\""
                            } else {
                                "Enter keyword(s), mobile numbers, or contact names above and tap 'Start Scan'.\nResults stream below in real time with yellow keyword highlighting."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("keyword_search_results_list"),
                contentPadding = PaddingValues(bottom = 96.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(uiState.keywordSearchResults, key = { it.item.path }) { result ->
                    SearchMatchItem(
                        result = result,
                        isSelectedForPreview = uiState.selectedItemForPreview?.path == result.item.path,
                        onClick = {
                            if (result.item.file.isDirectory) {
                                viewModel.setActiveTab(FileExplorerTab.BROWSER)
                                viewModel.loadDirectory(result.item.path)
                            } else {
                                viewModel.openFileChoicePrompt(result.item, result.matchedKeywords)
                            }
                        },
                        onAddTagClick = { viewModel.openTagSheet(result.item) },
                        onOpenWithSystem = { openFileInExternalApp(context, result.item.file) },
                        onShare = { shareFile(context, result.item.file) },
                        onRename = { viewModel.openRenameDialog(result.item) },
                        onShowDetails = { viewModel.openDetailsDialog(result.item) },
                        onDelete = { viewModel.deleteItem(result.item) }
                    )
                }
            }
        }
    }
}
