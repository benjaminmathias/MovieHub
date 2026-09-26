package com.benjamin.moviehub.viewmodel

import app.cash.turbine.test
import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.data.cache.ImageCacheManager
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import com.benjamin.moviehub.ui.settings.SettingsViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
    fun `clearing image cache reports success then failure`() =
        runTest {
            var fail = false
            coEvery { imageCacheManager.clear() } coAnswers {
                if (fail) throw IllegalStateException("cache failure")
            }
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

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
            val viewModel = SettingsViewModel(preferences, imageCacheManager)

            viewModel.updateTheme(AppTheme.DARK)

            coVerify(exactly = 1) { preferences.setTheme(AppTheme.DARK) }
        }
}
