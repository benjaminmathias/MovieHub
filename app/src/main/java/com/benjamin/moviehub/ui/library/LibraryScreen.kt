package com.benjamin.moviehub.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.CompactMovieShimmerList
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.PendingSnackbarEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onRemove: (Movie, LibraryTab) -> Unit,
    onMovieClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onRetry: () -> Unit = {},
    actionErrorPending: Boolean = false,
    onActionErrorAcknowledged: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    val actionErrorMessage = stringResource(R.string.error_updating_library)
    PendingSnackbarEffect(
        pending = actionErrorPending,
        snackbarHostState = snackbar,
        message = actionErrorMessage,
        onAcknowledged = onActionErrorAcknowledged,
    )

    var selected by rememberSaveable { mutableStateOf(LibraryTab.WATCHLIST) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.library_tab), color = MaterialTheme.colorScheme.onSurface) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selected.ordinal) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}"),
                    )
                }
            }

            when (state) {
                LibraryUiState.Loading -> CompactMovieShimmerList(Modifier.fillMaxSize())

                is LibraryUiState.Success -> {
                    val movies = state.moviesByTab.getValue(selected)
                    if (movies.isEmpty()) {
                        EmptyStateView(message = stringResource(selected.emptyMessageRes), icon = selected.emptyIcon)
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(movies, key = { it.id }) { movie ->
                                LibraryMovieItem(
                                    movie = movie,
                                    onMovieClick = onMovieClick,
                                    onRemove = { onRemove(it, selected) },
                                    removeLabel = stringResource(selected.removeLabelRes),
                                    removeIcon = selected.removeIcon,
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
