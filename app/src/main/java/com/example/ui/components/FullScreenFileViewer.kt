package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FileSystemItem
import com.example.util.AudioPlayerState
import com.example.util.FileCategory
import com.example.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Builds an AnnotatedString that highlights all occurrences of keywords with a bright yellow background.
 */
fun buildHighlightedText(
    text: String,
    keywords: List<String>,
    highlightBgColor: Color = Color(0xFFFFEB3B), // Bright Yellow
    highlightTextColor: Color = Color(0xFF000000) // High-contrast black text on yellow
): AnnotatedString {
    val cleanKeywords = keywords.map { it.trim() }.filter { it.isNotEmpty() }
    if (cleanKeywords.isEmpty()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        val lowerText = text.lowercase(Locale.ROOT)
        // Find all match intervals
        val matchIntervals = mutableListOf<IntRange>()
        for (kw in cleanKeywords) {
            val lowerKw = kw.lowercase(Locale.ROOT)
            var startIdx = 0
            while (startIdx < lowerText.length) {
                val found = lowerText.indexOf(lowerKw, startIdx)
                if (found == -1) break
                matchIntervals.add(found until (found + lowerKw.length))
                startIdx = found + lowerKw.length
            }
        }

        if (matchIntervals.isEmpty()) {
            append(text)
            return@buildAnnotatedString
        }

        // Merge overlapping intervals
        val merged = mutableListOf<IntRange>()
        val sorted = matchIntervals.sortedBy { it.first }
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= current.last + 1) {
                current = current.first..maxOf(current.last, next.last)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)

        var lastPos = 0
        for (interval in merged) {
            if (interval.first > lastPos) {
                append(text.substring(lastPos, interval.first))
            }
            withStyle(
                style = SpanStyle(
                    background = highlightBgColor,
                    color = highlightTextColor,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(interval.first, interval.last + 1))
            }
            lastPos = interval.last + 1
        }
        if (lastPos < text.length) {
            append(text.substring(lastPos))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenFileViewerDialog(
    item: FileSystemItem,
    initialKeywords: List<String> = emptyList(),
    audioState: AudioPlayerState,
    onTogglePlayAudio: (Uri, Long, String) -> Unit,
    onSeekAudio: (Long) -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val category = item.category
    val categoryColor = getCategoryColor(category)
    val uri = remember(item.path) { Uri.fromFile(item.file) }

    var searchFilterText by remember { mutableStateOf(initialKeywords.firstOrNull() ?: "") }
    val effectiveKeywords = remember(searchFilterText, initialKeywords) {
        val list = mutableListOf<String>()
        if (searchFilterText.isNotBlank()) {
            list.addAll(searchFilterText.split(" ", ",").map { it.trim() }.filter { it.isNotEmpty() })
        } else {
            list.addAll(initialKeywords)
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().testTag("fullscreen_file_viewer"),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.formattedSize} • ${category.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onShare) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onOpenExternal) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in External App",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // In-Viewer Live Keyword Search Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchFilterText,
                            onValueChange = { searchFilterText = it },
                            placeholder = { Text("Highlight keywords in yellow...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchFilterText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchFilterText = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        if (effectiveKeywords.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFEB3B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Highlighting ON",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Main Viewer Content for ALL Formats
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    val ext = item.file.extension.lowercase(Locale.ROOT)
                    when {
                        category == FileCategory.IMAGE -> {
                            FullScreenImageViewer(uri = uri)
                        }
                        category == FileCategory.VIDEO -> {
                            FullScreenVideoViewer(file = item.file, uri = uri, onOpenExternal = onOpenExternal)
                        }
                        category == FileCategory.PDF -> {
                            FullScreenPdfViewer(file = item.file, uri = uri, keywords = effectiveKeywords)
                        }
                        category == FileCategory.AUDIO -> {
                            FullScreenAudioViewer(
                                item = item,
                                uri = uri,
                                audioState = audioState,
                                onTogglePlay = { onTogglePlayAudio(uri, item.dbFileId ?: item.path.hashCode().toLong(), item.name) },
                                onSeek = onSeekAudio
                            )
                        }
                        ext in listOf("xlsx", "xls", "csv", "tsv", "ods") -> {
                            FullScreenSpreadsheetViewer(file = item.file, keywords = effectiveKeywords, onOpenExternal = onOpenExternal)
                        }
                        ext in listOf("docx", "doc", "odt", "rtf", "pptx") -> {
                            FullScreenDocumentViewer(file = item.file, keywords = effectiveKeywords, onOpenExternal = onOpenExternal)
                        }
                        category == FileCategory.TEXT || category == FileCategory.CODE -> {
                            FullScreenTextViewer(file = item.file, keywords = effectiveKeywords)
                        }
                        category == FileCategory.ARCHIVE -> {
                            FullScreenArchiveViewer(file = item.file, keywords = effectiveKeywords)
                        }
                        category == FileCategory.SYSTEM_BINARY || category == FileCategory.OTHER -> {
                            FullScreenHexViewer(file = item.file, keywords = effectiveKeywords)
                        }
                        else -> {
                            FullScreenTextViewer(file = item.file, keywords = effectiveKeywords)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenTextViewer(
    file: File,
    keywords: List<String>
) {
    val context = LocalContext.current
    var lines by remember(file.absolutePath) { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }
    var totalMatchesCount by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(file.absolutePath) {
        isLoading = true
        lines = withContext(Dispatchers.IO) {
            try {
                // If it's a docx / doc document, strip and extract text lines
                val ext = file.extension.lowercase(Locale.ROOT)
                if (ext in listOf("docx", "xlsx", "pptx", "odt", "ods")) {
                    val raw = FileUtil.readOfficeDocxText(file) ?: ""
                    raw.lines()
                } else if (ext == "vcf") {
                    val vcfText = file.readText()
                    vcfText.lines()
                } else {
                    file.useLines { it.take(3000).toList() }
                }
            } catch (e: Exception) {
                listOf("Could not read file: ${e.message}")
            }
        }
        isLoading = false
    }

    // Compute matching lines and scroll to first match
    val matchingIndices = remember(lines, keywords) {
        if (keywords.isEmpty()) emptyList()
        else {
            val list = mutableListOf<Int>()
            lines.forEachIndexed { idx, line ->
                val lower = line.lowercase(Locale.ROOT)
                if (keywords.any { lower.contains(it.lowercase(Locale.ROOT)) }) {
                    list.add(idx)
                }
            }
            list
        }
    }

    var currentMatchPos by remember(matchingIndices) { mutableIntStateOf(0) }

    LaunchedEffect(matchingIndices) {
        totalMatchesCount = matchingIndices.size
        if (matchingIndices.isNotEmpty()) {
            currentMatchPos = 0
            listState.animateScrollToItem(matchingIndices[0])
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Match Navigation Ribbon if keywords match
            if (matchingIndices.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFEF08A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${currentMatchPos + 1} of ${matchingIndices.size} matches found",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF854D0E)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (currentMatchPos > 0) {
                                        currentMatchPos--
                                        scope.launch { listState.animateScrollToItem(matchingIndices[currentMatchPos]) }
                                    }
                                },
                                enabled = currentMatchPos > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match")
                            }

                            IconButton(
                                onClick = {
                                    if (currentMatchPos < matchingIndices.size - 1) {
                                        currentMatchPos++
                                        scope.launch { listState.animateScrollToItem(matchingIndices[currentMatchPos]) }
                                    }
                                },
                                enabled = currentMatchPos < matchingIndices.size - 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match")
                            }
                        }
                    }
                }
            }

            // Text / Code Lines List
            SelectionContainer(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(lines.size) { index ->
                        val line = lines[index]
                        val isMatchedLine = matchingIndices.contains(index)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isMatchedLine) Color(0xFFFEF9C3).copy(alpha = 0.5f) else Color.Transparent)
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "${index + 1}".padStart(4, ' '),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                modifier = Modifier.width(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildHighlightedText(line, keywords),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenPdfViewer(
    file: File,
    uri: Uri,
    keywords: List<String>
) {
    val context = LocalContext.current
    var firstPageBitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var extractedText by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }

    LaunchedEffect(file.absolutePath) {
        isLoading = true
        withContext(Dispatchers.IO) {
            firstPageBitmap = FileUtil.renderPdfFirstPage(context, uri)
            extractedText = FileUtil.extractPdfText(file, maxPages = 15)
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (firstPageBitmap != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Image(
                        bitmap = firstPageBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page 1",
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            if (!extractedText.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracted PDF Text & Search Results",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("PDF text", extractedText ?: "")
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied PDF text", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        SelectionContainer {
                            Text(
                                text = buildHighlightedText(extractedText ?: "", keywords),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenImageViewer(uri: Uri) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = "Full Image View",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun FullScreenAudioViewer(
    item: FileSystemItem,
    uri: Uri,
    audioState: AudioPlayerState,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val isPlaying = audioState.isPlaying && audioState.currentFileId == (item.dbFileId ?: item.path.hashCode().toLong())
    val currentMs = if (audioState.currentFileId == (item.dbFileId ?: item.path.hashCode().toLong())) audioState.currentPositionMs else 0L
    val durationMs = if (audioState.currentFileId == (item.dbFileId ?: item.path.hashCode().toLong()) && audioState.durationMs > 0) audioState.durationMs else 180000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AudioFile,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = item.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${item.formattedSize} • ${item.permissions}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        Slider(
            value = currentMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = FileUtil.formatDuration(currentMs), style = MaterialTheme.typography.labelMedium)
            Text(text = FileUtil.formatDuration(durationMs), style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.height(20.dp))

        IconButton(
            onClick = onTogglePlay,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun FullScreenArchiveViewer(file: File, keywords: List<String>) {
    var entries by remember(file.absolutePath) { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }

    LaunchedEffect(file.absolutePath) {
        isLoading = true
        entries = withContext(Dispatchers.IO) {
            try {
                ZipFile(file).use { zip ->
                    zip.entries().asSequence().map { it.name }.toList()
                }
            } catch (e: Exception) {
                listOf("Archive error: ${e.message}")
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "Archive Contents (${entries.size} items)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            items(entries) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildHighlightedText(entry, keywords),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenHexViewer(file: File, keywords: List<String>) {
    var hexDump by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }

    LaunchedEffect(file.absolutePath) {
        isLoading = true
        hexDump = withContext(Dispatchers.IO) {
            FileUtil.generateHexDump(file, maxBytes = 2048)
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = "Hex / Binary Inspector",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildHighlightedText(hexDump ?: "No data", keywords),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun FullScreenVideoViewer(
    file: File,
    uri: Uri,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isError) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = null,
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Cannot play video codec internally",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This video container/codec (${file.extension.uppercase(Locale.ROOT)}) requires a dedicated external player (e.g. VLC or Google Photos).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = onOpenExternal,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open in External Video Player")
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val mc = MediaController(ctx)
                        mc.setAnchorView(this)
                        setMediaController(mc)
                        setOnErrorListener { _, _, _ ->
                            isError = true
                            true
                        }
                        if (uri.scheme == "file") {
                            setVideoPath(file.absolutePath)
                        } else {
                            setVideoURI(uri)
                        }
                        start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun FullScreenSpreadsheetViewer(
    file: File,
    keywords: List<String>,
    onOpenExternal: () -> Unit
) {
    var tableData by remember(file.absolutePath) { mutableStateOf<List<List<String>>>(emptyList()) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }
    var errorMessage by remember(file.absolutePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(file.absolutePath) {
        isLoading = true
        errorMessage = null
        try {
            tableData = withContext(Dispatchers.IO) {
                if (file.extension.equals("csv", ignoreCase = true) || file.extension.equals("tsv", ignoreCase = true)) {
                    FileUtil.readCsvTable(file, maxRows = 200)
                } else {
                    FileUtil.readOfficeXlsxTable(file, maxRows = 200, maxCols = 30)
                }
            }
            if (tableData.isEmpty()) {
                errorMessage = "No spreadsheet cells found or unsupported format."
            }
        } catch (e: Exception) {
            errorMessage = e.message
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (tableData.isEmpty() || errorMessage != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.TableChart,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Spreadsheet (${file.extension.uppercase(Locale.ROOT)})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage ?: "Could not parse worksheet xml internally.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(onClick = onOpenExternal) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open in External Sheets / Excel App")
            }
        }
    } else {
        val colCount = remember(tableData) { tableData.maxOfOrNull { it.size } ?: 1 }
        val horizontalScrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize()) {
            // Header stats & fallback banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 ${tableData.size} rows • $colCount columns",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                    Text(
                        text = "External Open ↗",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenExternal() }
                    )
                }
            }

            // Interactive 2D Spreadsheet Grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Column Letters Header Row (A, B, C, D...)
                    item {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 4.dp)
                        ) {
                            // Empty corner box for row index
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            }
                            for (c in 0 until colCount) {
                                val colName = if (c < 26) ('A' + c).toString() else "C${c + 1}"
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = colName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Data Rows
                    items(tableData.size) { rIdx ->
                        val rowCells = tableData[rIdx]
                        val isEven = rIdx % 2 == 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isEven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Row number
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rIdx + 1}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // Row cells
                            for (c in 0 until colCount) {
                                val cellVal = rowCells.getOrNull(c) ?: ""
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = buildHighlightedText(cellVal, keywords),
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
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

@Composable
fun FullScreenDocumentViewer(
    file: File,
    keywords: List<String>,
    onOpenExternal: () -> Unit
) {
    var paragraphs by remember(file.absolutePath) { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember(file.absolutePath) { mutableStateOf(true) }

    LaunchedEffect(file.absolutePath) {
        isLoading = true
        paragraphs = withContext(Dispatchers.IO) {
            try {
                FileUtil.readOfficeDocxParagraphs(file)
            } catch (e: Exception) {
                listOf("Could not parse document: ${e.message}")
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (paragraphs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Word Document (${file.extension.uppercase(Locale.ROOT)})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("This document can be opened in Microsoft Word / Google Docs / Office Reader.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(onClick = onOpenExternal) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open in External Word / Office App")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📄 ${paragraphs.size} paragraphs extracted",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Open in Office App ↗",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenExternal() }
                        )
                    }
                }
            }

            items(paragraphs.size) { pIdx ->
                val para = paragraphs[pIdx]
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = buildHighlightedText(para, keywords),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
