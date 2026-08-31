package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import com.benjamin.moviehub.ui.settings.ImageCacheManager
import com.benjamin.moviehub.ui.settings.ImageCacheState
import com.benjamin.moviehub.ui.settings.SettingsViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private val imageCacheManager: ImageCacheManager = mockk()

    @Before
    fun setUp() {
        every { preferences.theme } returns flowOf(AppTheme.SYSTEM)
    }

    @Test
    fun `clearing image cache reports success and only calls image cache manager`() =
        runTest {
            coEvery { imageCacheManager.clear() } coAnswers { Unit }
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

            viewModel.clearImageCacheNow()

            assertEquals(ImageCacheState.Success, viewModel.imageCacheState.value)
            coVerify(exactly = 1) { imageCacheManager.clear() }
        }

    @Test
    fun `clearing image cache exposes error when image cache fails`() =
        runTest {
            coEvery { imageCacheManager.clear() } throws IllegalStateException("cache failure")
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

            viewModel.clearImageCacheNow()

            assertEquals(ImageCacheState.Error, viewModel.imageCacheState.value)
        }

    @Test
    fun `clearing image cache propagates cancellation`() =
        runTest {
            val cancellation = CancellationException("cancelled")
            coEvery { imageCacheManager.clear() } throws cancellation
            val viewModel = SettingsViewModel(preferences, imageCacheManager)
            try {
                viewModel.clearImageCacheNow()
                error("Expected cancellation")
            } catch (error: CancellationException) {
                assertEquals(cancellation, error)
            }
        }

    @Test
    fun `theme changes are persisted`() =
        runTest {
            coEvery { preferences.setTheme(AppTheme.DARK) } just runs
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

            viewModel.updateTheme(AppTheme.DARK)

            coVerify(exactly = 1) { preferences.setTheme(AppTheme.DARK) }
        }
}
