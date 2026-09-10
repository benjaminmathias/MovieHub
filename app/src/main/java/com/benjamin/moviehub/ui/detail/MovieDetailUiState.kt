package com.benjamin.moviehub.ui.detail

import androidx.annotation.StringRes
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits

sealed class MovieDetailUiState {
    data object Loading : MovieDetailUiState()

    data class Success(
        val movie: Movie,
        val credits: MovieCredits,
        val recommendations: MovieRecommendationsUiState = MovieRecommendationsUiState.Loading,
    ) : MovieDetailUiState()

    data class Error(
        @param:StringRes val errorMessage: Int,
    ) : MovieDetailUiState()
}

sealed interface MovieRecommendationsUiState {
    data object Loading : MovieRecommendationsUiState

    data class Success(
        val movies: List<Movie>,
    ) : MovieRecommendationsUiState

    data object Empty : MovieRecommendationsUiState

    data object Error : MovieRecommendationsUiState
}
