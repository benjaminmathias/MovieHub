package com.benjamin.moviehub.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.benjamin.moviehub.MainActivity
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsMessageRecoveryTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun cacheResultSurvivesRecreationUntilDisplayedAndAcknowledged() {
        val clearCache = TestStrings.get(R.string.clear_image_cache)
        val message = TestStrings.get(R.string.image_cache_cleared)
        composeRule
            .onNodeWithContentDescription(TestStrings.get(R.string.settings_title))
            .performClick()
        composeRule.onNodeWithText(clearCache).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }

        // Interrupt the display: the retained ViewModel must keep the result pending.
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText(clearCache).assertIsDisplayed()
        composeRule.onNodeWithText(message).assertIsDisplayed()

        // Once the snackbar finishes, another recreation must not display it again.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(message).fetchSemanticsNodes().isEmpty()
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText(clearCache).assertIsDisplayed()
        composeRule.onNodeWithText(message).assertDoesNotExist()
    }
}
