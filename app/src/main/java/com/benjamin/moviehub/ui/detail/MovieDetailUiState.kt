package com.benjamin.moviehub.ui.detail

import androidx.annotation.StringRes
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits

sealed class MovieDetailUiState {
    data object Loading : MovieDetailUiState()

    data class Success(
        val movie: Movie,
        val credits: MovieCredits,
    ) : MovieDetailUiState()

    data class Error(
        @param:StringRes val errorMessage: Int,
    ) : MovieDetailUiState()
}
