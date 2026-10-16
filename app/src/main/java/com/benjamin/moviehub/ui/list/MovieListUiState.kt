package com.benjamin.moviehub.ui.list

import com.benjamin.moviehub.domain.model.Movie

sealed interface HeroMovieUiState {
    data object Loading : HeroMovieUiState

    data class Success(
        val movie: Movie?,
    ) : HeroMovieUiState

    data class Error(
        val code: HeroMovieErrorCode,
    ) : HeroMovieUiState
}

enum class HeroMovieErrorCode {
    LOAD_FEATURED_MOVIE,
}

enum class FavoriteActionErrorCode {
    UPDATE_FAVORITE,
}

sealed interface FavoriteActionErrorState {
    data object None : FavoriteActionErrorState

    data class Failure(
        val code: FavoriteActionErrorCode,
    ) : FavoriteActionErrorState
}
