package com.benjamin.moviehub.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NetworkStatusBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unknownToAvailableDoesNotShowNetworkBanner() {
        var status by mutableStateOf(ConnectivityStatus.UNKNOWN)

        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NetworkStatusBar(status, snackbarHostState)
                SnackbarHost(snackbarHostState) { NetworkStatusBar(it) }
            }
        }

        assertTrue(composeRule.onAllNodesWithText("Pas de connexion internet").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Connexion rétablie").fetchSemanticsNodes().isEmpty())

        composeRule.runOnIdle {
            status = ConnectivityStatus.AVAILABLE
        }

        assertTrue(composeRule.onAllNodesWithText("Pas de connexion internet").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Connexion rétablie").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun offlineToAvailableShowsRecoveryBanner() {
        var status by mutableStateOf(ConnectivityStatus.LOST)

        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NetworkStatusBar(status, snackbarHostState)
                SnackbarHost(snackbarHostState) { NetworkStatusBar(it) }
            }
        }

        composeRule.onNodeWithText("Pas de connexion internet").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Pas de connexion internet").fetchSemanticsNodes().size == 1)

        composeRule.runOnIdle {
            status = ConnectivityStatus.AVAILABLE
        }

        assertTrue(composeRule.onAllNodesWithText("Connexion rétablie").fetchSemanticsNodes().size == 1)
    }

    @Test
    fun offlineBannerRemainsDisplayedAfterTimeAdvances() {
        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NetworkStatusBar(ConnectivityStatus.LOST, snackbarHostState)
                SnackbarHost(snackbarHostState) { NetworkStatusBar(it) }
            }
        }

        composeRule.mainClock.advanceTimeBy(10_000)
        composeRule.onNodeWithText("Pas de connexion internet").assertIsDisplayed()
    }

    @Test
    fun lostToUnavailableKeepsOneOfflineSnackbar() {
        var status by mutableStateOf(ConnectivityStatus.LOST)
        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NetworkStatusBar(status, snackbarHostState)
                SnackbarHost(snackbarHostState) { NetworkStatusBar(it) }
            }
        }

        composeRule.runOnIdle { status = ConnectivityStatus.UNAVAILABLE }
        assertTrue(composeRule.onAllNodesWithText("Pas de connexion internet").fetchSemanticsNodes().size == 1)
    }
}
