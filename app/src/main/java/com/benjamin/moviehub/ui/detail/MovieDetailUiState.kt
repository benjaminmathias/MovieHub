package com.benjamin.moviehub.ui.detail

import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import kotlinx.collections.immutable.ImmutableList

/** Main renderable state of the detail destination. */
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

/** State of the recommendations section, independent from the main movie payload. */
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

/** Durable error state for library actions performed from the detail screen. */
sealed interface MovieDetailActionErrorState {
    data object None : MovieDetailActionErrorState

    data class Failure(
        val code: MovieDetailActionErrorCode,
    ) : MovieDetailActionErrorState
}

/** Sync status of the locally persisted Library flags shown on the detail screen. */
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

/** Cast and crew for the detail screen, tracked independently from the main payload. */
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
