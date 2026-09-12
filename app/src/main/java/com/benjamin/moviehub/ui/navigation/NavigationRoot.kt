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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import com.benjamin.moviehub.ui.components.NetworkSnackbar
import com.benjamin.moviehub.ui.components.NetworkStatusEffect

private data class BottomNavItem(
    val route: Route,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int,
)

private enum class TopLevelTab {
    HOME,
    DISCOVER,
    LIBRARY,
}

private val bottomNavItems =
    listOf(
        BottomNavItem(Route.List, Icons.Default.Home, Icons.Outlined.Home, R.string.home_tab),
        BottomNavItem(Route.Discover, Icons.Default.Explore, Icons.Outlined.Explore, R.string.discover_tab),
        BottomNavItem(Route.Library, Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, R.string.library_tab),
    )

@Composable
fun NavigationRoot(networkStatus: ConnectivityStatus) {
    val homeBackStack = rememberNavBackStack(Route.List)
    val discoverBackStack = rememberNavBackStack(Route.Discover)
    val libraryBackStack = rememberNavBackStack(Route.Library)
    var selectedTab by rememberSaveable { mutableStateOf(TopLevelTab.HOME) }
    val backStack =
        when (selectedTab) {
            TopLevelTab.HOME -> homeBackStack
            TopLevelTab.DISCOVER -> discoverBackStack
            TopLevelTab.LIBRARY -> libraryBackStack
        }
    val currentRoute = backStack.lastOrNull()
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline = networkStatus == ConnectivityStatus.LOST || networkStatus == ConnectivityStatus.UNAVAILABLE

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        val showTopLevelNavigation =
            currentRoute is Route.List || currentRoute is Route.Discover || currentRoute is Route.Library
        fun navigateToTopLevel(route: Route) {
            val tab =
                when (route) {
                    Route.List -> TopLevelTab.HOME
                    Route.Discover -> TopLevelTab.DISCOVER
                    Route.Library -> TopLevelTab.LIBRARY
                    else -> return
                }
            if (selectedTab != tab) {
                selectedTab = tab
            }
        }

        fun openMovieDetails(movieId: Int) {
            val route = Route.Detail(movieId)
            if (backStack.lastOrNull() != route) {
                backStack.add(route)
            }
        }

        val entryProvider =
            entryProvider<NavKey> {
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

                entry<Route.Library> {
                    LibraryEntry(
                        onOpenSettings = { backStack.add(Route.Settings) },
                        onOpenDetails = { id -> openMovieDetails(id) },
                    )
                }

                entry<Route.Settings> {
                    SettingsEntry(onBack = { backStack.removeLastOrNull() })
                }
            }

        val homeEntries = rememberDecoratedEntries(homeBackStack, entryProvider)
        val discoverEntries = rememberDecoratedEntries(discoverBackStack, entryProvider)
        val libraryEntries = rememberDecoratedEntries(libraryBackStack, entryProvider)
        val entries =
            when (selectedTab) {
                TopLevelTab.HOME -> homeEntries
                TopLevelTab.DISCOVER -> discoverEntries
                TopLevelTab.LIBRARY -> libraryEntries
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
                        entries = entries,
                        onBack = {
                            when {
                                backStack.size > 1 -> backStack.removeLastOrNull()
                                selectedTab != TopLevelTab.HOME -> selectedTab = TopLevelTab.HOME
                            }
                        },
                        transitionSpec = { forwardTransition() },
                        popTransitionSpec = { backTransition() },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberDecoratedEntries(
    backStack: List<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> =
    rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider = entryProvider,
    )

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
        icon = { Icon(visuals.icon, contentDescription = null) },
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
        icon = { Icon(visuals.icon, contentDescription = null) },
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
