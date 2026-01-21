package com.benjamin.moviehub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.benjamin.moviehub.ui.movie_detail.MovieDetailScreen
import com.benjamin.moviehub.ui.movie_detail.MovieDetailViewModel
import com.benjamin.moviehub.ui.movie_list.MovieListScreen
import com.benjamin.moviehub.ui.movie_list.MovieListViewModel

@Composable
fun NavigationRoot(
) {
    val backStack = rememberNavBackStack(
        Route.List
    )

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) backStack.removeLastOrNull()
        },
        entryProvider = entryProvider {

            entry<Route.List> {
                val viewModel: MovieListViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

                MovieListScreen(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onSearchChanged = viewModel::onSearchQueryChanged,
                    onMovieClick = { id ->
                        backStack.add(Route.Detail(id))
                    },
                    onRefresh = {viewModel.refreshMovies()}
                )
            }

            entry<Route.Detail> { key ->
                val viewModel: MovieDetailViewModel = hiltViewModel()
                val detailsUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(key.movieId) {
                    viewModel.loadMovieDetails(key.movieId)
                }

                MovieDetailScreen(
                    uiState = detailsUiState,
                    onBackClick = { backStack.removeLastOrNull() },
                    onToggleFavorite = { movie -> viewModel.toggleFavorite(movie) }
                )
            }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        )
    )
}
