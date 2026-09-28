package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.ui.components.ActorItem
import com.benjamin.moviehub.ui.components.MovieCardShimmer
import com.benjamin.moviehub.ui.components.PosterMovieItem
import com.benjamin.moviehub.ui.components.previewMovie

@Composable
fun MovieDetailContent(
    movie: Movie,
    credits: MovieCredits,
    modifier: Modifier = Modifier,
    recommendations: MovieRecommendationsUiState = MovieRecommendationsUiState.Empty,
    listState: LazyListState = rememberLazyListState(),
    onToggleFavorite: (() -> Unit)? = null,
    onToggleWatchlist: (() -> Unit)? = null,
    onToggleWatched: (() -> Unit)? = null,
    onOpenTmdb: (() -> Unit)? = null,
    onRecommendationClick: (Int) -> Unit = {},
) {
    // Edge-to-edge insets: the system bottom inset goes into contentPadding, not the
    // Modifier, so content scrolls behind the bars instead of being clipped.
    val navigationInsets = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = navigationInsets.calculateLeftPadding(layoutDirection),
                end = navigationInsets.calculateRightPadding(layoutDirection),
                bottom = 24.dp + navigationInsets.calculateBottomPadding(),
            ),
    ) {
        item(key = "header") {
            MovieDetailHeader(
                movie = movie,
                director = credits.director?.takeIf(String::isNotBlank),
                onToggleFavorite = onToggleFavorite,
                onToggleWatchlist = onToggleWatchlist,
                onToggleWatched = onToggleWatched,
                onOpenTmdb = onOpenTmdb,
            )
        }

        if (movie.overview.isNotBlank()) {
            item(key = "synopsis") {
                DetailSection(
                    title = stringResource(R.string.synopsis),
                ) {
                    // Dedicated Column: without it the two children would overlap inside
                    // DetailSection's Box (the button used to land on the text).
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        var expanded by rememberSaveable(movie.id, movie.overview) {
                            mutableStateOf(false)
                        }
                        Text(
                            text = movie.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = { expanded = !expanded },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text =
                                    stringResource(
                                        if (expanded) R.string.show_less else R.string.read_more,
                                    ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        if (credits.actors.isNotEmpty()) {
            item(key = "cast") {
                DetailSection(
                    title = stringResource(R.string.cast_principal),
                    topPadding = 0.dp,
                    fullBleed = true,
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        items(credits.actors, key = Actor::id) { actor ->
                            ActorItem(actor = actor)
                        }
                    }
                }
            }
        }

        when (recommendations) {
            MovieRecommendationsUiState.Loading -> {
                item(key = "recommendations") {
                    RecommendationsSection {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            items(3) {
                                MovieCardShimmer(modifier = Modifier.width(140.dp))
                            }
                        }
                    }
                }
            }

            is MovieRecommendationsUiState.Success -> {
                item(key = "recommendations") {
                    RecommendationsSection {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            items(recommendations.movies, key = Movie::id) { recommended ->
                                PosterMovieItem(
                                    movie = recommended,
                                    onMovieClick = onRecommendationClick,
                                    modifier = Modifier.width(140.dp),
                                )
                            }
                        }
                    }
                }
            }

            MovieRecommendationsUiState.Empty, MovieRecommendationsUiState.Error -> Unit
        }
    }
}

@Composable
private fun RecommendationsSection(content: @Composable () -> Unit) {
    DetailSection(
        title = stringResource(R.string.you_might_also_like),
        topPadding = 0.dp,
        fullBleed = true,
        content = content,
    )
}

@Composable
private fun DetailSection(
    title: String,
    topPadding: Dp = 12.dp,
    fullBleed: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = topPadding, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .semantics { heading() },
        )
        if (fullBleed) {
            content()
        } else {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                content()
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MovieDetailContentPreview() {
    MovieHubTheme {
        MovieDetailContent(
            movie = previewMovie().copy(runtimeMinutes = 124, voteCount = 1200),
            credits = MovieCredits(director = "James Cameron"),
        )
    }
}
