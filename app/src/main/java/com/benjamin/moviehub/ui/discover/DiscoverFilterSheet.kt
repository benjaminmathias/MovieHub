package com.benjamin.moviehub.ui.discover

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.MovieGenre
import com.benjamin.moviehub.ui.components.RetryButton
import kotlin.math.roundToInt

private const val DEFAULT_MINIMUM_RATING = 7.0
private const val MIN_YEAR = 1870

private enum class FilterGroup {
    GENRE,
    YEAR,
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
    var selectedGroup by rememberSaveable { mutableStateOf<FilterGroup?>(null) }
    var customYearMode by rememberSaveable {
        mutableStateOf(filters.releaseYear != null && filters.releaseYear !in recentYears)
    }
    var customYearText by rememberSaveable {
        mutableStateOf(
            filters.releaseYear
                ?.takeIf { it !in recentYears }
                ?.toString()
                .orEmpty(),
        )
    }
    val customYearInvalid = customYearIsInvalid(customYearMode, customYearText, currentYear)
    val canApply = !customYearInvalid && filters != state.appliedFilters
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
                    FilterGroup.YEAR -> stringResource(R.string.discover_year)
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
                        customYearMode = customYearMode,
                        customYearText = customYearText,
                        onOpenGenre = { selectedGroup = FilterGroup.GENRE },
                        onOpenYear = { selectedGroup = FilterGroup.YEAR },
                        onSortSelected = onSortSelected,
                        onMinimumRatingSelected = onMinimumRatingSelected,
                    )

                FilterGroup.GENRE ->
                    GenrePicker(
                        state = state,
                        filters = filters,
                        onGenreSelected = onGenreSelected,
                        onRetryGenres = onRetryGenres,
                    )

                FilterGroup.YEAR ->
                    YearPicker(
                        filters = filters,
                        yearOptions = yearOptions,
                        recentYears = recentYears,
                        currentYear = currentYear,
                        customYearMode = customYearMode,
                        onCustomYearModeChange = { customYearMode = it },
                        customYearText = customYearText,
                        onCustomYearTextChange = { customYearText = it },
                        customYearInvalid = customYearInvalid,
                        onReleaseYearSelected = onReleaseYearSelected,
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
    customYearMode: Boolean,
    customYearText: String,
    onOpenGenre: () -> Unit,
    onOpenYear: () -> Unit,
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
    val yearValue =
        when {
            customYearMode && customYearText.isNotEmpty() -> customYearText
            customYearMode -> stringResource(R.string.discover_other_year)
            filters.releaseYear != null -> filters.releaseYear.toString()
            else -> stringResource(R.string.discover_all_years)
        }

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
                    modifier = Modifier.padding(horizontal = 16.dp).testTag("discover_genre_row"),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                FilterSummaryRow(
                    title = stringResource(R.string.discover_year),
                    value = yearValue,
                    onClick = onOpenYear,
                    modifier = Modifier.padding(horizontal = 16.dp).testTag("discover_year_row"),
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
                .padding(vertical = 10.dp),
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
    val enabled = filters.minimumVoteAverage != null
    val currentValue = (filters.minimumVoteAverage ?: DEFAULT_MINIMUM_RATING).toFloat().coerceIn(0f, 10f)

    Column(
        modifier = modifier.fillMaxWidth().testTag("discover_rating_section"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.discover_filter_by_rating),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = { checked -> onMinimumRatingSelected(if (checked) DEFAULT_MINIMUM_RATING else null) },
                modifier = Modifier.testTag("discover_rating_switch"),
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
                OptionGrid(
                    options =
                        state.genres.map { genre ->
                            FilterOptionItem(
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
private fun YearPicker(
    filters: DiscoverFilters,
    yearOptions: List<Int?>,
    recentYears: List<Int>,
    currentYear: Int,
    customYearMode: Boolean,
    onCustomYearModeChange: (Boolean) -> Unit,
    customYearText: String,
    onCustomYearTextChange: (String) -> Unit,
    customYearInvalid: Boolean,
    onReleaseYearSelected: (Int?) -> Unit,
) {
    val allYears = yearOptions.firstOrNull()

    PickerColumn(testTag = "discover_year_section") {
        FilterOption(
            label = stringResource(R.string.discover_all_years),
            selected = !customYearMode && filters.releaseYear == allYears,
            onClick = {
                onCustomYearModeChange(false)
                onCustomYearTextChange("")
                onReleaseYearSelected(allYears)
            },
            modifier = Modifier.fillMaxWidth().testTag("discover_year_option_all"),
        )

        OptionGrid(
            options =
                yearOptions.drop(1).map { year ->
                    FilterOptionItem(
                        label = year.toString(),
                        selected = !customYearMode && filters.releaseYear == year,
                        testTag = "discover_year_option_$year",
                        onClick = {
                            onCustomYearModeChange(false)
                            onCustomYearTextChange("")
                            onReleaseYearSelected(year)
                        },
                    )
                } +
                    FilterOptionItem(
                        label = stringResource(R.string.discover_other_year),
                        selected = customYearMode,
                        testTag = "discover_year_option_other",
                        onClick = {
                            onCustomYearModeChange(true)
                            onCustomYearTextChange(
                                filters.releaseYear
                                    ?.takeIf { it !in recentYears }
                                    ?.toString()
                                    .orEmpty(),
                            )
                            onReleaseYearSelected(null)
                        },
                    ),
        )

        if (customYearMode) {
            val showError = customYearInvalid && customYearText.isNotEmpty()
            OutlinedTextField(
                value = customYearText,
                onValueChange = { value ->
                    val digits = value.filter(Char::isDigit).take(4)
                    onCustomYearTextChange(digits)
                    val year = digits.toIntOrNull()
                    when {
                        digits.isEmpty() -> onReleaseYearSelected(null)
                        digits.length == 4 && year in MIN_YEAR..currentYear -> onReleaseYearSelected(year)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("discover_custom_year"),
                label = { Text(stringResource(R.string.discover_other_year)) },
                supportingText = {
                    Text(
                        if (showError) {
                            stringResource(R.string.discover_year_error, MIN_YEAR, currentYear)
                        } else {
                            stringResource(R.string.discover_year_hint, MIN_YEAR, currentYear)
                        },
                    )
                },
                isError = showError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

private data class FilterOptionItem(
    val label: String,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit,
)

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
            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = modifier.heightIn(min = 48.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGrid(options: List<FilterOptionItem>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterOption(
                label = option.label,
                selected = option.selected,
                onClick = option.onClick,
                modifier = Modifier.testTag(option.testTag),
            )
        }
    }
}

internal fun customYearIsInvalid(
    customYearMode: Boolean,
    customYearText: String,
    currentYear: Int,
): Boolean =
    customYearMode &&
        (customYearText.length != 4 || customYearText.toIntOrNull() !in MIN_YEAR..currentYear)

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
        filters.releaseYear?.let { add(it.toString()) }
        filters.minimumVoteAverage?.let { add(stringResource(R.string.rating_out_of_ten, it)) }
        if (filters.sort != DiscoverSortOption.POPULARITY) add(sortLabel(filters.sort))
    }.joinToString(" · ")

internal fun DiscoverFilters.activeFilterCount(): Int =
    listOf(genreId, releaseYear, minimumVoteAverage).count { it != null } +
        if (sort != DiscoverSortOption.POPULARITY) 1 else 0
