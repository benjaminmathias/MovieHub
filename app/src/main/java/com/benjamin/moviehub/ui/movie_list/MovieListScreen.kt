package com.benjamin.moviehub.ui.movie_list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.movie_list.MovieListUiState
import com.benjamin.moviehub.ui.components.MovieItem
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.MovieSearchBar
import com.benjamin.moviehub.ui.components.MovieShimmerItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    uiState: MovieListUiState,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onMovieClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {

    val refreshState = rememberPullToRefreshState()
    val isRefreshing = uiState is MovieListUiState.Loading

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                { Text(if (searchQuery.isEmpty()) "MovieHub Popular" else "Recherche") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            MovieSearchBar(
                query = searchQuery,
                onQueryChanged = {
                    onSearchChanged(it)
                }
            )

            PullToRefreshBox(
                state = refreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                AnimatedContent(
                    targetState = uiState,
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
                        is MovieListUiState.Loading -> {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                repeat(5) {
                                    MovieShimmerItem()
                                }
                            }
                        }

                        is MovieListUiState.Success -> {
                            if (state.movies.isEmpty()) {
                                EmptyStateView(
                                    message = if (searchQuery.isEmpty())
                                        "Aucun film disponible"
                                    else
                                        "Aucun résultat pour \"$searchQuery\"",
                                )
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(state.movies) { movie ->
                                        MovieItem(
                                            movie = movie, onMovieClick = onMovieClick
                                        )
                                    }
                                }
                            }
                        }

                        is MovieListUiState.Error -> {
                            Text(
                                text = "Erreur: ${state.message}",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

