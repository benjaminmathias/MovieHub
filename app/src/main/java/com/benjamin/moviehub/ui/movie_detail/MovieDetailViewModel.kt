package com.benjamin.moviehub.ui.movie_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            try {
                val movie = repository.getMovieDetails(movieId)
                _uiState.value = MovieDetailUiState.Success(movie)
            } catch (e: Exception) {
                _uiState.value = MovieDetailUiState.Error("Erreur de chargement")
            }
        }
    }

    fun toggleFavorite(movie: Movie){
        viewModelScope.launch {
            val newStatus = !movie.isFavorite
            repository.toggleFavorite(movie.id, newStatus)

            loadMovieDetails(movie.id)
        }
    }
}