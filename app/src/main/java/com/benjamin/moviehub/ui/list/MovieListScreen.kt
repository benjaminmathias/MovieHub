package com.benjamin.moviehub.ui.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.ui.components.HeroMovieBanner
import com.benjamin.moviehub.ui.components.HeroMovieShimmer
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    categoryMovies: Map<MovieCategory, Flow<PagingData<Movie>>>,
    heroMovie: Movie?,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_label),
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        HomeContent(
            categoryMovies = categoryMovies,
            heroMovie = heroMovie,
            onMovieClick = onMovieClick,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    categoryMovies: Map<MovieCategory, Flow<PagingData<Movie>>>,
    heroMovie: Movie?,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshState = rememberPullToRefreshState()
    val homeMovies =
        MovieCategory.entries.associateWith { category ->
            categoryMovies.getValue(category).collectAsLazyPagingItems()
        }

    PullToRefreshBox(
        state = refreshState,
        isRefreshing = homeMovies.values.any { it.loadState.refresh is LoadState.Loading },
        onRefresh = { homeMovies.values.forEach { it.refresh() } },
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("home_sections"),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            item(key = "hero") {
                if (heroMovie != null) {
                    HeroMovieBanner(
                        movie = heroMovie,
                        onMovieClick = onMovieClick,
                        onToggleFavorite = onToggleFavorite,
                    )
                } else {
                    HeroMovieShimmer()
                }
            }

            MovieCategory.entries.forEach { category ->
                item(key = category.key) {
                    CategoryRow(
                        category = category,
                        lazyPagingItems = homeMovies.getValue(category),
                        onMovieClick = onMovieClick,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }
    }
}
