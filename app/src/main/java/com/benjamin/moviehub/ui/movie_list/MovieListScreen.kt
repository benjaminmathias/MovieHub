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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benjamin.moviehub.ui.movie_list.MovieListUiState
import com.benjamin.moviehub.ui.components.MovieItem
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.MovieSearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    // onMovieClick: (Int) -> Unit,
    // viewModel: MovieListViewModel = hiltViewModel()
    uiState: MovieListUiState,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onMovieClick: (Int) -> Unit
) {

    //val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    //val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

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
                onQueryChanged = { //viewModel.onSearchQueryChanged(it)
                    onSearchChanged(it)
                }
            )

            Box(modifier = Modifier.weight(1f)) {

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
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        is MovieListUiState.Success -> {
                            if (state.movies.isEmpty()) {
                                EmptyStateView()
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

@Composable
fun MovieListRoute(
    onMovieClick: (Int) -> Unit,
    viewModel: MovieListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    MovieListScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchChanged = viewModel::onSearchQueryChanged,
        onMovieClick = onMovieClick
    )
}

