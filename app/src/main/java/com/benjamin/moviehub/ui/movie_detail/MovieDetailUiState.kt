package com.benjamin.moviehub.ui.movie_detail

import com.benjamin.moviehub.domain.model.Movie

sealed class MovieDetailUiState {
    object Loading : MovieDetailUiState()
    data class Success(val movie: Movie) : MovieDetailUiState()
    data class Error(val message: String) : MovieDetailUiState()
}