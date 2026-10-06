package com.benjamin.moviehub.ui.navigation

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import com.benjamin.moviehub.core.theme.MovieHubTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeFavoriteErrorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun leavingHomeBeforeTheMessageFinishesKeepsTheErrorPending() {
        var pending by mutableStateOf(true)
        var showHome by mutableStateOf(true)
        var acknowledged = false
        val message = TestStrings.get(R.string.error_updating_favorite)

        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                if (showHome) {
                    FavoriteErrorEffect(
                        pending = pending,
                        snackbarHostState = snackbarHostState,
                        message = message,
                        onAcknowledged = {
                            acknowledged = true
                            pending = false
                        },
                    )
                }
                SnackbarHost(snackbarHostState)
            }
        }

        composeRule.onNodeWithText(message).assertIsDisplayed()

        // Leaving HOME mid-display cancels the effect, so the pending error is not acknowledged.
        composeRule.runOnIdle { showHome = false }
        composeRule.waitForIdle()
        assertFalse(acknowledged)
        assertTrue(pending)

        // Returning to HOME shows the still-pending generic error again.
        composeRule.runOnIdle { showHome = true }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun aPendingErrorIsAcknowledgedOnlyAfterTheMessageIsShown() {
        var pending by mutableStateOf(true)
        val message = TestStrings.get(R.string.error_updating_favorite)

        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                FavoriteErrorEffect(
                    pending = pending,
                    snackbarHostState = snackbarHostState,
                    message = message,
                    onAcknowledged = { pending = false },
                )
                SnackbarHost(snackbarHostState)
            }
        }

        composeRule.onNodeWithText(message).assertIsDisplayed()

        // The acknowledgement only happens once the snackbar (Short) finishes displaying.
        composeRule.waitUntil(timeoutMillis = 10_000) { !pending }
    }
}
