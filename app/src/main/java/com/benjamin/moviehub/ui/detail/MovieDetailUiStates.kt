package com.benjamin.moviehub.ui.detail

import com.benjamin.moviehub.domain.model.MovieCredits

/**
 * Cast and crew for the detail screen, tracked separately from the main film payload so a
 * failed request stays visible with its own retry instead of degrading to an empty section.
 */
sealed interface MovieCreditsUiState {
    data object Loading : MovieCreditsUiState

    data class Success(
        val credits: MovieCredits,
    ) : MovieCreditsUiState

    data object Error : MovieCreditsUiState
}

/**
 * Sync status of the locally persisted Library flags shown on the detail screen.
 *
 * Library writes stay disabled until the observer produces a first valid snapshot, so the
 * screen never persists a toggle on top of flags that were never loaded.
 */
sealed interface LibraryObservationUiState {
    data object Loading : LibraryObservationUiState

    data object Ready : LibraryObservationUiState

    data object Error : LibraryObservationUiState
}
