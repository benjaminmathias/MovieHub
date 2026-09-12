package com.benjamin.moviehub.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.previewMovie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    discoverResults: Flow<PagingData<Movie>>,
    onGenreSelected: (Int?) -> Unit,
    onReleaseYearSelected: (Int?) -> Unit,
    onMinimumRatingSelected: (Double?) -> Unit,
    onSortSelected: (DiscoverSortOption) -> Unit,
    onBeginFilterEditing: () -> Unit,
    onApplyFilters: () -> Unit,
    onResetFilters: () -> Unit,
    onDiscardFilterEdits: () -> Unit,
    onRetryGenres: () -> Unit,
    onMovieClick: (Int) -> Unit,
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val yearOptions = remember(currentYear) { listOf<Int?>(null) + (currentYear downTo currentYear - 9) }
    var filterSheetVisible by rememberSaveable { mutableStateOf(false) }
    val activeFilterCount = state.appliedFilters.activeFilterCount()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.discover_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (activeFilterCount > 0) {
                                Badge(
                                    modifier = Modifier.testTag("discover_filter_count"),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Text(activeFilterCount.toString())
                                }
                            }
                        },
                    ) {
                        IconButton(
                            onClick = {
                                onBeginFilterEditing()
                                filterSheetVisible = true
                            },
                            modifier = Modifier.padding(end = 8.dp).size(48.dp).testTag("discover_filter_button"),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (activeFilterCount > 0) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            },
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.discover_filters),
                                    tint =
                                        if (activeFilterCount > 0) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues),
        ) {
            if (activeFilterCount > 0) {
                val summary = activeFiltersSummary(state.appliedFilters, state.genres)
                val summaryDescription = stringResource(R.string.discover_active_filters, summary)
                Text(
                    text = summaryDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { contentDescription = summaryDescription }
                            .testTag("discover_active_filter_summary"),
                )
            }
            DiscoverResults(
                discoverResults = discoverResults,
                onMovieClick = onMovieClick,
                modifier = Modifier.fillMaxSize().weight(1f, fill = true),
            )
        }
    }

    if (filterSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                filterSheetVisible = false
                onDiscardFilterEdits()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            DiscoverFilterSheet(
                state = state,
                currentYear = currentYear,
                yearOptions = yearOptions,
                onGenreSelected = onGenreSelected,
                onReleaseYearSelected = onReleaseYearSelected,
                onMinimumRatingSelected = onMinimumRatingSelected,
                onSortSelected = onSortSelected,
                onApplyFilters = {
                    onApplyFilters()
                    filterSheetVisible = false
                },
                onResetFilters = {
                    onResetFilters()
                    filterSheetVisible = false
                },
                onClose = {
                    filterSheetVisible = false
                    onDiscardFilterEdits()
                },
                onRetryGenres = onRetryGenres,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DiscoverScreenPreview() {
    MovieHubTheme {
        DiscoverScreen(
            state = DiscoverUiState(isLoadingGenres = false),
            discoverResults = flowOf(PagingData.from(listOf(previewMovie()))),
            onGenreSelected = {},
            onReleaseYearSelected = {},
            onMinimumRatingSelected = {},
            onSortSelected = {},
            onBeginFilterEditing = {},
            onApplyFilters = {},
            onResetFilters = {},
            onDiscardFilterEdits = {},
            onRetryGenres = {},
            onMovieClick = {},
        )
    }
}
