package com.benjamin.moviehub.ui.movie_detail

import com.benjamin.moviehub.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()


    fun loadMovieDetails(movieId: Int) {
        viewModelScope.launch {

            _uiState.value = MovieDetailUiState.Loading

            try {
                val movie = repository.getMovieDetails(movieId)
                _uiState.value = MovieDetailUiState.Success(movie)
            } catch (e: Exception) {
                _uiState.value =
                    MovieDetailUiState.Error(UiText.StringResource(R.string.error_loading_movie_detail))
            }
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            val newStatus = !movie.isFavorite

            val currentState = _uiState.value
            if(currentState is MovieDetailUiState.Success) {
                _uiState.value = MovieDetailUiState.Success(
                    currentState.movie.copy(isFavorite = newStatus)
                )
            }
            repository.toggleFavorite(movie, newStatus)
        }
    }
}