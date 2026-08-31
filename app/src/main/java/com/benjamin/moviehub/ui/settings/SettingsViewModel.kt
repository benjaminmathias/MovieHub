package com.benjamin.moviehub.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.core.util.AppTheme
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

sealed interface ImageCacheState {
    data object Idle : ImageCacheState

    data object Loading : ImageCacheState

    data object Success : ImageCacheState

    data object Error : ImageCacheState
}

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userPreferenceRepository: UserPreferencesRepository,
        private val imageCacheManager: ImageCacheManager,
    ) : ViewModel() {
        private val _imageCacheState = MutableStateFlow<ImageCacheState>(ImageCacheState.Idle)
        val imageCacheState: StateFlow<ImageCacheState> = _imageCacheState.asStateFlow()
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
                clearImageCacheNow()
            }
        }

        internal suspend fun clearImageCacheNow() {
            _imageCacheState.value = ImageCacheState.Loading
            try {
                imageCacheManager.clear()
                _imageCacheState.value = ImageCacheState.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _imageCacheState.value = ImageCacheState.Error
            }
        }

        fun resetImageCacheState() {
            _imageCacheState.value = ImageCacheState.Idle
        }
    }
