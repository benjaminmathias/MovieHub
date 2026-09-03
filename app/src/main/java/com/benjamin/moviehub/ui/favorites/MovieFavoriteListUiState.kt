package com.benjamin.moviehub.ui.favorites

import androidx.annotation.StringRes
import com.benjamin.moviehub.domain.model.Movie

sealed class MovieFavoriteListUiState {
    data object Loading : MovieFavoriteListUiState()

    data class Success(
        val movies: List<Movie>,
    ) : MovieFavoriteListUiState()

    data class Error(
        @param:StringRes val errorMessage: Int,
    ) : MovieFavoriteListUiState()
}
