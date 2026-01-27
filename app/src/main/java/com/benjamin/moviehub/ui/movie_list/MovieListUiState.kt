package com.benjamin.moviehub.ui.movie_list

import androidx.paging.PagingData
import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.coroutines.flow.Flow

sealed class MovieListUiState {
    data object Loading : MovieListUiState()
    data class Success(
        val pagedMovies: Flow<PagingData<Movie>>,
        val searchQuery: String = "",
        val emptyMessage: UiText? = null
    ) : MovieListUiState()

    data class Error(val errorMessage: UiText? = null) : MovieListUiState()
}