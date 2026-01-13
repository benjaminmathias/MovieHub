package com.benjamin.moviehub.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.domain.model.MovieListUiState
import com.benjamin.moviehub.domain.model.MovieMocks
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MovieListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<MovieListUiState>(MovieListUiState.Loading)

    val uiState: StateFlow<MovieListUiState> = _uiState.asStateFlow()

    init {
        loadMovies()
    }

    private fun loadMovies() {
        viewModelScope.launch {

            _uiState.value = MovieListUiState.Loading

            delay(2000)

            _uiState.value = MovieListUiState.Success(movies = MovieMocks.dummyMovies)
        }
    }
}