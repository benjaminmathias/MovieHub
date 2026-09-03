package com.benjamin.moviehub.ui.favorites

import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie

sealed class MovieFavoriteListUiState {
    data object Loading : MovieFavoriteListUiState()

    data class Success(
        val movies: List<Movie>,
    ) : MovieFavoriteListUiState()

    data class Error(
        val errorMessage: UiText,
    ) : MovieFavoriteListUiState()
}
