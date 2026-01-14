package com.benjamin.moviehub.domain.model

sealed class MovieListUiState {
    data object Loading : MovieListUiState()
    data class Success(val movies: List<Movie>) : MovieListUiState()
    data class Error(val message: String) : MovieListUiState()
}