package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.data.repository.UserPreferenceRepository
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
import io.mockk.verify
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

    private val preferences: UserPreferenceRepository = mockk()
    private val imageCacheManager: ImageCacheManager = mockk()

    @Before
    fun setUp() {
        every { preferences.theme } returns flowOf(AppTheme.SYSTEM)
    }

    @Test
    fun `clearing image cache reports success and only calls image cache manager`() =
        runTest {
            every { imageCacheManager.clear() } just runs
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

            viewModel.clearImageCacheNow()

            assertEquals(ImageCacheState.Success, viewModel.imageCacheState.value)
            verify(exactly = 1) { imageCacheManager.clear() }
        }

    @Test
    fun `clearing image cache exposes error when image cache fails`() =
        runTest {
            every { imageCacheManager.clear() } throws IllegalStateException("cache failure")
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

            viewModel.clearImageCacheNow()

            assertEquals(ImageCacheState.Error, viewModel.imageCacheState.value)
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
