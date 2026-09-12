package com.benjamin.moviehub.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.MovieGenre

@Composable
internal fun GenreFilterSection(
    state: DiscoverUiState,
    filters: DiscoverFilters,
    expanded: Boolean,
    onToggle: () -> Unit,
    onGenreSelected: (Int?) -> Unit,
    onRetryGenres: () -> Unit,
    choiceChipColors: SelectableChipColors,
) {
    FilterSection(
        title = stringResource(R.string.discover_genre),
        selectedLabel =
            when {
                state.isLoadingGenres -> stringResource(R.string.discover_genres_loading)
                state.hasGenreError -> stringResource(R.string.discover_genres_error)
                else ->
                    state.genres.firstOrNull { it.id == filters.genreId }?.name
                        ?: stringResource(R.string.discover_all_genres)
            },
        selected = filters.genreId != null,
        expanded = expanded,
        enabled = !state.isLoadingGenres,
        onClick = onToggle,
        modifier = Modifier.testTag("discover_genre_section"),
    ) {
        when {
            state.isLoadingGenres -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.hasGenreError ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.discover_genres_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onRetryGenres,
                        modifier = Modifier.testTag("discover_retry_genres"),
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            else ->
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2,
                ) {
                    val genreOptions = listOf<MovieGenre?>(null) + state.genres
                    genreOptions.forEach { genre ->
                        FilterChip(
                            selected = filters.genreId == genre?.id,
                            onClick = { onGenreSelected(genre?.id) },
                            colors = choiceChipColors,
                            label = {
                                Text(
                                    genre?.name ?: stringResource(R.string.discover_all_genres),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("discover_genre_option_${genre?.id ?: "all"}"),
                        )
                    }
                }
        }
    }
}

@Composable
internal fun RatingFilterSection(
    filters: DiscoverFilters,
    expanded: Boolean,
    onToggle: () -> Unit,
    onMinimumRatingSelected: (Double?) -> Unit,
    choiceChipColors: SelectableChipColors,
) {
    FilterSection(
        title = stringResource(R.string.discover_min_rating),
        selectedLabel =
            filters.minimumVoteAverage?.let { stringResource(R.string.rating_out_of_ten, it) }
                ?: stringResource(R.string.discover_any_rating),
        selected = filters.minimumVoteAverage != null,
        expanded = expanded,
        onClick = onToggle,
        modifier = Modifier.testTag("discover_rating_section"),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3,
        ) {
            listOf<Double?>(null, 5.0, 6.0, 7.0, 8.0).forEach { rating ->
                FilterChip(
                    selected = filters.minimumVoteAverage == rating,
                    onClick = { onMinimumRatingSelected(rating) },
                    colors = choiceChipColors,
                    label = {
                        Text(
                            rating?.let { stringResource(R.string.rating_out_of_ten, it) }
                                ?: stringResource(R.string.discover_any_rating),
                        )
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
internal fun SortFilterSection(
    filters: DiscoverFilters,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSortSelected: (DiscoverSortOption) -> Unit,
) {
    FilterSection(
        title = stringResource(R.string.discover_sort),
        selectedLabel = sortLabel(filters.sort),
        selected = filters.sort != DiscoverSortOption.POPULARITY,
        expanded = expanded,
        onClick = onToggle,
        modifier = Modifier.testTag("discover_sort_section"),
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            DiscoverSortOption.entries.forEach { sort ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = filters.sort == sort,
                                onClick = { onSortSelected(sort) },
                                role = Role.RadioButton,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sortLabel(sort), modifier = Modifier.weight(1f))
                    RadioButton(selected = filters.sort == sort, onClick = null)
                }
            }
        }
    }
}

@Composable
internal fun FilterSection(
    title: String,
    selectedLabel: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sectionState = stringResource(if (expanded) R.string.discover_section_open else R.string.discover_section_closed)

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = enabled,
                            onClick = onClick,
                            role = Role.Button,
                        ).semantics {
                            stateDescription = sectionState
                        }.heightIn(min = 64.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
internal fun sortLabel(sort: DiscoverSortOption): String =
    stringResource(
        when (sort) {
            DiscoverSortOption.POPULARITY -> R.string.discover_sort_popularity
            DiscoverSortOption.RATING -> R.string.discover_sort_rating
            DiscoverSortOption.RELEASE_DATE -> R.string.discover_sort_release_date
        },
    )
