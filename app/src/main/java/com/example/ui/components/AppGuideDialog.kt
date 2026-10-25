package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@Composable
fun AppGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("⚡ Overview", "👁️ Preview", "☑️ Multi-Select", "🔍 Search", "💾 Memory")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .testTag("app_guide_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "How Files+ Works",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Interactive Guide & Feature Tour",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("close_app_guide_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close guide",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive Tab Selector
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> OverviewGuideTab()
                        1 -> InstantPreviewGuideTab()
                        2 -> MultiSelectGuideTab()
                        3 -> DeepSearchGuideTab()
                        4 -> MemoryAndSafeDeleteGuideTab()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Launch HTML Demo Page button
                Button(
                    onClick = {
                        openHtmlDemoPage(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("launch_html_guide_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🌐 Open Full HTML Demo Page",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("confirm_dismiss_guide_btn")
            ) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun OverviewGuideTab() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GuideFeatureCard(
            icon = Icons.Default.Info,
            title = "Zero-Friction Layout",
            description = "The screen is arranged for maximum speed:\n• 80.dp top promotional banner\n• Unified navigation bar with breadcrumbs & 3-dot drawer\n• Live Top Preview card\n• Multi-select batch action toolbar\n• High-speed file list with checkboxes"
        )
        GuideFeatureCard(
            icon = Icons.Default.CheckCircle,
            title = "100% Offline & Private",
            description = "All search indexing, PDF text extraction, and file organization run strictly on your device's CPU. No cloud uploads or telemetry."
        )
    }
}

@Composable
private fun InstantPreviewGuideTab() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GuideFeatureCard(
            icon = Icons.Default.Preview,
            title = "Top Instant Preview Card",
            description = "Tapping or checking any file instantly updates the top card without opening slow third-party apps:\n• 📄 PDFs: Native page-1 vector rendering\n• 🎵 Audio: In-line scrubbable mini player\n• 🖼️ Photos: Instant HD view & resolution\n• 💻 Code & Text: Monospace syntax viewer\n• 📊 Spreadsheets: Formatted CSV/Excel table\n• 🔬 Binaries: 16-byte Hex Dump inspector"
        )
    }
}

@Composable
private fun MultiSelectGuideTab() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GuideFeatureCard(
            icon = Icons.Default.Share,
            title = "Checkboxes & Batch Multi-Share",
            description = "• Checkboxes on every file allow selecting one or many files.\n• 'Select All' checkbox in header selects everything in the active folder.\n• Checking a box keeps instant preview active for that file.\n• Tap '📤 Share' to send all selected files simultaneously to WhatsApp, Gmail, Drive, etc.\n• Tap '🗑️ Delete' to safely purge all selected files with confirmation."
        )
    }
}

@Composable
private fun DeepSearchGuideTab() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GuideFeatureCard(
            icon = Icons.Default.Search,
            title = "Inside-Document Full Text Search",
            description = "• Searches inside PDFs, Word (.docx), Excel (.xlsx), CSV, TXT, and contacts (.vcf).\n• Phone Number Matcher: Searches like '9876543210' automatically match '+91 98765-43210' or '(987) 654-3210'.\n• Runs in the background with a synchronized system notification."
        )
    }
}

@Composable
private fun MemoryAndSafeDeleteGuideTab() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GuideFeatureCard(
            icon = Icons.Default.Restore,
            title = "State Memory & Safe Deletion",
            description = "• Remembers Folder & File: Relaunching the app restores your exact last folder, preview file, and active tab.\n• Safe Deletion (No Root Reset): Deleting files keeps you on the same folder page.\n• Up navigation safely stops at user storage boundaries instead of Linux root ('/')."
        )
    }
}

@Composable
private fun GuideFeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

/**
 * Extracts app_guide.html from assets to app cache directory and opens it in the browser or viewer.
 */
fun openHtmlDemoPage(context: Context) {
    try {
        val targetFile = File(context.cacheDir, "app_guide.html")
        context.assets.open("app_guide.html").use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(Intent.createChooser(intent, "Open Demonstration Page"))
    } catch (e: Exception) {
        Toast.makeText(context, "Opening guide: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
