package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.ui.components.ActorItem
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.MovieCardShimmer
import com.benjamin.moviehub.ui.components.PosterMovieItem
import com.benjamin.moviehub.ui.components.previewMovie

private val SectionTitleSpacing = 8.dp
private val SectionRowItemSpacing = 12.dp
private val RecommendationCardWidth = 140.dp
private const val COLLAPSED_SYNOPSIS_LINES = 4

@Composable
fun MovieDetailContent(
    movie: Movie,
    credits: MovieCreditsUiState,
    modifier: Modifier = Modifier,
    recommendations: MovieRecommendationsUiState = MovieRecommendationsUiState.Empty,
    listState: LazyListState = rememberLazyListState(),
    isLibraryActionPending: Boolean = false,
    libraryObservation: LibraryObservationUiState = LibraryObservationUiState.Ready,
    onToggleFavorite: (() -> Unit)? = null,
    onToggleWatchlist: (() -> Unit)? = null,
    onToggleWatched: (() -> Unit)? = null,
    onOpenTmdb: (() -> Unit)? = null,
    onRecommendationClick: (Int) -> Unit = {},
    onRetryCredits: () -> Unit = {},
    onRetryRecommendations: () -> Unit = {},
    onRetryLibraryObservation: () -> Unit = {},
) {
    // Edge-to-edge insets: the system bottom inset goes into contentPadding, not the
    // Modifier, so content scrolls behind the bars instead of being clipped.
    val navigationInsets = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val libraryActionsEnabled = !isLibraryActionPending && libraryObservation is LibraryObservationUiState.Ready
    val creditsSuccess = credits as? MovieCreditsUiState.Success
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("detail_content_list"),
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
                director = creditsSuccess?.credits?.director?.takeIf(String::isNotBlank),
                libraryActionsEnabled = libraryActionsEnabled,
                onToggleFavorite = onToggleFavorite,
                onToggleWatchlist = onToggleWatchlist,
                onToggleWatched = onToggleWatched,
                onOpenTmdb = onOpenTmdb,
            )
        }

        if (libraryObservation is LibraryObservationUiState.Error) {
            item(key = "library_sync_error") {
                Box(
                    modifier = Modifier.fillMaxWidth().testTag("library_sync_error"),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(modifier = Modifier.widthIn(max = DetailContentMaxWidth).fillMaxWidth()) {
                        EmptyStateView(
                            message = stringResource(R.string.error_syncing_library),
                            onRetry = onRetryLibraryObservation,
                            compact = true,
                        )
                    }
                }
            }
        }

        if (movie.overview.isNotBlank()) {
            item(key = "synopsis") {
                DetailSection(
                    title = stringResource(R.string.synopsis),
                ) {
                    SynopsisText(movieId = movie.id, overview = movie.overview)
                }
            }
        }

        when (credits) {
            MovieCreditsUiState.Loading -> Unit

            is MovieCreditsUiState.Success -> {
                if (credits.credits.actors.isNotEmpty()) {
                    item(key = "cast") {
                        DetailSection(
                            title = stringResource(R.string.cast_principal),
                            fullBleed = true,
                        ) {
                            LazyRow {
                                items(credits.credits.actors, key = Actor::id) { actor ->
                                    ActorItem(actor = actor)
                                }
                            }
                        }
                    }
                }
            }

            MovieCreditsUiState.Error -> {
                item(key = "cast_error") {
                    DetailSection(
                        title = stringResource(R.string.cast_principal),
                        modifier = Modifier.testTag("credits_error"),
                    ) {
                        EmptyStateView(
                            message = stringResource(R.string.error_loading_credits),
                            onRetry = onRetryCredits,
                            compact = true,
                        )
                    }
                }
            }
        }

        when (recommendations) {
            MovieRecommendationsUiState.Loading -> {
                item(key = "recommendations") {
                    RecommendationsSection {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(SectionRowItemSpacing),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            items(3) {
                                MovieCardShimmer(modifier = Modifier.width(RecommendationCardWidth))
                            }
                        }
                    }
                }
            }

            is MovieRecommendationsUiState.Success -> {
                item(key = "recommendations") {
                    RecommendationsSection {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(SectionRowItemSpacing),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            items(recommendations.movies, key = Movie::id) { recommended ->
                                PosterMovieItem(
                                    movie = recommended,
                                    onMovieClick = onRecommendationClick,
                                    modifier = Modifier.width(RecommendationCardWidth),
                                )
                            }
                        }
                    }
                }
            }

            MovieRecommendationsUiState.Error -> {
                item(key = "recommendations_error") {
                    RecommendationsSection(modifier = Modifier.testTag("recommendations_error")) {
                        EmptyStateView(
                            message = stringResource(R.string.error_loading_recommendations),
                            onRetry = onRetryRecommendations,
                            compact = true,
                        )
                    }
                }
            }

            MovieRecommendationsUiState.Empty -> Unit
        }
    }
}

/**
 * Synopsis with a collapsed state. The expand toggle is only offered when the text
 * actually exceeds the collapsed line count at the current width, font scale and style.
 */
@Composable
private fun SynopsisText(
    movieId: Int,
    overview: String,
) {
    val textStyle = MaterialTheme.typography.bodyLarge
    val textMeasurer = rememberTextMeasurer()
    var expanded by rememberSaveable(movieId, overview) { mutableStateOf(false) }
    BoxWithConstraints {
        val availableWidth = constraints.maxWidth
        // Measured from the layout constraints instead of the text layout callback so the
        // toggle visibility never feeds back into the measurement.
        val hasOverflow =
            remember(overview, textStyle, availableWidth, textMeasurer) {
                textMeasurer
                    .measure(
                        text = overview,
                        style = textStyle,
                        constraints = Constraints(maxWidth = availableWidth),
                    ).lineCount > COLLAPSED_SYNOPSIS_LINES
            }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = overview,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_SYNOPSIS_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (expanded || hasOverflow) {
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

@Composable
private fun RecommendationsSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DetailSection(
        title = stringResource(R.string.you_might_also_like),
        modifier = modifier,
        fullBleed = true,
        content = content,
    )
}

@Composable
private fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    fullBleed: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = DetailContentMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SectionTitleSpacing),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
}

@PreviewLightDark
@Composable
private fun MovieDetailContentPreview() {
    MovieHubTheme {
        MovieDetailContent(
            movie = previewMovie().copy(runtimeMinutes = 124, voteCount = 1200),
            credits = MovieCreditsUiState.Success(MovieCredits(director = "James Cameron")),
        )
    }
}
