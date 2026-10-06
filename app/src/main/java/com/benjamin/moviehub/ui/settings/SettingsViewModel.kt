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

        // Pending image-cache result awaiting display: null = nothing to show, true = cleared,
        // false = clearing failed. A state value survives a collector gap, unlike a one-shot event.
        private val _imageCacheResult = MutableStateFlow<Boolean?>(null)
        val imageCacheResult: StateFlow<Boolean?> = _imageCacheResult.asStateFlow()

        /** A theme write failed and its message has not been shown yet. */
        private val _themeUpdateErrorPending = MutableStateFlow(false)
        val themeUpdateErrorPending: StateFlow<Boolean> = _themeUpdateErrorPending.asStateFlow()

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
                    _themeUpdateErrorPending.value = true
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
                    _imageCacheResult.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _imageCacheResult.value = false
                } finally {
                    _isClearing.value = false
                }
            }
        }

        /** Clears the pending image-cache result only when it still matches [result]. */
        fun acknowledgeImageCacheResult(result: Boolean) {
            if (_imageCacheResult.value == result) _imageCacheResult.value = null
        }

        /** Clears the pending theme update error once its message has finished being displayed. */
        fun acknowledgeThemeUpdateError() {
            _themeUpdateErrorPending.value = false
        }
    }
