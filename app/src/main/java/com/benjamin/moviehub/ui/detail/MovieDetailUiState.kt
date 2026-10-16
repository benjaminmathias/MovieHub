package com.benjamin.moviehub.ui.detail

import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import kotlinx.collections.immutable.ImmutableList

sealed interface MovieDetailUiState {
    data object Loading : MovieDetailUiState

    data class Success(
        val movie: Movie,
        val credits: MovieCreditsUiState,
        val recommendations: MovieRecommendationsUiState = MovieRecommendationsUiState.Loading,
        val isLibraryActionPending: Boolean = false,
        val libraryObservation: LibraryObservationUiState = LibraryObservationUiState.Loading,
    ) : MovieDetailUiState

    data class Error(
        val code: MovieDetailErrorCode,
    ) : MovieDetailUiState
}

enum class MovieDetailErrorCode {
    LOAD_MOVIE,
}

sealed interface MovieRecommendationsUiState {
    data object Loading : MovieRecommendationsUiState

    data class Success(
        val movies: ImmutableList<Movie>,
    ) : MovieRecommendationsUiState

    data object Empty : MovieRecommendationsUiState

    data class Error(
        val code: MovieRecommendationsErrorCode,
    ) : MovieRecommendationsUiState
}

enum class MovieRecommendationsErrorCode {
    LOAD_RECOMMENDATIONS,
}

enum class MovieDetailActionErrorCode {
    UPDATE_LIBRARY,
}

sealed interface MovieDetailActionErrorState {
    data object None : MovieDetailActionErrorState

    data class Failure(
        val code: MovieDetailActionErrorCode,
    ) : MovieDetailActionErrorState
}

sealed interface LibraryObservationUiState {
    data object Loading : LibraryObservationUiState

    data object Ready : LibraryObservationUiState

    data class Error(
        val code: LibraryObservationErrorCode,
    ) : LibraryObservationUiState
}

enum class LibraryObservationErrorCode {
    LOAD_LIBRARY,
}

sealed interface MovieCreditsUiState {
    data object Loading : MovieCreditsUiState

    data class Success(
        val credits: MovieCredits,
    ) : MovieCreditsUiState

    data class Error(
        val code: MovieCreditsErrorCode,
    ) : MovieCreditsUiState
}

enum class MovieCreditsErrorCode {
    LOAD_CREDITS,
}
