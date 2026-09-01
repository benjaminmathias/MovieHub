package com.benjamin.moviehub.ui.detail

import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits

sealed class MovieDetailUiState {
    object Loading : MovieDetailUiState()

    data class Success(
        val movie: Movie,
        val credits: MovieCredits,
    ) : MovieDetailUiState()

    data class Error(
        val errorMessage: UiText,
    ) : MovieDetailUiState()
}
