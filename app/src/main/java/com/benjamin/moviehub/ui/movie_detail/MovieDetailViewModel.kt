package com.benjamin.moviehub.ui.movie_detail

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.ui.movie_detail.MovieDetailUiState
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    var uiState by mutableStateOf<MovieDetailUiState>(MovieDetailUiState.Loading)
        private set

    fun loadMovieDetails(movieId: Int) {
        viewModelScope.launch {
            try {
                val movie = repository.getMovieDetails(movieId)
                uiState = MovieDetailUiState.Success(movie)
            } catch (e: Exception) {
                uiState = MovieDetailUiState.Error("Erreur de chargement")
            }
        }
    }
}