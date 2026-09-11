package com.benjamin.moviehub.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Explore
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
import androidx.compose.ui.platform.LocalContext
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
import com.benjamin.moviehub.ui.discover.DiscoverScreen
import com.benjamin.moviehub.ui.discover.DiscoverViewModel
import com.benjamin.moviehub.ui.favorites.FavoriteScreen
import com.benjamin.moviehub.ui.favorites.FavoriteViewModel
import com.benjamin.moviehub.ui.list.MovieListScreen
import com.benjamin.moviehub.ui.list.MovieListViewModel
import com.benjamin.moviehub.ui.search.SearchScreen
import com.benjamin.moviehub.ui.search.SearchViewModel
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
        BottomNavItem(Route.Discover, Icons.Default.Explore, Icons.Outlined.Explore, R.string.discover_tab),
        BottomNavItem(Route.FavoriteList, Icons.Default.Favorite, Icons.Outlined.FavoriteBorder, R.string.favorite_tab),
    )

@Composable
fun NavigationRoot(networkStatus: ConnectivityStatus) {
    val backStack = rememberNavBackStack(Route.List)
    val currentRoute = backStack.lastOrNull()
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline = networkStatus == ConnectivityStatus.LOST || networkStatus == ConnectivityStatus.UNAVAILABLE

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        val showTopLevelNavigation =
            currentRoute is Route.List || currentRoute is Route.Discover || currentRoute is Route.FavoriteList
        fun navigateToTopLevel(route: Route) {
            if (currentRoute == route) return
            while (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
            if (route != Route.List) backStack.add(route)
        }

        fun openMovieDetails(movieId: Int) {
            val route = Route.Detail(movieId)
            if (backStack.lastOrNull() != route) {
                backStack.add(route)
            }
        }

        NetworkStatusEffect(networkStatus, snackbarHostState)

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
                        transitionSpec = { forwardTransition() },
                        popTransitionSpec = { backTransition() },
                        entryProvider =
                            entryProvider {
                                entry<Route.List> {
                                    MovieListEntry(
                                        onOpenDetails = { id -> openMovieDetails(id) },
                                        onOpenSettings = { backStack.add(Route.Settings) },
                                        onOpenSearch = { backStack.add(Route.Search) },
                                        snackbarHostState = snackbarHostState,
                                    )
                                }

                                entry<Route.Discover> {
                                    DiscoverEntry(
                                        onOpenDetails = { id -> openMovieDetails(id) },
                                    )
                                }

                                entry<Route.Search> {
                                    SearchEntry(
                                        onBack = { backStack.removeLastOrNull() },
                                        onOpenDetails = { id -> openMovieDetails(id) },
                                    )
                                }

                                entry<Route.Detail> { key ->
                                    MovieDetailEntry(
                                        movieId = key.movieId,
                                        onBack = { backStack.removeLastOrNull() },
                                        onOpenRecommendation = { id -> openMovieDetails(id) },
                                    )
                                }

                                entry<Route.FavoriteList> {
                                    FavoriteListEntry(
                                        onBack = { backStack.removeLastOrNull() },
                                        onOpenSettings = { backStack.add(Route.Settings) },
                                        onOpenDetails = { id -> openMovieDetails(id) },
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
private fun MovieListEntry(
    onOpenDetails: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val viewModel: MovieListViewModel = hiltViewModel()
    val heroMovie by viewModel.heroMovie.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.favoriteActionErrors.collect {
            snackbarHostState.showSnackbar(context.getString(R.string.error_updating_favorite))
        }
    }

    MovieListScreen(
        categoryMovies = viewModel.categoryMovies,
        heroMovie = heroMovie,
        onMovieClick = onOpenDetails,
        onToggleFavorite = viewModel::onToggleFavorite,
        onSearchClick = onOpenSearch,
        onSettingsClick = onOpenSettings,
    )
}

@Composable
private fun SearchEntry(
    onBack: () -> Unit,
    onOpenDetails: (Int) -> Unit,
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    SearchScreen(
        searchResults = viewModel.searchResults,
        searchQuery = searchQuery,
        onSearchChanged = viewModel::onSearchQueryChanged,
        onMovieClick = onOpenDetails,
        onBack = onBack,
    )
}

@Composable
private fun DiscoverEntry(
    onOpenDetails: (Int) -> Unit,
) {
    val viewModel: DiscoverViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DiscoverScreen(
        state = state,
        discoverResults = viewModel.discoverResults,
        onGenreSelected = viewModel::onGenreSelected,
        onReleaseYearSelected = viewModel::onReleaseYearSelected,
        onMinimumRatingSelected = viewModel::onMinimumRatingSelected,
        onSortSelected = viewModel::onSortSelected,
        onBeginFilterEditing = viewModel::beginFilterEditing,
        onApplyFilters = viewModel::applyFilters,
        onResetFilters = viewModel::resetFilters,
        onDiscardFilterEdits = viewModel::discardFilterEdits,
        onRetryGenres = viewModel::retryGenres,
        onMovieClick = onOpenDetails,
    )
}

@Composable
private fun MovieDetailEntry(
    movieId: Int,
    onBack: () -> Unit,
    onOpenRecommendation: (Int) -> Unit,
) {
    val viewModel: MovieDetailViewModel = hiltViewModel()
    val detailsUiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(movieId) {
        viewModel.loadMovieDetails(movieId)
    }

    MovieDetailScreen(
        uiState = detailsUiState,
        onBackClick = onBack,
        onToggleFavorite = viewModel::toggleFavorite,
        onRetry = { viewModel.loadMovieDetails(movieId) },
        favoriteActionErrors = viewModel.favoriteActionErrors,
        onRecommendationClick = onOpenRecommendation,
    )
}

@Composable
private fun FavoriteListEntry(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDetails: (Int) -> Unit,
) {
    val viewModel: FavoriteViewModel = hiltViewModel()
    val favoriteUiState by viewModel.uiState.collectAsStateWithLifecycle()

    FavoriteScreen(
        state = favoriteUiState,
        onBackClick = onBack,
        onSettingsClick = onOpenSettings,
        onMovieClick = onOpenDetails,
        onRemoveFavorite = { movie -> viewModel.onToggleFavorite(movie) },
        onRetry = viewModel::onRetry,
        favoriteActionErrors = viewModel.favoriteActionErrors,
    )
}

private fun AnimatedContentTransitionScope<*>.forwardTransition(): ContentTransform =
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

private fun AnimatedContentTransitionScope<*>.backTransition(): ContentTransform =
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

private data class NavItemVisuals(
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun navItemVisuals(
    item: BottomNavItem,
    selected: Boolean,
): NavItemVisuals {
    val label = stringResource(item.labelRes)
    return NavItemVisuals(label, if (selected) item.selectedIcon else item.unselectedIcon)
}

@Composable
private fun RowScope.MovieBottomNavigationItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val visuals = navItemVisuals(item, selected)

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(visuals.icon, contentDescription = visuals.label) },
        label = { Text(visuals.label) },
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
    val visuals = navItemVisuals(item, selected)

    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(visuals.icon, contentDescription = visuals.label) },
        label = { Text(visuals.label) },
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
