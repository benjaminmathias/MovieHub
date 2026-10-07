package com.benjamin.moviehub.ui.discover

import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.MovieGenre
import kotlinx.collections.immutable.ImmutableList

enum class DiscoverErrorCode {
    LOAD_GENRES,
}

sealed interface DiscoverGenresUiState {
    data object Loading : DiscoverGenresUiState

    data class Success(
        val genres: ImmutableList<MovieGenre>,
    ) : DiscoverGenresUiState

    data class Error(
        val code: DiscoverErrorCode,
    ) : DiscoverGenresUiState
}

data class DiscoverUiState(
    val draftFilters: DiscoverFilters = DiscoverFilters(),
    val appliedFilters: DiscoverFilters = DiscoverFilters(),
    val genres: DiscoverGenresUiState = DiscoverGenresUiState.Loading,
)
