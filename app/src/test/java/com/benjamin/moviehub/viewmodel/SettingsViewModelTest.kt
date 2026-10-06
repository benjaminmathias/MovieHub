package com.benjamin.moviehub.viewmodel

import app.cash.turbine.test
import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.domain.repository.ImageCacheCleaner
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import com.benjamin.moviehub.ui.settings.SettingsViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences: UserPreferencesRepository = mockk()
    private val imageCacheCleaner: ImageCacheCleaner = mockk()

    @Before
    fun setUp() {
        every { preferences.theme } returns flowOf(AppTheme.SYSTEM)
    }

    @Test
    fun `clearing image cache reports success then failure`() =
        runTest {
            var fail = false
            coEvery { imageCacheCleaner.clear() } coAnswers {
                if (fail) throw IllegalStateException("cache failure")
            }
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)

            viewModel.imageCacheMessages.test {
                viewModel.clearImageCache()
                assertEquals(true, awaitItem())
            }
            assertEquals(false, viewModel.isClearing.value)

            fail = true
            viewModel.imageCacheMessages.test {
                viewModel.clearImageCache()
                assertEquals(false, awaitItem())
            }
            assertEquals(false, viewModel.isClearing.value)
        }

    @Test
    fun `theme changes are persisted`() =
        runTest {
            coEvery { preferences.setTheme(AppTheme.DARK) } just runs
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)

            viewModel.updateTheme(AppTheme.DARK)

            coVerify(exactly = 1) { preferences.setTheme(AppTheme.DARK) }
        }

    @Test
    fun `repeated cache clear requests run a single operation`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            coEvery { imageCacheCleaner.clear() } coAnswers { gate.await() }
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)

            viewModel.clearImageCache()
            viewModel.clearImageCache()
            runCurrent()

            coVerify(exactly = 1) { imageCacheCleaner.clear() }
            assertEquals(true, viewModel.isClearing.value)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(false, viewModel.isClearing.value)
        }
}
