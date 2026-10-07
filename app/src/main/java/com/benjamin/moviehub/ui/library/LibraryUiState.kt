package com.benjamin.moviehub.ui.library

import com.benjamin.moviehub.domain.model.Movie
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

private fun ImmutableList<Movie>.byLibraryTab(): ImmutableMap<LibraryTab, ImmutableList<Movie>> =
    LibraryTab.entries
        .associateWith { tab -> filter(tab.flag.isSet).toImmutableList() }
        .toImmutableMap()

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Success(
        val movies: ImmutableList<Movie>,
    ) : LibraryUiState {
        // Derived once per state instance; copied states recompute from their own movies.
        val moviesByTab: ImmutableMap<LibraryTab, ImmutableList<Movie>> = movies.byLibraryTab()
    }

    data class Error(
        val code: LibraryLoadErrorCode,
    ) : LibraryUiState
}

enum class LibraryLoadErrorCode {
    LOAD_MOVIES,
}

enum class LibraryActionErrorCode {
    REMOVE_MOVIE,
}

sealed interface LibraryActionErrorState {
    data object None : LibraryActionErrorState

    data class Failure(
        val code: LibraryActionErrorCode,
    ) : LibraryActionErrorState
}
