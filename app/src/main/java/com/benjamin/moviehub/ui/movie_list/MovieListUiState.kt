package com.benjamin.moviehub.ui.movie_list

import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie

sealed class MovieListUiState {
    data object Loading : MovieListUiState()
    data class Success(
        val movies: List<Movie>,
        val emptyMessage: UiText? = null
    ) : MovieListUiState()

    data class Error(val errorMessage: UiText? = null) : MovieListUiState()

}