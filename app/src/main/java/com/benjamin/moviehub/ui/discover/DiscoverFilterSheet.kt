package com.benjamin.moviehub.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieGenre
import com.benjamin.moviehub.ui.components.CompactMovieShimmerItem
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.ErrorRetryItem
import com.benjamin.moviehub.ui.components.PosterMovieItem
import com.benjamin.moviehub.ui.components.PosterMovieShimmerItem
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

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
    val genreOptions = listOf<MovieGenre?>(null) + state.genres
    val ratingOptions = listOf<Double?>(null, 5.0, 6.0, 7.0, 8.0)
    val choiceChipColors =
        FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )

    val recentYears = yearOptions.filterNotNull()
    var expandedSection by rememberSaveable { mutableStateOf<DiscoverFilterSection?>(null) }
    var customYearMode by rememberSaveable {
        mutableStateOf(filters.releaseYear != null && filters.releaseYear !in recentYears)
    }
    var customYearText by rememberSaveable {
        mutableStateOf(filters.releaseYear?.takeIf { it !in recentYears }?.toString().orEmpty())
    }
    val customYearValue = customYearText.toIntOrNull()
    val customYearInvalid =
        customYearMode &&
            (customYearText.length != 4 || customYearValue !in 1870..currentYear)
    val canApply = !customYearInvalid && filters != state.appliedFilters

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .imePadding()
                .testTag("discover_filter_sheet"),
    ) {
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
                    modifier = Modifier.padding(top = 4.dp),
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

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .testTag("discover_filter_body")
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

                DiscoverFilterSection(
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
                    expanded = expandedSection == DiscoverFilterSection.GENRE,
                    enabled = !state.isLoadingGenres,
                    onClick = {
                        expandedSection =
                            if (expandedSection == DiscoverFilterSection.GENRE) {
                                null
                            } else {
                                DiscoverFilterSection.GENRE
                            }
                    },
                    modifier = Modifier.testTag("discover_genre_section"),
                ) {
                    if (state.isLoadingGenres) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else if (state.hasGenreError) {
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
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 2,
                        ) {
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

                DiscoverFilterSection(
                    title = stringResource(R.string.discover_year),
                    selectedLabel =
                        when {
                            customYearMode && customYearText.isNotEmpty() -> customYearText
                            customYearMode -> stringResource(R.string.discover_other_year)
                            filters.releaseYear != null -> filters.releaseYear.toString()
                            else -> stringResource(R.string.discover_all_years)
                        },
                    selected = filters.releaseYear != null || customYearMode,
                    expanded = expandedSection == DiscoverFilterSection.YEAR,
                    onClick = {
                        expandedSection =
                            if (expandedSection == DiscoverFilterSection.YEAR) {
                                null
                            } else {
                                DiscoverFilterSection.YEAR
                            }
                    },
                    modifier = Modifier.testTag("discover_year_section"),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3,
                    ) {
                        yearOptions.forEach { year ->
                            FilterChip(
                                selected = !customYearMode && filters.releaseYear == year,
                                onClick = {
                                    customYearMode = false
                                    customYearText = ""
                                    onReleaseYearSelected(year)
                                },
                                colors = choiceChipColors,
                                label = {
                                    Text(year?.toString() ?: stringResource(R.string.discover_all_years))
                                },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("discover_year_option_${year ?: "all"}"),
                            )
                        }
                        FilterChip(
                            selected = customYearMode,
                            onClick = {
                                customYearMode = true
                                customYearText =
                                    filters.releaseYear
                                        ?.takeIf { it !in recentYears }
                                        ?.toString()
                                        .orEmpty()
                                onReleaseYearSelected(null)
                            },
                            colors = choiceChipColors,
                            label = { Text(stringResource(R.string.discover_other_year)) },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("discover_year_option_other"),
                        )
                    }

                    if (customYearMode) {
                        OutlinedTextField(
                            value = customYearText,
                            onValueChange = { value ->
                                val digits = value.filter(Char::isDigit).take(4)
                                customYearText = digits
                                val year = digits.toIntOrNull()
                                if (digits.isEmpty()) {
                                    onReleaseYearSelected(null)
                                } else if (digits.length == 4 && year in 1870..currentYear) {
                                    onReleaseYearSelected(year)
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .testTag("discover_custom_year"),
                            label = { Text(stringResource(R.string.discover_other_year)) },
                            supportingText = {
                                Text(
                                    if (customYearInvalid) {
                                        stringResource(R.string.discover_year_error, 1870, currentYear)
                                    } else {
                                        stringResource(R.string.discover_year_hint, 1870, currentYear)
                                    },
                                )
                            },
                            isError = customYearInvalid,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }

                DiscoverFilterSection(
                    title = stringResource(R.string.discover_min_rating),
                    selectedLabel =
                        filters.minimumVoteAverage?.let { stringResource(R.string.rating_out_of_ten, it) }
                            ?: stringResource(R.string.discover_any_rating),
                    selected = filters.minimumVoteAverage != null,
                    expanded = expandedSection == DiscoverFilterSection.RATING,
                    onClick = {
                        expandedSection =
                            if (expandedSection == DiscoverFilterSection.RATING) {
                                null
                            } else {
                                DiscoverFilterSection.RATING
                            }
                    },
                    modifier = Modifier.testTag("discover_rating_section"),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3,
                    ) {
                        ratingOptions.forEach { rating ->
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

                DiscoverFilterSection(
                    title = stringResource(R.string.discover_sort),
                    selectedLabel = sortLabel(filters.sort),
                    selected = filters.sort != DiscoverSortOption.POPULARITY,
                    expanded = expandedSection == DiscoverFilterSection.SORT,
                    onClick = {
                        expandedSection =
                            if (expandedSection == DiscoverFilterSection.SORT) {
                                null
                            } else {
                                DiscoverFilterSection.SORT
                            }
                    },
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
private fun DiscoverFilterSection(
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

@Composable
internal fun sortLabel(sort: DiscoverSortOption): String =
    stringResource(
        when (sort) {
            DiscoverSortOption.POPULARITY -> R.string.discover_sort_popularity
            DiscoverSortOption.RATING -> R.string.discover_sort_rating
            DiscoverSortOption.RELEASE_DATE -> R.string.discover_sort_release_date
        },
    )

internal fun DiscoverFilters.activeFilterCount(): Int =
    listOf(genreId, releaseYear, minimumVoteAverage).count { it != null } +
        if (sort != DiscoverSortOption.POPULARITY) 1 else 0
