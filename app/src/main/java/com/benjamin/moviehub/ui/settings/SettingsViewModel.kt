package com.benjamin.moviehub.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.data.cache.ImageCacheManager
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userPreferenceRepository: UserPreferencesRepository,
        private val imageCacheManager: ImageCacheManager,
    ) : ViewModel() {
        private val _isClearing = MutableStateFlow(false)
        val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()
        // One-shot UI messages: true = cache cleared, false = clearing failed.
        private val _imageCacheMessages = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
        val imageCacheMessages = _imageCacheMessages.asSharedFlow()
        private val _themeUpdateErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val themeUpdateErrors = _themeUpdateErrors.asSharedFlow()

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
                    _themeUpdateErrors.tryEmit(Unit)
                }
            }
        }

        fun clearImageCache() {
            viewModelScope.launch {
                _isClearing.value = true
                try {
                    imageCacheManager.clear()
                    _imageCacheMessages.tryEmit(true)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _imageCacheMessages.tryEmit(false)
                } finally {
                    _isClearing.value = false
                }
            }
        }
    }
