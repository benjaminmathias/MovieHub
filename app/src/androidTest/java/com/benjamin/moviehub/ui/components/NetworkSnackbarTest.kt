package com.benjamin.moviehub.ui.components

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
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NetworkSnackbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun offlineToAvailableShowsRecoveryBanner() {
        var status by mutableStateOf(ConnectivityStatus.LOST)

        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NetworkStatusEffect(status, snackbarHostState)
                SnackbarHost(snackbarHostState) {
                    NetworkSnackbar(
                        it,
                        status == ConnectivityStatus.LOST || status == ConnectivityStatus.UNAVAILABLE,
                    )
                }
            }
        }

        composeRule.onNodeWithText("Pas de connexion internet").assertIsDisplayed()

        composeRule.runOnIdle {
            status = ConnectivityStatus.AVAILABLE
        }

        composeRule.onNodeWithText("Connexion rétablie").assertIsDisplayed()
    }

    @Test
    fun unknownToAvailableDoesNotShowNetworkBanner() {
        var status by mutableStateOf(ConnectivityStatus.UNKNOWN)

        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NetworkStatusEffect(status, snackbarHostState)
                SnackbarHost(snackbarHostState) {
                    NetworkSnackbar(
                        it,
                        status == ConnectivityStatus.LOST || status == ConnectivityStatus.UNAVAILABLE,
                    )
                }
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
    fun offlineBannerRemainsDisplayedAfterTimeAdvances() {
        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NetworkStatusEffect(ConnectivityStatus.LOST, snackbarHostState)
                SnackbarHost(snackbarHostState) {
                    NetworkSnackbar(it, isOffline = true)
                }
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
                NetworkStatusEffect(status, snackbarHostState)
                SnackbarHost(snackbarHostState) {
                    NetworkSnackbar(it, isOffline = true)
                }
            }
        }

        composeRule.runOnIdle {
            status = ConnectivityStatus.UNAVAILABLE
        }

        composeRule.onNodeWithText("Pas de connexion internet").assertIsDisplayed()
    }
}
