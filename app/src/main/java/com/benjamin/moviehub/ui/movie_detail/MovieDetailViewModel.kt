package com.benjamin.moviehub.ui.movie_detail

import android.util.Log
import com.benjamin.moviehub.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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

        val currentState = _uiState.value
        if (currentState is MovieDetailUiState.Success && currentState.movie.id == movieId) {
            return
        }

        viewModelScope.launch {
            _uiState.value = MovieDetailUiState.Loading

            try {

                val movieDeferred = async { repository.getMovieDetails(movieId) }
                val actorsDeferred = async { repository.getMovieActors(movieId) }

                val movie = movieDeferred.await()
                val actors = actorsDeferred.await().getOrDefault(emptyList())

                Log.d("Genres", movie.genres.toString())

                _uiState.value = MovieDetailUiState.Success(movie, actors)
            } catch (e: Exception) {
                _uiState.value =
                    MovieDetailUiState.Error(UiText.StringResource(R.string.error_loading_movie_detail))
            }
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            val currentState = _uiState.value as? MovieDetailUiState.Success ?: return@launch
            val newStatus = !movie.isFavorite

            _uiState.value = currentState.copy(
                movie = currentState.movie.copy(isFavorite = newStatus)
            )

            try {
                repository.toggleFavorite(movie, newStatus)
            } catch (e: Exception) {
                _uiState.value = currentState
            }
        }
    }
}