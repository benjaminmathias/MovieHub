package com.benjamin.moviehub.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
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

        awaitBanner(R.string.no_internet_connection)

        composeRule.runOnIdle { status = ConnectivityStatus.AVAILABLE }

        awaitBanner(R.string.connection_restored)
    }

    @Test
    fun offlineToUnknownThenAvailableRestoresOnlyOnAvailable() {
        var status by mutableStateOf(ConnectivityStatus.LOST)
        setNetworkContent { status }

        awaitBanner(R.string.no_internet_connection)

        // A degraded UNKNOWN must not announce a restoration...
        composeRule.runOnIdle { status = ConnectivityStatus.UNKNOWN }
        assertBannerAbsent(R.string.connection_restored)

        // ... but the outage is still remembered until a usable connection is back.
        composeRule.runOnIdle { status = ConnectivityStatus.AVAILABLE }
        awaitBanner(R.string.connection_restored)
    }

    @Test
    fun aSecondOfflineStatusDoesNotReannounceTheBanner() {
        var status by mutableStateOf(ConnectivityStatus.LOST)
        setNetworkContent { status }

        composeRule.onNodeWithText(TestStrings.get(R.string.no_internet_connection)).assertIsDisplayed()

        // LOST <-> UNAVAILABLE are both offline; moving between them keeps the same banner.
        composeRule.runOnIdle { status = ConnectivityStatus.UNAVAILABLE }

        composeRule.onNodeWithText(TestStrings.get(R.string.no_internet_connection)).assertIsDisplayed()
    }

    @Test
    fun offlineTransitionDoesNotDismissAnInFlightMessage() {
        var status by mutableStateOf(ConnectivityStatus.AVAILABLE)
        composeRule.setContent {
            MovieHubTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar("favorite-error", duration = SnackbarDuration.Indefinite)
                }
                NetworkStatusEffect(status, snackbarHostState)
                SnackbarHost(snackbarHostState)
            }
        }

        composeRule.onNodeWithText("favorite-error").assertIsDisplayed()

        composeRule.runOnIdle { status = ConnectivityStatus.LOST }

        // The offline banner queues behind the in-flight message instead of dismissing it.
        composeRule.onNodeWithText("favorite-error").assertIsDisplayed()
    }

    @Test
    fun unknownToAvailableDoesNotShowNetworkBanner() {
        var status by mutableStateOf(ConnectivityStatus.UNKNOWN)
        setNetworkContent { status }

        assertNoBanner()

        composeRule.runOnIdle { status = ConnectivityStatus.AVAILABLE }

        assertNoBanner()
    }

    private fun awaitBanner(resId: Int) {
        val message = TestStrings.get(resId)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertBannerAbsent(resId: Int) {
        assertTrue(
            composeRule
                .onAllNodesWithText(TestStrings.get(resId))
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    private fun assertNoBanner() {
        assertBannerAbsent(R.string.no_internet_connection)
        assertBannerAbsent(R.string.connection_restored)
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
