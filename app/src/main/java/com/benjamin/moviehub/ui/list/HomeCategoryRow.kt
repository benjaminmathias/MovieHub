package com.benjamin.moviehub.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.HomeMovieCardWidth
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.ui.components.ErrorRetryItem
import com.benjamin.moviehub.ui.components.RowMovieItem
import com.benjamin.moviehub.ui.components.RowMovieShimmerItem
import com.benjamin.moviehub.ui.components.isInitialError
import com.benjamin.moviehub.ui.components.isInitialLoading

@Composable
internal fun CategoryRow(
    category: MovieCategory,
    lazyPagingItems: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            text = stringResource(category.labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        when {
            lazyPagingItems.isInitialLoading -> CategoryRowLoadingShimmer()
            lazyPagingItems.isInitialError ->
                ErrorRetryItem(
                    message = stringResource(R.string.error_loading_movies),
                    onRetry = { lazyPagingItems.retry() },
                )
            else ->
                CategoryRowList(
                    lazyPagingItems = lazyPagingItems,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = onToggleFavorite,
                )
        }
    }
}

@Composable
private fun CategoryRowList(
    lazyPagingItems: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie, Boolean) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().testTag("category_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.id },
        ) { index ->
            lazyPagingItems[index]?.let { movie ->
                RowMovieItem(
                    movie = movie,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.width(HomeMovieCardWidth),
                )
            }
        }

        when (val appendState = lazyPagingItems.loadState.append) {
            is LoadState.Error -> {
                item {
                    ErrorRetryItem(
                        message = stringResource(R.string.error_loading_more_movies),
                        onRetry = { lazyPagingItems.retry() },
                    )
                }
            }

            LoadState.Loading -> {
                item {
                    RowMovieShimmerItem(modifier = Modifier.width(HomeMovieCardWidth))
                }
            }

            is LoadState.NotLoading -> Unit
        }
    }
}

@Composable
private fun CategoryRowLoadingShimmer() {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(3) {
            RowMovieShimmerItem(modifier = Modifier.width(HomeMovieCardWidth))
        }
    }
}

private val MovieCategory.labelRes: Int
    get() =
        when (this) {
            MovieCategory.POPULAR -> R.string.category_popular
            MovieCategory.NOW_PLAYING -> R.string.category_now_playing
            MovieCategory.UPCOMING -> R.string.category_upcoming
            MovieCategory.TOP_RATED -> R.string.category_top_rated
        }
