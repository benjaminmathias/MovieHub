package com.benjamin.moviehub.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.MovieGenre

private enum class DiscoverFilterSection {
    GENRE,
    YEAR,
    RATING,
    SORT,
}

@Composable
internal fun DiscoverFilterSheet(
    state: DiscoverUiState,
    currentYear: Int,
    yearOptions: List<Int?>,
    onGenreSelected: (Int?) -> Unit,
    onReleaseYearSelected: (Int?) -> Unit,
    onMinimumRatingSelected: (Double?) -> Unit,
    onSortSelected: (DiscoverSortOption) -> Unit,
    onApplyFilters: () -> Unit,
    onResetFilters: () -> Unit,
    onClose: () -> Unit,
    onRetryGenres: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = state.draftFilters
    val recentYears = yearOptions.filterNotNull()
    var expandedSection by rememberSaveable { mutableStateOf<DiscoverFilterSection?>(null) }
    var customYearMode by rememberSaveable {
        mutableStateOf(filters.releaseYear != null && filters.releaseYear !in recentYears)
    }
    var customYearText by rememberSaveable {
        mutableStateOf(filters.releaseYear?.takeIf { it !in recentYears }?.toString().orEmpty())
    }
    val customYearInvalid = customYearIsInvalid(customYearMode, customYearText, currentYear)
    val canApply = !customYearInvalid && filters != state.appliedFilters
    val choiceChipColors =
        FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )

    fun toggle(section: DiscoverFilterSection) {
        expandedSection = if (expandedSection == section) null else section
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .imePadding()
                .testTag("discover_filter_sheet"),
    ) {
        SheetHeader(onClose = onClose)

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .testTag("discover_filter_body")
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GenreFilterSection(
                state = state,
                filters = filters,
                expanded = expandedSection == DiscoverFilterSection.GENRE,
                onToggle = { toggle(DiscoverFilterSection.GENRE) },
                onGenreSelected = onGenreSelected,
                onRetryGenres = onRetryGenres,
                choiceChipColors = choiceChipColors,
            )

            YearFilterSection(
                filters = filters,
                yearOptions = yearOptions,
                recentYears = recentYears,
                currentYear = currentYear,
                expanded = expandedSection == DiscoverFilterSection.YEAR,
                onToggle = { toggle(DiscoverFilterSection.YEAR) },
                customYearMode = customYearMode,
                onCustomYearModeChange = { customYearMode = it },
                customYearText = customYearText,
                onCustomYearTextChange = { customYearText = it },
                customYearInvalid = customYearInvalid,
                onReleaseYearSelected = onReleaseYearSelected,
                choiceChipColors = choiceChipColors,
            )

            RatingFilterSection(
                filters = filters,
                expanded = expandedSection == DiscoverFilterSection.RATING,
                onToggle = { toggle(DiscoverFilterSection.RATING) },
                onMinimumRatingSelected = onMinimumRatingSelected,
                choiceChipColors = choiceChipColors,
            )

            SortFilterSection(
                filters = filters,
                expanded = expandedSection == DiscoverFilterSection.SORT,
                onToggle = { toggle(DiscoverFilterSection.SORT) },
                onSortSelected = onSortSelected,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("discover_apply_filters"),
            ) {
                Text(stringResource(R.string.discover_apply))
            }
        }
    }
}

@Composable
private fun SheetHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.discover_filters),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.discover_filter_sheet_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("discover_close_filters"),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close_filters),
            )
        }
    }
}

@Composable
internal fun activeFiltersSummary(
    filters: DiscoverFilters,
    genres: List<MovieGenre>,
): String =
    buildList {
        filters.genreId?.let { genreId ->
            add(genres.firstOrNull { it.id == genreId }?.name ?: stringResource(R.string.discover_selected_genre))
        }
        filters.releaseYear?.let { add(it.toString()) }
        filters.minimumVoteAverage?.let { add(stringResource(R.string.rating_out_of_ten, it)) }
        if (filters.sort != DiscoverSortOption.POPULARITY) {
            add(sortLabel(filters.sort))
        }
    }.joinToString(" · ")

internal fun DiscoverFilters.activeFilterCount(): Int =
    listOf(genreId, releaseYear, minimumVoteAverage).count { it != null } +
        if (sort != DiscoverSortOption.POPULARITY) 1 else 0
