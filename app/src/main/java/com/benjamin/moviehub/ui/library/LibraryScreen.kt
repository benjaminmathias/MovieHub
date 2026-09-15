package com.benjamin.moviehub.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.CompactMovieShimmerItem
import com.benjamin.moviehub.ui.components.EmptyStateView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onRemove: (Movie, LibraryTab) -> Unit,
    onMovieClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onRetry: () -> Unit = {},
    actionErrors: Flow<Unit> = emptyFlow(),
) {
    val snackbar = remember { SnackbarHostState() }
    val actionErrorMessage = stringResource(R.string.error_updating_library)
    LaunchedEffect(actionErrors, actionErrorMessage) { actionErrors.collect { snackbar.showSnackbar(actionErrorMessage) } }
    val tabs = listOf(LibraryTab.WATCHLIST, LibraryTab.FAVORITES, LibraryTab.WATCHED)
    var savedTab by rememberSaveable { mutableStateOf(LibraryTab.WATCHLIST.name) }
    val selected = runCatching { LibraryTab.valueOf(savedTab) }.getOrDefault(LibraryTab.WATCHLIST)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.library_tab),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                    ) { Icon(Icons.Default.Settings, stringResource(R.string.settings_title)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tabs.indexOf(selected)) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selected == tab,
                        onClick = { savedTab = tab.name },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(stringResource(tab.label()), style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }
            when (state) {
                LibraryUiState.Loading -> Column(Modifier.verticalScroll(rememberScrollState())) { repeat(5) { CompactMovieShimmerItem() } }
                is LibraryUiState.Success -> {
                    val movies =
                        state.movies.filter {
                            when (selected) {
                                LibraryTab.WATCHLIST -> it.isWatchlist
                                LibraryTab.FAVORITES -> it.isFavorite
                                LibraryTab.WATCHED -> it.isWatched
                            }
                        }
                    if (movies.isEmpty()) {
                        EmptyStateView(message = stringResource(selected.emptyLabel()), icon = selected.emptyIcon())
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(movies, key = { it.id }) { movie ->
                                LibraryMovieItem(
                                    movie = movie,
                                    onMovieClick = onMovieClick,
                                    onRemove = { onRemove(it, selected) },
                                    removeLabel = stringResource(selected.removeLabel()),
                                    removeIcon = selected.removeIcon(),
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
                is LibraryUiState.Error ->
                    EmptyStateView(
                        message = stringResource(R.string.error_prefix, stringResource(state.errorMessage)),
                        icon = Icons.Default.ErrorOutline,
                        onRetry = onRetry,
                    )
            }
        }
    }
}

private fun LibraryTab.label() =
    when (this) {
        LibraryTab.WATCHLIST -> R.string.watchlist_tab
        LibraryTab.FAVORITES -> R.string.favorite_tab
        LibraryTab.WATCHED -> R.string.watched_tab
    }

private fun LibraryTab.emptyLabel() =
    when (this) {
        LibraryTab.WATCHLIST -> R.string.no_watchlist_added
        LibraryTab.FAVORITES -> R.string.no_favorite_added
        LibraryTab.WATCHED -> R.string.no_watched_added
    }

private fun LibraryTab.removeLabel() =
    when (this) {
        LibraryTab.WATCHLIST -> R.string.remove_watchlist_accessibility
        LibraryTab.FAVORITES -> R.string.remove_favorite_accessibility
        LibraryTab.WATCHED -> R.string.remove_watched_accessibility
    }

private fun LibraryTab.emptyIcon() =
    when (this) {
        LibraryTab.WATCHLIST -> Icons.Outlined.BookmarkBorder
        LibraryTab.FAVORITES -> Icons.Outlined.FavoriteBorder
        LibraryTab.WATCHED -> Icons.Filled.CheckCircleOutline
    }

private fun LibraryTab.removeIcon() =
    when (this) {
        LibraryTab.WATCHLIST -> Icons.Filled.BookmarkRemove
        LibraryTab.FAVORITES -> Icons.Filled.HeartBroken
        LibraryTab.WATCHED -> Icons.Filled.VisibilityOff
    }
