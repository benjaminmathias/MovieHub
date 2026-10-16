package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.domain.repository.ImageCacheCleaner
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import com.benjamin.moviehub.ui.settings.ImageCacheErrorCode
import com.benjamin.moviehub.ui.settings.ImageCacheResult
import com.benjamin.moviehub.ui.settings.SettingsActionErrorCode
import com.benjamin.moviehub.ui.settings.SettingsActionErrorState
import com.benjamin.moviehub.ui.settings.SettingsViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
    private val theme = MutableStateFlow(AppTheme.SYSTEM)

    @Before
    fun setUp() {
        every { preferences.theme } returns theme
    }

    @Test
    fun `current theme follows preference changes`() =
        runTest {
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)
            val job = launch { viewModel.currentTheme.collect() }
            runCurrent()

            assertEquals(AppTheme.SYSTEM, viewModel.currentTheme.value)
            theme.value = AppTheme.DARK
            runCurrent()

            assertEquals(AppTheme.DARK, viewModel.currentTheme.value)
            job.cancel()
        }

    @Test
    fun `theme update persists the selected preference`() =
        runTest {
            coEvery { preferences.setTheme(AppTheme.DARK) } returns Unit
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)

            viewModel.updateTheme(AppTheme.DARK)
            advanceUntilIdle()

            coVerify(exactly = 1) { preferences.setTheme(AppTheme.DARK) }
            assertEquals(SettingsActionErrorState.None, viewModel.themeUpdateError.value)
        }

    @Test
    fun `theme update failure remains pending until acknowledged`() =
        runTest {
            coEvery { preferences.setTheme(AppTheme.DARK) } throws IllegalStateException("theme failure")
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)

            viewModel.updateTheme(AppTheme.DARK)
            advanceUntilIdle()

            assertEquals(
                SettingsActionErrorState.Failure(SettingsActionErrorCode.UPDATE_THEME),
                viewModel.themeUpdateError.value,
            )

            viewModel.acknowledgeThemeUpdateError()
            assertEquals(SettingsActionErrorState.None, viewModel.themeUpdateError.value)
        }

    @Test
    fun `cancelled theme update is not reported as a failure`() =
        runTest {
            coEvery { preferences.setTheme(AppTheme.DARK) } throws CancellationException("cancelled")
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)

            viewModel.updateTheme(AppTheme.DARK)
            advanceUntilIdle()

            assertEquals(SettingsActionErrorState.None, viewModel.themeUpdateError.value)
        }

    @Test
    fun `clearing failure is a pending result distinguishable from success`() =
        runTest {
            coEvery { imageCacheCleaner.clear() } throws IllegalStateException("cache failure")
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)

            viewModel.clearImageCache()
            advanceUntilIdle()

            assertEquals(
                ImageCacheResult.Failed(ImageCacheErrorCode.CLEAR_CACHE),
                viewModel.imageCacheResult.value,
            )
            assertEquals(false, viewModel.isClearing.value)
        }

    @Test
    fun `acknowledging a stale cache result keeps the newer different result`() =
        runTest {
            var fail = true
            coEvery { imageCacheCleaner.clear() } coAnswers {
                if (fail) throw IllegalStateException("cache failure")
            }
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)

            viewModel.clearImageCache()
            advanceUntilIdle()
            assertEquals(
                ImageCacheResult.Failed(ImageCacheErrorCode.CLEAR_CACHE),
                viewModel.imageCacheResult.value,
            )

            fail = false
            viewModel.clearImageCache()
            advanceUntilIdle()
            assertEquals(ImageCacheResult.Cleared, viewModel.imageCacheResult.value)

            // A late acknowledgement of the older failure must not clear the newer success.
            viewModel.acknowledgeImageCacheResult(ImageCacheResult.Failed(ImageCacheErrorCode.CLEAR_CACHE))
            assertEquals(ImageCacheResult.Cleared, viewModel.imageCacheResult.value)

            viewModel.acknowledgeImageCacheResult(ImageCacheResult.Cleared)
            assertEquals(ImageCacheResult.Idle, viewModel.imageCacheResult.value)
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
