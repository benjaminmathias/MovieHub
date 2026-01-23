package com.benjamin.moviehub.ui.movie_favorite_list

import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie

sealed class MovieFavoriteListUiState {
    data object Loading : MovieFavoriteListUiState()
    data class Success(
        val movies: List<Movie>,
        val emptyMessage: UiText? = null
    ) : MovieFavoriteListUiState()

    data class Error(val errorMessage: UiText? = null) : MovieFavoriteListUiState()
}