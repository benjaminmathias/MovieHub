package com.benjamin.moviehub

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MovieSearchFavoriteTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun searchInterstellar_selectMovie_andAddToFavorites() {
        composeTestRule
            .onNode(hasSetTextAction())
            .performTextInput("Interstellar")

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule
                .onAllNodesWithText("Interstellar")
                .fetchSemanticsNodes()
                .size >= 2
        }

        composeTestRule
            .onAllNodesWithText("Interstellar")
            .onLast()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Ajouter aux favoris")
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeTestRule
                    .onAllNodesWithContentDescription("Retirer des favoris")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }

        composeTestRule.onNodeWithTag("detail_screen").assertIsDisplayed()

        // Make the test idempotent when the emulator already contains Interstellar.
        if (
            composeTestRule
                .onAllNodesWithContentDescription("Retirer des favoris")
                .fetchSemanticsNodes()
                .isNotEmpty()
        ) {
            composeTestRule
                .onNodeWithContentDescription("Retirer des favoris")
                .performClick()
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule
                    .onAllNodesWithContentDescription("Ajouter aux favoris")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Ajouter aux favoris")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Retirer des favoris")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
