package com.benjamin.moviehub.ui.detail

import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie

sealed class MovieDetailUiState {
    object Loading : MovieDetailUiState()

    data class Success(
        val movie: Movie,
        val actors: List<Actor>,
    ) : MovieDetailUiState()

    data class Error(
        val errorMessage: UiText,
    ) : MovieDetailUiState()
}
