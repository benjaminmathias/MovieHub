package com.benjamin.moviehub.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

class NetworkStatusBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unknownToAvailableDoesNotShowNetworkBanner() {
        var status by mutableStateOf(ConnectivityStatus.UNKNOWN)

        composeRule.setContent {
            MovieHubTheme {
                NetworkStatusBar(status)
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
                NetworkStatusBar(status)
            }
        }

        composeRule.onNodeWithText("Pas de connexion internet").assertIsDisplayed()

        composeRule.runOnIdle {
            status = ConnectivityStatus.AVAILABLE
        }

        composeRule.onNodeWithText("Connexion rétablie").assertIsDisplayed()
    }
}
