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
import com.benjamin.moviehub.R
import com.benjamin.moviehub.TestStrings
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import com.benjamin.moviehub.domain.connectivity.isOffline
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NetworkSnackbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun offlineToAvailableShowsRecoveryBanner() {
        var status by mutableStateOf(ConnectivityStatus.LOST)
        setNetworkContent { status }

        composeRule.onNodeWithText(TestStrings.get(R.string.no_internet_connection)).assertIsDisplayed()

        composeRule.runOnIdle { status = ConnectivityStatus.AVAILABLE }

        composeRule.onNodeWithText(TestStrings.get(R.string.connection_restored)).assertIsDisplayed()
    }

    @Test
    fun unknownToAvailableDoesNotShowNetworkBanner() {
        var status by mutableStateOf(ConnectivityStatus.UNKNOWN)
        setNetworkContent { status }

        assertNoBanner()

        composeRule.runOnIdle { status = ConnectivityStatus.AVAILABLE }

        assertNoBanner()
    }

    private fun assertNoBanner() {
        assertTrue(
            composeRule
                .onAllNodesWithText(TestStrings.get(R.string.no_internet_connection))
                .fetchSemanticsNodes()
                .isEmpty(),
        )
        assertTrue(
            composeRule
                .onAllNodesWithText(TestStrings.get(R.string.connection_restored))
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    private fun setNetworkContent(status: () -> ConnectivityStatus) {
        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                NetworkStatusEffect(status(), snackbarHostState)
                SnackbarHost(snackbarHostState) {
                    NetworkSnackbar(it, isOffline = status().isOffline)
                }
            }
        }
    }
}
