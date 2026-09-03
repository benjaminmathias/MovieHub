package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.data.cache.ImageCacheManager
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import com.benjamin.moviehub.ui.settings.SettingsViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import app.cash.turbine.test
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
            coEvery { imageCacheManager.clear() } returns Unit
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

            viewModel.imageCacheMessages.test {
                viewModel.clearImageCache()
                assertEquals(true, awaitItem())
            }
            assertEquals(false, viewModel.isClearing.value)
            coVerify(exactly = 1) { imageCacheManager.clear() }
        }

    @Test
    fun `clearing image cache exposes error when image cache fails`() =
        runTest {
            coEvery { imageCacheManager.clear() } throws IllegalStateException("cache failure")
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

            viewModel.imageCacheMessages.test {
                viewModel.clearImageCache()
                assertEquals(false, awaitItem())
            }
            assertEquals(false, viewModel.isClearing.value)
        }

    @Test
    fun `clearing image cache emits nothing on cancellation`() =
        runTest {
            coEvery { imageCacheManager.clear() } throws CancellationException("cancelled")
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

            viewModel.imageCacheMessages.test {
                viewModel.clearImageCache()
                expectNoEvents()
            }
            assertEquals(false, viewModel.isClearing.value)
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
