package com.benjamin.moviehub.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.HomeMovieCardWidth
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.MovieCardShimmer
import com.benjamin.moviehub.ui.components.RowMovieItem
import com.benjamin.moviehub.ui.components.isInitialError
import com.benjamin.moviehub.ui.components.isInitialLoading
import com.benjamin.moviehub.ui.components.isRefreshError
import com.benjamin.moviehub.ui.components.movieAppendFooter
import com.benjamin.moviehub.ui.components.moviePagingItems

@Composable
internal fun CategoryRow(
    category: MovieCategory,
    lazyPagingItems: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
) {
    val appendErrorMessage = stringResource(R.string.error_loading_more_movies)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(category.labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        if (lazyPagingItems.isRefreshError) {
            EmptyStateView(
                message = stringResource(R.string.error_refreshing_movies),
                onRetry = { lazyPagingItems.retry() },
                compact = true,
            )
        }

        when {
            lazyPagingItems.isInitialLoading -> CategoryRowLoadingShimmer()
            lazyPagingItems.isInitialError ->
                EmptyStateView(
                    message = stringResource(R.string.error_loading_movies),
                    onRetry = { lazyPagingItems.retry() },
                    compact = true,
                )

            else ->
                CategoryRowList(
                    lazyPagingItems = lazyPagingItems,
                    onMovieClick = onMovieClick,
                    onToggleFavorite = onToggleFavorite,
                    appendErrorMessage = appendErrorMessage,
                )
        }
    }
}

@Composable
private fun CategoryRowList(
    lazyPagingItems: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    appendErrorMessage: String,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().testTag("category_row"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        moviePagingItems(lazyPagingItems) { movie ->
            RowMovieItem(
                movie = movie,
                onMovieClick = onMovieClick,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.width(HomeMovieCardWidth),
            )
        }
        movieAppendFooter(
            items = lazyPagingItems,
            errorMessage = appendErrorMessage,
            loading = { MovieCardShimmer(modifier = Modifier.width(HomeMovieCardWidth)) },
        )
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
        items(3) { MovieCardShimmer(modifier = Modifier.width(HomeMovieCardWidth)) }
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
