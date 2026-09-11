package com.benjamin.moviehub.ui.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.core.util.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsActionsInvokeCallbacks() {
        var selectedTheme: AppTheme? = null
        var cacheCleared = false

        composeRule.setContent {
            MovieHubTheme {
                SettingsScreen(
                    currentTheme = AppTheme.SYSTEM,
                    isClearing = false,
                    snackbarHostState = SnackbarHostState(),
                    onThemeSelected = { selectedTheme = it },
                    onClearImageCache = { cacheCleared = true },
                    onBackClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Sombre").performClick()
        composeRule.onNodeWithText("Vider le cache des images").performClick()

        composeRule.runOnIdle {
            assertEquals(AppTheme.DARK, selectedTheme)
            assertTrue(cacheCleared)
        }
    }
}
