package com.benjamin.moviehub.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.benjamin.moviehub.core.util.AppTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class UserPreferencesRepositoryTest {
    private val dataStore: DataStore<Preferences> = mockk()

    @Test
    fun `missing or unknown theme uses the system setting`() =
        runTest {
            val key = stringPreferencesKey("app_theme")
            for (preferences in listOf(emptyPreferences(), preferencesOf(key to "obsolete"))) {
                every { dataStore.data } returns flowOf(preferences)
                assertEquals(AppTheme.SYSTEM, UserPreferencesRepositoryImpl(dataStore).theme.first())
            }
        }

    @Test
    fun `stored theme names are decoded`() =
        runTest {
            val key = stringPreferencesKey("app_theme")
            for ((name, expected) in listOf("LIGHT" to AppTheme.LIGHT, "DARK" to AppTheme.DARK, "SYSTEM" to AppTheme.SYSTEM)) {
                every { dataStore.data } returns flowOf(preferencesOf(key to name))
                assertEquals(expected, UserPreferencesRepositoryImpl(dataStore).theme.first())
            }
        }

    @Test
    fun `an IO read failure falls back to system theme`() =
        runTest {
            every { dataStore.data } returns flow { throw IOException("unavailable") }
            assertEquals(AppTheme.SYSTEM, UserPreferencesRepositoryImpl(dataStore).theme.first())
        }

    @Test
    fun `non IO failures and cancellation propagate to the collector`() =
        runTest {
            for (failure in listOf(IllegalStateException("broken"), CancellationException("cancelled"))) {
                every { dataStore.data } returns flow { throw failure }
                try {
                    UserPreferencesRepositoryImpl(dataStore).theme.first()
                    fail("Expected the original failure")
                } catch (actual: Exception) {
                    assertSame(failure, actual)
                }
            }
        }
}
