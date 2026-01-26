package com.benjamin.moviehub.ui.movie_favorite_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<MovieFavoriteListUiState>(MovieFavoriteListUiState.Loading)
    val uiState: StateFlow<MovieFavoriteListUiState> = _uiState.asStateFlow()

    init {
        loadFavoriteMovies()
    }

    private fun loadFavoriteMovies() {
        viewModelScope.launch {
            repository.getFavoriteMovies()
                .catch { e ->
                    _uiState.value =
                        MovieFavoriteListUiState.Error(UiText.StringResource(R.string.error_loading_movies))
                }
                .collect { movies ->
                    val emptyMsg =
                        if (movies.isEmpty()) UiText.StringResource(R.string.no_favorite_added) else null
                    _uiState.value = MovieFavoriteListUiState.Success(movies, emptyMsg)
                }
        }
    }

    fun onToggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite(movie, !movie.isFavorite)
        }
    }

    fun refreshFavorite(){
        loadFavoriteMovies()
    }
}