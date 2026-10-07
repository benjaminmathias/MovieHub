package com.benjamin.moviehub.ui.discover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.MovieGenre
import com.benjamin.moviehub.ui.components.RetryButton
import kotlin.math.roundToInt

private const val DEFAULT_MINIMUM_RATING = 7.0
private const val OLDEST_DECADE = 1870

private enum class FilterGroup {
    GENRE,
    DECADE,
}

@Composable
internal fun DiscoverFilterSheet(
    state: DiscoverUiState,
    currentYear: Int,
    onGenreSelected: (Int?) -> Unit,
    onReleaseDecadeSelected: (Int?) -> Unit,
    onMinimumRatingSelected: (Double?) -> Unit,
    onSortSelected: (DiscoverSortOption) -> Unit,
    onApplyFilters: () -> Unit,
    onResetFilters: () -> Unit,
    onClose: () -> Unit,
    onRetryGenres: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = state.draftFilters
    var selectedGroup by rememberSaveable { mutableStateOf<FilterGroup?>(null) }
    val canApply = filters != state.appliedFilters
    val draftFilterCount = filters.activeFilterCount()

    BackHandler(enabled = selectedGroup != null) { selectedGroup = null }

    Column(
        modifier = modifier.fillMaxWidth().imePadding().testTag("discover_filter_sheet"),
    ) {
        SheetHeader(
            title =
                when (selectedGroup) {
                    null -> stringResource(R.string.discover_filters)
                    FilterGroup.GENRE -> stringResource(R.string.discover_genre)
                    FilterGroup.DECADE -> stringResource(R.string.discover_decade)
                },
            onBack = selectedGroup?.let { { selectedGroup = null } },
            onClose = onClose,
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            when (selectedGroup) {
                null ->
                    MainFilterList(
                        state = state,
                        filters = filters,
                        onOpenGenre = { selectedGroup = FilterGroup.GENRE },
                        onOpenDecade = { selectedGroup = FilterGroup.DECADE },
                        onSortSelected = onSortSelected,
                        onMinimumRatingSelected = onMinimumRatingSelected,
                    )

                FilterGroup.GENRE ->
                    GenrePicker(
                        state = state,
                        filters = filters,
                        onGenreSelected = { genre ->
                            onGenreSelected(genre)
                            selectedGroup = null
                        },
                        onRetryGenres = onRetryGenres,
                    )

                FilterGroup.DECADE ->
                    DecadePicker(
                        filters = filters,
                        currentYear = currentYear,
                        onDecadeSelected = { decade ->
                            onReleaseDecadeSelected(decade)
                            selectedGroup = null
                        },
                    )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onResetFilters,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("discover_reset_filters"),
                ) {
                    Text(stringResource(R.string.discover_reset))
                }
                Button(
                    onClick = onApplyFilters,
                    enabled = canApply,
                    colors =
                        ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                        ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("discover_apply_filters"),
                ) {
                    Text(
                        if (draftFilterCount > 0) {
                            stringResource(R.string.discover_apply_count, draftFilterCount)
                        } else {
                            stringResource(R.string.discover_apply)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    onBack: (() -> Unit)?,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("discover_filter_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = if (onBack != null) 0.dp else 16.dp),
        )
        IconButton(onClick = onClose, modifier = Modifier.testTag("discover_close_filters")) {
            Icon(Icons.Default.Close, stringResource(R.string.close_filters))
        }
    }
}

@Composable
private fun MainFilterList(
    state: DiscoverUiState,
    filters: DiscoverFilters,
    onOpenGenre: () -> Unit,
    onOpenDecade: () -> Unit,
    onSortSelected: (DiscoverSortOption) -> Unit,
    onMinimumRatingSelected: (Double?) -> Unit,
) {
    val genreValue =
        when {
            state.isLoadingGenres -> stringResource(R.string.discover_genres_loading)
            state.hasGenreError -> stringResource(R.string.discover_genres_error)
            else ->
                state.genres.firstOrNull { it.id == filters.genreId }?.name
                    ?: stringResource(R.string.discover_all_genres)
        }
    val decadeValue =
        filters.releaseDecade?.let { stringResource(R.string.discover_decade_title, it) }
            ?: stringResource(R.string.discover_all_decades)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("discover_filter_body")
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SortFilterSection(filters = filters, onSortSelected = onSortSelected)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Column {
                FilterSummaryRow(
                    title = stringResource(R.string.discover_genre),
                    value = genreValue,
                    onClick = onOpenGenre,
                    modifier = Modifier.testTag("discover_genre_row"),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                FilterSummaryRow(
                    title = stringResource(R.string.discover_decade),
                    value = decadeValue,
                    onClick = onOpenDecade,
                    modifier = Modifier.testTag("discover_decade_row"),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                RatingFilterControl(
                    filters = filters,
                    onMinimumRatingSelected = onMinimumRatingSelected,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterSummaryRow(
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun RatingFilterControl(
    filters: DiscoverFilters,
    onMinimumRatingSelected: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratingLabel = stringResource(R.string.discover_filter_by_rating)
    val enabled = filters.minimumVoteAverage != null
    val currentValue = (filters.minimumVoteAverage ?: DEFAULT_MINIMUM_RATING).toFloat().coerceIn(0f, 10f)

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
                    valueRange = 0f..10f,
                    steps = 9,
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
private fun SortFilterSection(
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

@Composable
private fun GenrePicker(
    state: DiscoverUiState,
    filters: DiscoverFilters,
    onGenreSelected: (Int?) -> Unit,
    onRetryGenres: () -> Unit,
) {
    PickerColumn(testTag = "discover_genre_section") {
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
                    RetryButton(onClick = onRetryGenres, modifier = Modifier.testTag("discover_retry_genres"))
                }

            else -> {
                FilterOption(
                    label = stringResource(R.string.discover_all_genres),
                    selected = filters.genreId == null,
                    onClick = { onGenreSelected(null) },
                    modifier = Modifier.fillMaxWidth().testTag("discover_genre_option_all"),
                )
                OptionGrid {
                    state.genres.forEach { genre ->
                        FilterOption(
                            label = genre.name,
                            selected = filters.genreId == genre.id,
                            onClick = { onGenreSelected(genre.id) },
                            modifier = Modifier.testTag("discover_genre_option_${genre.id}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecadePicker(
    filters: DiscoverFilters,
    currentYear: Int,
    onDecadeSelected: (Int?) -> Unit,
) {
    PickerColumn(testTag = "discover_decade_section") {
        FilterOption(
            label = stringResource(R.string.discover_all_decades),
            selected = filters.releaseDecade == null,
            onClick = { onDecadeSelected(null) },
            modifier = Modifier.fillMaxWidth().testTag("discover_decade_option_all"),
        )
        OptionGrid {
            // Decades descend from the current one down to the oldest selectable decade, 1870.
            (decadeStartOf(currentYear) downTo OLDEST_DECADE step 10).forEach { decade ->
                FilterOption(
                    label = stringResource(R.string.discover_decade_title, decade),
                    selected = filters.releaseDecade == decade,
                    onClick = { onDecadeSelected(decade) },
                    modifier = Modifier.testTag("discover_decade_option_$decade"),
                )
            }
        }
    }
}

private fun decadeStartOf(year: Int): Int = (year / 10) * 10

@Composable
private fun PickerColumn(
    testTag: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag(testTag)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = { content() },
    )
}

@Composable
private fun FilterOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(text = label)
        },
        leadingIcon = if (selected) ({ Icon(Icons.Default.Check, contentDescription = null) }) else null,
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        modifier = modifier.heightIn(min = 48.dp),
    )
}

@Composable
private fun OptionGrid(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
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

@Composable
internal fun activeFiltersSummary(
    filters: DiscoverFilters,
    genres: List<MovieGenre>,
): String =
    buildList {
        filters.genreId?.let { genreId ->
            add(genres.firstOrNull { it.id == genreId }?.name ?: stringResource(R.string.discover_selected_genre))
        }
        filters.releaseDecade?.let { add(stringResource(R.string.discover_decade_title, it)) }
        filters.minimumVoteAverage?.let { add(stringResource(R.string.rating_out_of_ten, it)) }
        if (filters.sort != DiscoverSortOption.POPULARITY) add(sortLabel(filters.sort))
    }.joinToString(" · ")

internal fun DiscoverFilters.activeFilterCount(): Int =
    listOf(genreId, releaseDecade, minimumVoteAverage).count { it != null } +
        if (sort != DiscoverSortOption.POPULARITY) 1 else 0
