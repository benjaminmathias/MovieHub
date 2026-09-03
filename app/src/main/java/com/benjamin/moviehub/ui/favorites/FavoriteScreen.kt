package com.benjamin.moviehub.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.DeleteBackground
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.MovieItem
import com.benjamin.moviehub.ui.components.MovieShimmerItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    state: MovieFavoriteListUiState,
    onRemoveFavorite: (Movie) -> Unit,
    onMovieClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRetry: () -> Unit,
    favoriteActionErrors: Flow<Unit> = emptyFlow(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val favoriteErrorMessage = stringResource(R.string.error_updating_favorite)

    LaunchedEffect(favoriteActionErrors, favoriteErrorMessage) {
        favoriteActionErrors.collect {
            snackbarHostState.showSnackbar(favoriteErrorMessage)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.favorite_tab)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings_title))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (state) {
                    is MovieFavoriteListUiState.Loading -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            repeat(5) {
                                MovieShimmerItem(compact = true)
                            }
                        }
                    }

                    is MovieFavoriteListUiState.Success -> {
                        if (state.movies.isEmpty()) {
                            EmptyStateView(
                                message = stringResource(R.string.no_favorite_added),
                                icon = Icons.Outlined.FavoriteBorder,
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                            ) {
                                items(state.movies, key = { it.id }) { movie ->

                                    val haptic = LocalHapticFeedback.current
                                    val removeFavoriteLabel =
                                        stringResource(R.string.remove_favorite_accessibility)
                                    val dismissState = rememberSwipeToDismissBoxState()
                                    LaunchedEffect(dismissState.currentValue) {
                                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onRemoveFavorite(movie)
                                        }
                                    }
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .animateItem(),
                                    ) {
                                        SwipeToDismissBox(
                                            state = dismissState,
                                            modifier =
                                                Modifier.semantics {
                                                    customActions =
                                                        listOf(
                                                            CustomAccessibilityAction(
                                                                label = removeFavoriteLabel,
                                                                action = {
                                                                    onRemoveFavorite(movie)
                                                                    true
                                                                },
                                                            ),
                                                        )
                                                },
                                            enableDismissFromStartToEnd = false,
                                            backgroundContent = {
                                                val isVisible =
                                                    dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
                                                        dismissState.targetValue != SwipeToDismissBoxValue.Settled

                                                if (isVisible) {
                                                    DeleteBackground()
                                                }
                                            },
                                            content = {
                                                MovieItem(
                                                    movie = movie,
                                                    onMovieClick = onMovieClick,
                                                    compact = true,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is MovieFavoriteListUiState.Error -> {
                        EmptyStateView(
                            message = stringResource(R.string.error_prefix, state.errorMessage.asString()),
                            icon = Icons.Default.ErrorOutline,
                            onRetry = onRetry,
                        )
                    }
                }
            }
        }
    }
}
