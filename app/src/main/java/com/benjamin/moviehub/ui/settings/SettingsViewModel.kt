package com.benjamin.moviehub.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.domain.repository.ImageCacheCleaner
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userPreferenceRepository: UserPreferencesRepository,
        private val imageCacheCleaner: ImageCacheCleaner,
    ) : ViewModel() {
        private val _isClearing = MutableStateFlow(false)
        val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

        // Pending image-cache result awaiting display. A state value survives a collector gap,
        // unlike a one-shot event.
        private val _imageCacheResult = MutableStateFlow<ImageCacheResult>(ImageCacheResult.Idle)
        val imageCacheResult: StateFlow<ImageCacheResult> = _imageCacheResult.asStateFlow()

        /** A theme write failed and its message has not been shown yet. */
        private val _themeUpdateError = MutableStateFlow<SettingsActionErrorState>(SettingsActionErrorState.None)
        val themeUpdateError: StateFlow<SettingsActionErrorState> = _themeUpdateError.asStateFlow()

        val currentTheme: StateFlow<AppTheme> =
            userPreferenceRepository.theme
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = AppTheme.SYSTEM,
                )

        fun updateTheme(theme: AppTheme) {
            viewModelScope.launch {
                try {
                    userPreferenceRepository.setTheme(theme)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _themeUpdateError.value = SettingsActionErrorState.Failure(SettingsActionErrorCode.UPDATE_THEME)
                }
            }
        }

        fun clearImageCache() {
            // Synchronous guard: a request received while a clear is in flight is ignored so
            // concurrent operations cannot race on the clearing flag.
            if (_isClearing.value) return
            _isClearing.value = true
            viewModelScope.launch {
                try {
                    imageCacheCleaner.clear()
                    _imageCacheResult.value = ImageCacheResult.Cleared
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _imageCacheResult.value = ImageCacheResult.Failed(ImageCacheErrorCode.CLEAR_CACHE)
                } finally {
                    _isClearing.value = false
                }
            }
        }

        /** Clears the pending image-cache result only when it still matches [result]. */
        fun acknowledgeImageCacheResult(result: ImageCacheResult) {
            if (_imageCacheResult.value === result) _imageCacheResult.value = ImageCacheResult.Idle
        }

        /** Clears the pending theme update error once its message has finished being displayed. */
        fun acknowledgeThemeUpdateError() {
            _themeUpdateError.value = SettingsActionErrorState.None
        }
    }
