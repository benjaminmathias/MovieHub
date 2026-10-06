package com.benjamin.moviehub.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
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
import com.benjamin.moviehub.domain.connectivity.isOffline
import com.benjamin.moviehub.ui.components.NetworkSnackbar
import com.benjamin.moviehub.ui.components.NetworkStatusEffect

/** The three top-level tabs, each owning its start route, icons and label. */
private enum class TopLevelDestination(
    val route: Route,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @param:StringRes val labelRes: Int,
) {
    HOME(Route.List, Icons.Default.Home, Icons.Outlined.Home, R.string.home_tab),
    DISCOVER(Route.Discover, Icons.Default.Explore, Icons.Outlined.Explore, R.string.discover_tab),
    LIBRARY(Route.Library, Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, R.string.library_tab),
    ;

    companion object {
        fun of(route: NavKey?): TopLevelDestination? = entries.firstOrNull { it.route == route }
    }
}

@Composable
fun NavigationRoot(networkStatus: ConnectivityStatus) {
    val homeBackStack = rememberNavBackStack(Route.List)
    val discoverBackStack = rememberNavBackStack(Route.Discover)
    val libraryBackStack = rememberNavBackStack(Route.Library)
    val backStacks =
        remember(homeBackStack, discoverBackStack, libraryBackStack) {
            mapOf(
                TopLevelDestination.HOME to homeBackStack,
                TopLevelDestination.DISCOVER to discoverBackStack,
                TopLevelDestination.LIBRARY to libraryBackStack,
            )
        }

    var selected by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    val backStack = backStacks.getValue(selected)
    val showTopLevelNavigation = TopLevelDestination.of(backStack.lastOrNull()) != null
    val snackbarHostState = remember { SnackbarHostState() }

    // System back at a non-HOME tab root returns to HOME; nested entries keep the NavDisplay handling.
    BackHandler(enabled = selected != TopLevelDestination.HOME && backStack.size == 1) {
        selected = TopLevelDestination.HOME
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        fun openMovieDetails(
            movieId: Int,
            lifecycle: Lifecycle,
        ) {
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
            val route = Route.Detail(movieId)
            if (backStack.lastOrNull() != route) backStack.add(route)
        }

        val entryProvider =
            entryProvider<NavKey> {
                entry<Route.List> {
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    MovieListEntry(
                        onOpenDetails = { movieId -> openMovieDetails(movieId, lifecycle) },
                        onOpenSettings = dropUnlessResumed { backStack.add(Route.Settings) },
                        onOpenSearch = dropUnlessResumed { backStack.add(Route.Search) },
                        snackbarHostState = snackbarHostState,
                    )
                }

                entry<Route.Discover> {
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    DiscoverEntry(onOpenDetails = { movieId -> openMovieDetails(movieId, lifecycle) })
                }

                entry<Route.Search> {
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    SearchEntry(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenDetails = { movieId -> openMovieDetails(movieId, lifecycle) },
                    )
                }

                entry<Route.Detail> { key ->
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    MovieDetailEntry(
                        movieId = key.movieId,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenRecommendation = { movieId -> openMovieDetails(movieId, lifecycle) },
                    )
                }

                entry<Route.Library> {
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    LibraryEntry(
                        onOpenSettings = dropUnlessResumed { backStack.add(Route.Settings) },
                        onOpenDetails = { movieId -> openMovieDetails(movieId, lifecycle) },
                    )
                }

                entry<Route.Settings> {
                    SettingsEntry(onBack = { backStack.removeLastOrNull() })
                }
            }

        val decoratedEntries =
            mapOf(
                TopLevelDestination.HOME to rememberDecoratedEntries(homeBackStack, entryProvider),
                TopLevelDestination.DISCOVER to rememberDecoratedEntries(discoverBackStack, entryProvider),
                TopLevelDestination.LIBRARY to rememberDecoratedEntries(libraryBackStack, entryProvider),
            )

        NetworkStatusEffect(networkStatus, snackbarHostState)

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = if (useNavigationRail || !showTopLevelNavigation) Modifier.navigationBarsPadding() else Modifier,
                ) { snackbarData ->
                    NetworkSnackbar(snackbarData = snackbarData, isOffline = networkStatus.isOffline)
                }
            },
            bottomBar = {
                if (!useNavigationRail && showTopLevelNavigation) {
                    TopLevelNavigationBar(selected = selected, onSelect = { selected = it })
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
                    TopLevelNavigationRail(selected = selected, onSelect = { selected = it })
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavDisplay(
                        entries = decoratedEntries.getValue(selected),
                        onBack = {
                            when {
                                backStack.size > 1 -> backStack.removeLastOrNull()
                                selected != TopLevelDestination.HOME -> selected = TopLevelDestination.HOME
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

@Composable
private fun TopLevelNavigationBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { TopLevelIcon(destination, selected == destination) },
                label = { Text(stringResource(destination.labelRes)) },
                modifier = Modifier.testTag("nav_tab_${destination.name.lowercase()}"),
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
    }
}

@Composable
private fun TopLevelNavigationRail(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { TopLevelIcon(destination, selected == destination) },
                label = { Text(stringResource(destination.labelRes)) },
                modifier = Modifier.testTag("nav_tab_${destination.name.lowercase()}"),
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
    }
}

@Composable
private fun TopLevelIcon(
    destination: TopLevelDestination,
    selected: Boolean,
) {
    Icon(
        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
        contentDescription = null,
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
