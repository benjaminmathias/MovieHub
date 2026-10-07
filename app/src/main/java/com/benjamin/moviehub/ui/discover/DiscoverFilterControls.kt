package com.benjamin.moviehub.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import kotlin.math.roundToInt

private const val DEFAULT_MINIMUM_RATING = 7.0
private const val MINIMUM_RATING = 1.0

@Composable
internal fun RatingFilterControl(
    filters: DiscoverFilters,
    onMinimumRatingSelected: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratingLabel = stringResource(R.string.discover_filter_by_rating)
    val enabled = filters.minimumVoteAverage != null
    val currentValue =
        (filters.minimumVoteAverage ?: DEFAULT_MINIMUM_RATING)
            .toFloat()
            .coerceIn(MINIMUM_RATING.toFloat(), 10f)

    Column(
        modifier = modifier.fillMaxWidth().testTag("discover_rating_section"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = ratingLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = { checked -> onMinimumRatingSelected(if (checked) DEFAULT_MINIMUM_RATING else null) },
                modifier = Modifier.testTag("discover_rating_switch").semantics { contentDescription = ratingLabel },
            )
        }
        if (enabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Slider(
                    value = currentValue,
                    onValueChange = { value -> onMinimumRatingSelected(value.roundToInt().toDouble()) },
                    valueRange = MINIMUM_RATING.toFloat()..10f,
                    steps = 8,
                    modifier = Modifier.weight(1f).testTag("discover_rating_slider"),
                )
                Text(
                    text = stringResource(R.string.discover_rating_plus, currentValue.roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
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
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = DiscoverSortOption.entries.size),
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
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
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
