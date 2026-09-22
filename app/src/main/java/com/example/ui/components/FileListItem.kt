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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FileWithTags
import com.example.data.local.entity.TagEntity
import com.example.util.FileCategory
import com.example.util.FileUtil

@Composable
fun FileListItem(
    fileWithTags: FileWithTags,
    isSelectedForPreview: Boolean,
    onClick: () -> Unit,
    onAddTagClick: () -> Unit,
    onOpenWithSystem: () -> Unit,
    onShare: () -> Unit,
    onShowDetails: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val file = fileWithTags.file
    val tags = fileWithTags.tags
    val category = FileUtil.getMimeTypeCategory(file.fileName, file.fileType)
    val categoryColor = getCategoryColor(category)
    val categoryIcon = getCategoryIcon(category)

    var showMenu by remember { mutableStateOf(false) }

    val formattedSize = FileUtil.formatFileSize(file.fileSize)
    val formattedDate = FileUtil.formatShortDate(file.lastModified)
    val fileDetails = "$formattedSize • $formattedDate"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 12.dp)
            .clickable { onClick() }
            .testTag("file_item_${file.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectedForPreview) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelectedForPreview) 3.dp else 0.5.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // TOP ROW: Icon + File Name & Metadata + Menu Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = category.name,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name and details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = file.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = fileDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // Quick Tag & Options Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onAddTagClick,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("add_tag_button_${file.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add tag",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "File Options",
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
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Manage Tags") },
                                onClick = {
                                    showMenu = false
                                    onAddTagClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("File Details") },
                                onClick = {
                                    showMenu = false
                                    onShowDetails()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open with App") },
                                onClick = {
                                    showMenu = false
                                    onOpenWithSystem()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share File") },
                                onClick = {
                                    showMenu = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove from List", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // DEDICATED FULL-WIDTH TAGS ROW: Clean text display with no background/border, spacious single line
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 46.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tags.forEach { tag ->
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
            text = tag.tagName,
            style = MaterialTheme.typography.labelSmall,
            color = tagColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

fun getCategoryIcon(category: FileCategory): ImageVector {
    return when (category) {
        FileCategory.PDF -> Icons.Default.PictureAsPdf
        FileCategory.IMAGE -> Icons.Default.Image
        FileCategory.VIDEO -> Icons.Default.VideoFile
        FileCategory.AUDIO -> Icons.Default.AudioFile
        FileCategory.TEXT -> Icons.Default.Description
        FileCategory.DOCUMENT -> Icons.Default.Description
        FileCategory.ARCHIVE -> Icons.Default.FolderZip
        FileCategory.OTHER -> Icons.Default.InsertDriveFile
    }
}

fun getCategoryColor(category: FileCategory): Color {
    return when (category) {
        FileCategory.PDF -> Color(0xFFE11D48)
        FileCategory.IMAGE -> Color(0xFF8B5CF6)
        FileCategory.VIDEO -> Color(0xFF10B981)
        FileCategory.AUDIO -> Color(0xFFF59E0B)
        FileCategory.TEXT -> Color(0xFF0284C7)
        FileCategory.DOCUMENT -> Color(0xFF2563EB)
        FileCategory.ARCHIVE -> Color(0xFF64748B)
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

