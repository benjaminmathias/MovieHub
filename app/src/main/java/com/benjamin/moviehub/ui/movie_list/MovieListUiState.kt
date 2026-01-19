package com.benjamin.moviehub.ui.movie_list

import com.benjamin.moviehub.domain.model.Movie

sealed class MovieListUiState {
    data object Loading : MovieListUiState()
    data class Success(val movies: List<Movie>) : MovieListUiState()
    data class Error(val message: String) : MovieListUiState()
}