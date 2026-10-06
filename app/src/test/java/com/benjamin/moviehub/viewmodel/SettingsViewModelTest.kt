package com.benjamin.moviehub.viewmodel

import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.domain.repository.ImageCacheCleaner
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import com.benjamin.moviehub.ui.settings.SettingsViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `clearing failure is a pending result distinguishable from success`() =
        runTest {
            coEvery { imageCacheCleaner.clear() } throws IllegalStateException("cache failure")
            val viewModel = SettingsViewModel(preferences, imageCacheCleaner)

            viewModel.clearImageCache()
            advanceUntilIdle()

            assertEquals(false, viewModel.imageCacheResult.value)
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
            assertEquals(false, viewModel.imageCacheResult.value)

            fail = false
            viewModel.clearImageCache()
            advanceUntilIdle()
            assertEquals(true, viewModel.imageCacheResult.value)

            // A late acknowledgement of the older failure must not clear the newer success.
            viewModel.acknowledgeImageCacheResult(false)
            assertEquals(true, viewModel.imageCacheResult.value)

            viewModel.acknowledgeImageCacheResult(true)
            assertNull(viewModel.imageCacheResult.value)
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
