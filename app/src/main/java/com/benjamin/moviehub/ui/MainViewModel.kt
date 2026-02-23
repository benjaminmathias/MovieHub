package com.benjamin.moviehub.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.data.repository.UserPreferenceRepository
import com.benjamin.moviehub.domain.connectivity.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferenceRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {
    val theme = userPreferencesRepository.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppTheme.SYSTEM
    )

    init {
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                Log.d("Network value :", status.toString())
            }
        }
    }
}