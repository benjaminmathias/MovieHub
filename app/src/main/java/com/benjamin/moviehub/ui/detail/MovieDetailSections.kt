package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Actor
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.ui.components.ActorItem
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.MovieCardShimmer
import com.benjamin.moviehub.ui.components.PosterMovieItem

private val SectionTitleSpacing = 8.dp
private val SectionRowItemSpacing = 12.dp
private val RecommendationCardWidth = 140.dp
private const val COLLAPSED_SYNOPSIS_LINES = 4

@Composable
internal fun SynopsisText(
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
                        text = stringResource(if (expanded) R.string.show_less else R.string.read_more),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MovieCreditsSection(credits: MovieCredits) {
    DetailSection(
        title = stringResource(R.string.cast_principal),
        fullBleed = true,
    ) {
        LazyRow {
            items(credits.actors, key = Actor::id) { actor ->
                ActorItem(actor = actor)
            }
        }
    }
}

@Composable
internal fun MovieCreditsErrorSection(onRetry: () -> Unit) {
    DetailSection(
        title = stringResource(R.string.cast_principal),
        modifier = Modifier.testTag("credits_error"),
    ) {
        EmptyStateView(
            message = stringResource(R.string.error_loading_credits),
            onRetry = onRetry,
            compact = true,
        )
    }
}

@Composable
internal fun MovieRecommendationsLoadingSection() {
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

@Composable
internal fun MovieRecommendationsSection(
    movies: List<Movie>,
    onMovieClick: (Int) -> Unit,
) {
    RecommendationsSection {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(SectionRowItemSpacing),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            items(movies, key = Movie::id) { recommended ->
                PosterMovieItem(
                    movie = recommended,
                    onMovieClick = onMovieClick,
                    modifier = Modifier.width(RecommendationCardWidth),
                )
            }
        }
    }
}

@Composable
internal fun MovieRecommendationsErrorSection(onRetry: () -> Unit) {
    RecommendationsSection(modifier = Modifier.testTag("recommendations_error")) {
        EmptyStateView(
            message = stringResource(R.string.error_loading_recommendations),
            onRetry = onRetry,
            compact = true,
        )
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
internal fun DetailSection(
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
                modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
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
