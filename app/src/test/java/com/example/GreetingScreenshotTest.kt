package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.entity.FileEntity
import com.example.data.local.entity.FileWithTags
import com.example.data.local.entity.TagEntity
import com.example.ui.components.FileListItem
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun greeting_screenshot() {
        val sampleFileWithTags = FileWithTags(
            file = FileEntity(
                id = 1L,
                fileUri = "content://media/external/files/100",
                fileName = "project_proposal.pdf",
                fileSize = 2516582L,
                fileType = "application/pdf",
                lastModified = 1760000000000L,
                contentText = "Scoped Storage & Room Architecture"
            ),
            tags = listOf(
                TagEntity(tagId = 1, tagName = "work", colorHex = "#6366F1"),
                TagEntity(tagId = 2, tagName = "2026", colorHex = "#0EA5E9"),
                TagEntity(tagId = 3, tagName = "draft", colorHex = "#EC4899")
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                FileListItem(
                    fileWithTags = sampleFileWithTags,
                    isSelectedForPreview = false,
                    onClick = {},
                    onAddTagClick = {},
                    onOpenWithSystem = {},
                    onShare = {},
                    onShowDetails = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
