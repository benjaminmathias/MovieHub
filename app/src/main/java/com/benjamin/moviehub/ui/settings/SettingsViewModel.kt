package com.benjamin.moviehub.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.repository.UserPreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferenceRepository: UserPreferenceRepository,
    private val database: MovieDatabase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {


    val currentTheme: StateFlow<AppTheme> = userPreferenceRepository.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppTheme.SYSTEM
        )

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPreferenceRepository.setTheme(theme)
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.clearAllTables()
                context.imageLoader.memoryCache?.clear()
                context.imageLoader.diskCache?.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}