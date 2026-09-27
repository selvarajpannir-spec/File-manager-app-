package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.entity.TagEntity
import com.example.data.model.FileSystemItem
import com.example.ui.components.FileListItem
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FileCategory
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun greeting_screenshot() {
        val sampleItem = FileSystemItem(
            file = File("/storage/emulated/0/Documents/project_proposal.pdf"),
            name = "project_proposal.pdf",
            path = "/storage/emulated/0/Documents/project_proposal.pdf",
            isDirectory = false,
            size = 2516582L,
            formattedSize = "2.4 MB",
            lastModified = 1760000000000L,
            category = FileCategory.PDF,
            tags = listOf(
                TagEntity(tagId = 1, tagName = "work", colorHex = "#6366F1"),
                TagEntity(tagId = 2, tagName = "2026", colorHex = "#0EA5E9"),
                TagEntity(tagId = 3, tagName = "draft", colorHex = "#EC4899")
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                FileListItem(
                    item = sampleItem,
                    isSelectedForPreview = false,
                    onClick = {},
                    onAddTagClick = {},
                    onOpenWithSystem = {},
                    onShare = {},
                    onRename = {},
                    onShowDetails = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
