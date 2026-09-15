package com.benjamin.moviehub.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.ui.components.RetryButton

internal data class DiscoverFilterOptionItem(
    val label: String,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
internal fun DiscoverFilterOption(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small

    Row(
        modifier =
            modifier
                .heightIn(min = 52.dp)
                .clip(shape)
                .background(
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        },
                    shape = shape,
                ).clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DiscoverFilterOptionGrid(options: List<DiscoverFilterOptionItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowOptions.forEach { option ->
                    DiscoverFilterOption(
                        selected = option.selected,
                        onClick = option.onClick,
                        label = option.label,
                        modifier = Modifier.weight(1f).testTag(option.testTag),
                    )
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun FilterSummaryRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SortFilterSection(
    filters: DiscoverFilters,
    onSortSelected: (DiscoverSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("discover_sort_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.discover_sort),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            DiscoverSortOption.entries.forEachIndexed { index, sort ->
                SegmentedButton(
                    selected = filters.sort == sort,
                    onClick = { onSortSelected(sort) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DiscoverSortOption.entries.size,
                        ),
                    label = {
                        Text(
                            text = sortLabel(sort),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            activeContentColor = MaterialTheme.colorScheme.onSurface,
                            activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    modifier = Modifier.testTag("discover_sort_option_${sort.name}"),
                )
            }
        }
    }
}

@Composable
internal fun GenrePicker(
    state: DiscoverUiState,
    filters: DiscoverFilters,
    onGenreSelected: (Int?) -> Unit,
    onRetryGenres: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag("discover_genre_section")
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.isLoadingGenres -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.hasGenreError ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.discover_genres_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    RetryButton(
                        onClick = onRetryGenres,
                        modifier = Modifier.testTag("discover_retry_genres"),
                    )
                }
            else -> {
                DiscoverFilterOption(
                    selected = filters.genreId == null,
                    onClick = { onGenreSelected(null) },
                    label = stringResource(R.string.discover_all_genres),
                    modifier = Modifier.fillMaxWidth().testTag("discover_genre_option_all"),
                )
                DiscoverFilterOptionGrid(
                    options =
                        state.genres.map { genre ->
                            DiscoverFilterOptionItem(
                                label = genre.name,
                                selected = filters.genreId == genre.id,
                                testTag = "discover_genre_option_${genre.id}",
                                onClick = { onGenreSelected(genre.id) },
                            )
                        },
                )
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
