package com.benjamin.moviehub.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.benjamin.moviehub.R
import com.benjamin.moviehub.ui.detail.MovieDetailScreen
import com.benjamin.moviehub.ui.detail.MovieDetailViewModel
import com.benjamin.moviehub.ui.components.NetworkSnackbar
import com.benjamin.moviehub.ui.components.NetworkStatusEffect
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import com.benjamin.moviehub.ui.favorites.FavoriteScreen
import com.benjamin.moviehub.ui.favorites.FavoriteViewModel
import com.benjamin.moviehub.ui.list.MovieListScreen
import com.benjamin.moviehub.ui.list.MovieListViewModel
import com.benjamin.moviehub.ui.settings.SettingsScreen

private data class BottomNavItem(
    val route: Route,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int,
)

private val bottomNavItems =
    listOf(
        BottomNavItem(Route.List, Icons.Default.Home, Icons.Outlined.Home, R.string.home_tab),
        BottomNavItem(Route.FavoriteList, Icons.Default.Favorite, Icons.Outlined.FavoriteBorder, R.string.favorite_tab),
    )

@Composable
fun NavigationRoot(networkStatus: ConnectivityStatus) {
    val backStack = rememberNavBackStack(Route.List)
    val currentRoute = backStack.lastOrNull()
    val snackbarHostState = remember { SnackbarHostState() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        val showTopLevelNavigation = currentRoute is Route.List || currentRoute is Route.FavoriteList
        fun navigateToTopLevel(route: Route) {
            when {
                currentRoute == route -> Unit
                route == Route.List && currentRoute is Route.FavoriteList -> backStack.removeLastOrNull()
                else -> backStack.add(route)
            }
        }

        fun openMovieDetails(movieId: Int) {
            val route = Route.Detail(movieId)
            if (backStack.lastOrNull() != route) {
                backStack.add(route)
            }
        }

        NetworkStatusEffect(networkStatus, snackbarHostState)
        val isOffline = networkStatus == ConnectivityStatus.LOST || networkStatus == ConnectivityStatus.UNAVAILABLE

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = if (useNavigationRail || !showTopLevelNavigation) Modifier.navigationBarsPadding() else Modifier,
                ) { snackbarData ->
                    NetworkSnackbar(
                        snackbarData = snackbarData,
                        isOffline = isOffline,
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail && showTopLevelNavigation) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        bottomNavItems.forEach { item ->
                            MovieBottomNavigationItem(
                                item = item,
                                selected = currentRoute == item.route,
                                onClick = { navigateToTopLevel(item.route) },
                            )
                        }
                    }
                }
            },
        ) { paddingValues ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .consumeWindowInsets(paddingValues),
            ) {
                if (useNavigationRail && showTopLevelNavigation) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        bottomNavItems.forEach { item ->
                            MovieRailNavigationItem(
                                item = item,
                                selected = currentRoute == item.route,
                                onClick = { navigateToTopLevel(item.route) },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            when {
                                currentRoute is Route.FavoriteList -> backStack.removeLastOrNull()
                                backStack.size > 1 -> backStack.removeLastOrNull()
                            }
                        },
                        transitionSpec = {
                            (
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300),
                                ) + fadeIn(animationSpec = tween(300))
                            ).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(300),
                                ) + fadeOut(animationSpec = tween(300)),
                            )
                        },
                        popTransitionSpec = {
                            (
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(300),
                                ) + fadeIn(animationSpec = tween(300))
                            ).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300),
                                ) + fadeOut(animationSpec = tween(300)),
                            )
                        },
                        entryProvider =
                            entryProvider {
                                entry<Route.List> {
                                    val viewModel: MovieListViewModel = hiltViewModel()
                                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

                                    MovieListScreen(
                                        pagedMovies = viewModel.pagedMovies,
                                        searchQuery = searchQuery,
                                        onSearchChanged = viewModel::onSearchQueryChanged,
                                        onMovieClick = { id -> openMovieDetails(id) },
                                        onSettingsClick = { backStack.add(Route.Settings) },
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
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onRetry = { viewModel.loadMovieDetails(key.movieId) },
                                        favoriteActionErrors = viewModel.favoriteActionErrors,
                                    )
                                }

                                entry<Route.FavoriteList> {
                                    val viewModel: FavoriteViewModel = hiltViewModel()
                                    val favoriteUiState by viewModel.uiState.collectAsStateWithLifecycle()

                                    FavoriteScreen(
                                        state = favoriteUiState,
                                        onBackClick = { backStack.removeLastOrNull() },
                                        onSettingsClick = { backStack.add(Route.Settings) },
                                        onMovieClick = { id -> openMovieDetails(id) },
                                        onRemoveFavorite = { movie -> viewModel.onToggleFavorite(movie) },
                                        onRetry = viewModel::onRetry,
                                        favoriteActionErrors = viewModel.favoriteActionErrors,
                                    )
                                }

                                entry<Route.Settings> {
                                    SettingsScreen(
                                        onBackClick = { backStack.removeLastOrNull() },
                                    )
                                }
                            },
                        entryDecorators =
                            listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.MovieBottomNavigationItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(item.labelRes)
    val icon = if (selected) item.selectedIcon else item.unselectedIcon

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}

@Composable
private fun MovieRailNavigationItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(item.labelRes)
    val icon = if (selected) item.selectedIcon else item.unselectedIcon

    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors =
            NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}
