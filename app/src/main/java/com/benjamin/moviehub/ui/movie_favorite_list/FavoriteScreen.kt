package com.benjamin.moviehub.ui.movie_favorite_list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.DeleteBackground
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.MovieItem
import com.benjamin.moviehub.ui.components.MovieShimmerItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    state: MovieFavoriteListUiState,
    onRemoveFavorite: (Movie) -> Unit,
    onMovieClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {

    val refreshState = rememberPullToRefreshState()
    val isRefreshing = state is MovieFavoriteListUiState.Loading

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(stringResource(R.string.favorite_tab)) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PullToRefreshBox(
                state = refreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                            animationSpec = tween(
                                300
                            )
                        )
                    },
                    label = "StateAnimation"
                ) { state ->
                    when (state) {

                        is MovieFavoriteListUiState.Loading -> {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                repeat(5) {
                                    MovieShimmerItem()
                                }
                            }
                        }

                        is MovieFavoriteListUiState.Success -> {
                            if (state.movies.isEmpty()) {
                                EmptyStateView(
                                    message = state.emptyMessage?.asString() ?: "",
                                    icon = Icons.Default.ErrorOutline
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(state.movies, key = { it.id }) { movie ->

                                        val haptic = LocalHapticFeedback.current
                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = { value ->
                                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onRemoveFavorite(movie)
                                                    true
                                                } else {
                                                    false
                                                }
                                            })
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItem()
                                        ) {
                                            SwipeToDismissBox(
                                                state = dismissState,
                                                enableDismissFromStartToEnd = false,
                                                backgroundContent = {
                                                    val isVisible = dismissState.progress > 0f

                                                    if (isVisible) {
                                                        DeleteBackground()
                                                    }
                                                },
                                                content = {
                                                    MovieItem(
                                                        movie = movie,
                                                        onMovieClick = onMovieClick
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is MovieFavoriteListUiState.Error -> {
                            val errorMessage = state.errorMessage?.asString()
                                ?: stringResource(R.string.error_loading_movies)
                            EmptyStateView(
                                message = stringResource(R.string.error_prefix, errorMessage),
                                icon = Icons.Default.ErrorOutline
                            )
                        }
                    }
                }
            }
        }
    }
}