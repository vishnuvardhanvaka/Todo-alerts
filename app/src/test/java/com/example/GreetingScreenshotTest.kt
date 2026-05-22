package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.TodoItem
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
    val sampleItem = TodoItem(
        id = 1,
        title = "Interactive alarm alert",
        description = "This is a priority task designed to send phone alerts.",
        isCompleted = false,
        isReminderSet = true,
        reminderTime = System.currentTimeMillis() - 5000, // overdue
        category = "Personal",
        priority = 3
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        TodoCard(
            item = sampleItem,
            onToggleCompletion = {},
            onEditClick = {},
            onDeleteClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
