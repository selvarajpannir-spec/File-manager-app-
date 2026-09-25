package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FileSystemItem
import com.example.util.AudioPlayerState
import com.example.util.FileCategory
import com.example.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Description

/**
 * Top preview card sized cleanly for the ~40% display area.
 */
@Composable
fun TopPreviewCard(
    item: FileSystemItem?,
    audioState: AudioPlayerState,
    highlightKeywords: List<String> = emptyList(),
    onClose: () -> Unit,
    onPreviewClick: () -> Unit,
    onAddTagClick: () -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onTogglePlayAudio: (Uri, Long, String) -> Unit,
    onSeekAudio: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) {
        return
    }

    val category = item.category
    val categoryColor = getCategoryColor(category)
    val uri = remember(item.path) { Uri.fromFile(item.file) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("top_preview_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Header row: Compact Title + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = category.name,
                            color = categoryColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onPreviewClick,
                        modifier = Modifier.size(28.dp).testTag("top_preview_fullscreen_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Open File Options",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (!item.isDirectory) {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenExternal,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in External App",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // ❌ Close Preview Button - Returns to full screen file list
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("top_preview_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Preview",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Interactive Content Preview Box - Takes full remaining space inside the 40% card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable { onPreviewClick() },
                contentAlignment = Alignment.Center
            ) {
                when (category) {
                    FileCategory.PDF -> {
                        CompactPdfPreview(uri = uri, onFullScreen = onPreviewClick)
                    }
                    FileCategory.IMAGE -> {
                        CompactImagePreview(uri = uri)
                    }
                    FileCategory.VIDEO -> {
                        CompactVideoPreview(file = item.file, uri = uri, onFullScreen = onPreviewClick)
                    }
                    FileCategory.AUDIO -> {
                        CompactAudioPreview(
                            fileId = item.dbFileId ?: item.path.hashCode().toLong(),
                            fileName = item.name,
                            uri = uri,
                            audioState = audioState,
                            onTogglePlay = { onTogglePlayAudio(uri, item.dbFileId ?: item.path.hashCode().toLong(), item.name) },
                            onSeek = onSeekAudio
                        )
                    }
                    FileCategory.DOCUMENT -> {
                        CompactOfficePreview(file = item.file, keywords = highlightKeywords, onFullScreen = onPreviewClick)
                    }
                    FileCategory.TEXT, FileCategory.CODE -> {
                        CompactCodeTextPreview(file = item.file, keywords = highlightKeywords)
                    }
                    FileCategory.SYSTEM_BINARY -> {
                        CompactHexPreview(file = item.file)
                    }
                    FileCategory.FOLDER -> {
                        CompactFolderPreview(item = item)
                    }
                    else -> {
                        CompactGenericPreview(item = item, onFullScreen = onPreviewClick)
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Bottom info line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (item.isDirectory) item.permissions else "${item.formattedSize} • ${item.permissions}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                Text(
                    text = "Tap preview area to Open (Internal / External) →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { onPreviewClick() }
                )
            }
        }
    }
}

@Composable
fun CompactCodeTextPreview(file: File, keywords: List<String>) {
    var textSnippet by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }

    LaunchedEffect(file.absolutePath) {
        isLoading = true
        textSnippet = withContext(Dispatchers.IO) {
            try {
                file.useLines { lines ->
                    lines.take(20).joinToString("\n")
                }
            } catch (e: Exception) {
                "Text preview unavailable"
            }
        }
        isLoading = false
    }

    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                text = buildHighlightedText(textSnippet ?: "Empty file", keywords),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CompactPdfPreview(uri: Uri, onFullScreen: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(uri) { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            FileUtil.renderPdfFirstPage(context, uri)
        }
        isLoading = false
    }

    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    } else if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "PDF Page 1",
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Fit
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = Color(0xFFE11D48),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PDF Document Ready",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap to view full document & yellow keyword search",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompactImagePreview(uri: Uri) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(uri)
            .crossfade(true)
            .build(),
        contentDescription = "Image Preview",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun CompactAudioPreview(
    fileId: Long,
    fileName: String,
    uri: Uri,
    audioState: AudioPlayerState,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val isThisPlaying = audioState.currentFileId == fileId && audioState.isPlaying
    val currentMs = if (audioState.currentFileId == fileId) audioState.currentPositionMs else 0L
    val durationMs = if (audioState.currentFileId == fileId && audioState.durationMs > 0) audioState.durationMs else 180000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isThisPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isThisPlaying) "Playing Audio..." else "Tap play or scrub track",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = currentMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth().height(22.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = FileUtil.formatDuration(currentMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = FileUtil.formatDuration(durationMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CompactHexPreview(file: File) {
    var hexContent by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    LaunchedEffect(file.absolutePath) {
        hexContent = withContext(Dispatchers.IO) {
            FileUtil.generateHexDump(file, maxBytes = 128)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text(
            text = hexContent ?: "ELF/Binary data",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CompactFolderPreview(item: FileSystemItem) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${item.childCount ?: 0} items inside • Tap to enter folder",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompactVideoPreview(file: File, uri: Uri, onFullScreen: () -> Unit) {
    val context = LocalContext.current
    var durationStr by remember(file.absolutePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            try {
                val meta = FileUtil.extractAudioMetadata(context, uri)
                if (meta.durationMs > 0) {
                    durationStr = FileUtil.formatDuration(meta.durationMs)
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF0284C7)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video Play",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Video Media (${file.extension.uppercase(Locale.ROOT)})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (durationStr != null) "Length: $durationStr • Tap to Play" else "Tap preview to play inside app or open in external player",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompactOfficePreview(file: File, keywords: List<String>, onFullScreen: () -> Unit) {
    val ext = file.extension.lowercase(Locale.ROOT)
    val isSpreadsheet = ext in listOf("xlsx", "xls", "csv", "tsv", "ods")
    var snippetText by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }

    LaunchedEffect(file.absolutePath) {
        isLoading = true
        snippetText = withContext(Dispatchers.IO) {
            try {
                if (isSpreadsheet) {
                    val table = FileUtil.readOfficeXlsxTable(file, maxRows = 6, maxCols = 5)
                    if (table.isNotEmpty()) {
                        table.joinToString("\n") { row -> row.filter { it.isNotBlank() }.joinToString(" | ") }
                    } else {
                        "Spreadsheet Document Ready"
                    }
                } else {
                    val paragraphs = FileUtil.readOfficeDocxParagraphs(file)
                    if (paragraphs.isNotEmpty()) {
                        paragraphs.take(8).joinToString("\n")
                    } else {
                        "Word Document Ready"
                    }
                }
            } catch (e: Exception) {
                "Document preview ready"
            }
        }
        isLoading = false
    }

    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSpreadsheet) Icons.Default.TableChart else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (isSpreadsheet) Color(0xFF16A34A) else Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSpreadsheet) "Spreadsheet (${ext.uppercase(Locale.ROOT)})" else "Document (${ext.uppercase(Locale.ROOT)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpreadsheet) Color(0xFF16A34A) else Color(0xFF2563EB)
                    )
                }
                Text(
                    text = "Tap to Open",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildHighlightedText(snippetText ?: "Document ready", keywords),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CompactGenericPreview(item: FileSystemItem, onFullScreen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${item.formattedSize} • Tap for In-App Reader",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
