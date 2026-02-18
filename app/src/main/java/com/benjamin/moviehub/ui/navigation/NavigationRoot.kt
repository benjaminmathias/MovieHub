package com.benjamin.moviehub.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.benjamin.moviehub.ui.movie_detail.MovieDetailScreen
import com.benjamin.moviehub.ui.movie_detail.MovieDetailViewModel
import com.benjamin.moviehub.ui.movie_favorite_list.FavoriteScreen
import com.benjamin.moviehub.ui.movie_favorite_list.FavoriteViewModel
import com.benjamin.moviehub.ui.movie_list.MovieListScreen
import com.benjamin.moviehub.ui.movie_list.MovieListViewModel
import com.benjamin.moviehub.ui.settings.SettingsScreen

@Composable
fun NavigationRoot(
) {
    val backStack = rememberNavBackStack(
        Route.List
    )

    val currentRoute = backStack.lastOrNull()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute is Route.List || currentRoute is Route.FavoriteList) {
                NavigationBar {
                    val items = listOf(
                        BottomNavItem.Home,
                        BottomNavItem.Favorite
                    )
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    backStack.clear()
                                    backStack.add(item.route)
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        }

    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                transitionSpec = {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(400)
                    ) + fadeIn(animationSpec = tween(400))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(400)
                        ) + fadeOut(animationSpec = tween(400))
                    )
                },

                popTransitionSpec = {
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(400)
                    ) + fadeIn(animationSpec = tween(400))).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(400)
                        ) + fadeOut(animationSpec = tween(400))
                    )
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
                            onMovieClick = { id -> backStack.add(Route.Detail(id)) },
                            retryGlobal = viewModel::retryGlobal,
                            onSettingsClick = { backStack.add(Route.Settings) }
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

                    entry<Route.FavoriteList> {
                        val viewModel: FavoriteViewModel = hiltViewModel()
                        val favoriteUiState by viewModel.uiState.collectAsStateWithLifecycle()

                        FavoriteScreen(
                            state = favoriteUiState,
                            onMovieClick = { id ->
                                backStack.add(Route.Detail(id))
                            },
                            onRemoveFavorite = { movie -> viewModel.onToggleFavorite(movie) },
                            onRefresh = { viewModel.refreshFavorite() }
                        )
                    }

                    entry<Route.Settings> {
                        SettingsScreen(
                            onBackClick = { backStack.removeLastOrNull() }
                        )
                    }
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                )
            )
        }
    }
}



