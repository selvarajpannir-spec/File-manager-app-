package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileSystemItem
import com.example.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileDetailsDialog(
    item: FileSystemItem?,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit
) {
    if (item == null) return
    val context = LocalContext.current
    var checksum by remember(item.path) { mutableStateOf<String>("Calculating...") }

    LaunchedEffect(item.path) {
        checksum = withContext(Dispatchers.IO) {
            if (!item.isDirectory && item.file.canRead() && item.size < 50_000_000) {
                FileUtil.calculateFileChecksum(item.file, "MD5")
            } else if (item.isDirectory) {
                "Directory (N/A)"
            } else {
                "Large/Inaccessible File"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "File & System Properties",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(label = "File Name", value = item.name)
                DetailRow(label = "Absolute Path", value = item.path, isMonospace = true, canCopy = true, context = context)
                DetailRow(label = "Size", value = if (item.isDirectory) "${item.childCount ?: 0} items" else item.formattedSize)
                DetailRow(label = "Category / Type", value = item.category.name)
                DetailRow(label = "UNIX Permissions", value = item.permissions, isMonospace = true)
                DetailRow(label = "Readable / Writable", value = "Read: ${item.canRead} • Write: ${item.canWrite}")
                DetailRow(label = "Is System File", value = if (item.isSystemFile) "Yes (System Partition / Root)" else "No (User / Storage)")
                DetailRow(label = "Modified Date", value = FileUtil.formatDate(item.lastModified))
                DetailRow(label = "MD5 Checksum", value = checksum, isMonospace = true, canCopy = true, context = context)

                if (item.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Assigned Tags (${item.tags.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item.tags.forEach { tag ->
                            TagChipItem(tag = tag)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!item.isDirectory) {
                    Button(
                        onClick = onOpenExternal,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_open_button")
                    ) {
                        Text("Open with App")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    canCopy: Boolean = false,
    context: Context? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                fontSize = if (isMonospace) 11.sp else 13.sp,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (canCopy && context != null) {
                IconButton(
                    onClick = {
                        val clip = ClipData.newPlainText(label, value)
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy $label",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
