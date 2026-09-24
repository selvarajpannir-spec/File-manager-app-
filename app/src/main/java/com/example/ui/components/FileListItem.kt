package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TagEntity
import com.example.data.model.FileSystemItem
import com.example.util.FileCategory
import com.example.util.FileUtil

@Composable
fun FileListItem(
    item: FileSystemItem,
    isSelectedForPreview: Boolean,
    onClick: () -> Unit,
    onAddTagClick: () -> Unit,
    onOpenWithSystem: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onShowDetails: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = item.category
    val categoryColor = getCategoryColor(category)
    val categoryIcon = getCategoryIcon(category)

    var showMenu by remember { mutableStateOf(false) }

    val formattedDate = FileUtil.formatShortDate(item.lastModified)
    val fileSubtitle = if (item.isDirectory) {
        val count = item.childCount
        if (count != null) "$count items • $formattedDate" else "Folder • $formattedDate"
    } else {
        "${item.formattedSize} • $formattedDate"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 12.dp)
            .clickable { onClick() }
            .testTag("file_item_${item.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectedForPreview) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelectedForPreview) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // MAIN ROW: Icon + File Name & Metadata + Quick Tag & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category / Type Icon Box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = category.name,
                        tint = categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name, Permissions, Subtitle
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (item.isSystemFile) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFDC2626).copy(alpha = 0.12f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "SYS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = fileSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Text(
                            text = item.permissions,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Actions: Quick Tag & Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onAddTagClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("add_tag_btn_${item.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Tag",
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Instant Preview") },
                                leadingIcon = { Icon(Icons.Default.Preview, contentDescription = null, Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Manage Tags") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    onAddTagClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Details & Perms") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    onShowDetails()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    onRename()
                                }
                            )
                            if (!item.isDirectory) {
                                DropdownMenuItem(
                                    text = { Text("Open in External App") },
                                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onOpenWithSystem()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onShare()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // TAGS ROW: Clean pill-free dot + text tags
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 50.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.tags.forEach { tag ->
                        TagChipItem(tag = tag, onClick = onAddTagClick)
                    }
                }
            }
        }
    }
}

@Composable
fun TagChipItem(
    tag: TagEntity,
    onClick: (() -> Unit)? = null
) {
    val tagColor = parseColor(tag.colorHex)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tagColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "#${tag.tagName}",
            style = MaterialTheme.typography.labelSmall,
            color = tagColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

fun getCategoryIcon(category: FileCategory): ImageVector {
    return when (category) {
        FileCategory.FOLDER -> Icons.Default.Folder
        FileCategory.PDF -> Icons.Default.PictureAsPdf
        FileCategory.IMAGE -> Icons.Default.Image
        FileCategory.VIDEO -> Icons.Default.VideoFile
        FileCategory.AUDIO -> Icons.Default.AudioFile
        FileCategory.CODE -> Icons.Default.Code
        FileCategory.TEXT -> Icons.Default.Description
        FileCategory.DOCUMENT -> Icons.Default.Description
        FileCategory.ARCHIVE -> Icons.Default.FolderZip
        FileCategory.SYSTEM_BINARY -> Icons.Default.Memory
        FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

fun getCategoryColor(category: FileCategory): Color {
    return when (category) {
        FileCategory.FOLDER -> Color(0xFFEAB308)
        FileCategory.PDF -> Color(0xFFE11D48)
        FileCategory.IMAGE -> Color(0xFF8B5CF6)
        FileCategory.VIDEO -> Color(0xFF10B981)
        FileCategory.AUDIO -> Color(0xFFF59E0B)
        FileCategory.CODE -> Color(0xFF2563EB)
        FileCategory.TEXT -> Color(0xFF0284C7)
        FileCategory.DOCUMENT -> Color(0xFF4F46E5)
        FileCategory.ARCHIVE -> Color(0xFF64748B)
        FileCategory.SYSTEM_BINARY -> Color(0xFFDC2626)
        FileCategory.OTHER -> Color(0xFF6B7280)
    }
}

fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16).toInt()
        if (clean.length == 6) {
            Color(colorInt or 0xFF000000.toInt())
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        Color(0xFF6366F1)
    }
}
