package com.benjamin.moviehub.ui.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieGenre
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bundle-compatible keys used to persist one [DiscoverFilters] instance. Values are stored as
 * primitives so they survive process death without a custom [android.os.Parcelable].
 */
private data class FilterKeys(
    val genreId: String,
    val releaseYear: String,
    val minimumRating: String,
    val sort: String,
)

private val DRAFT_FILTER_KEYS =
    FilterKeys(
        genreId = "discover_draft_genre_id",
        releaseYear = "discover_draft_release_year",
        minimumRating = "discover_draft_minimum_rating",
        sort = "discover_draft_sort",
    )

private val APPLIED_FILTER_KEYS =
    FilterKeys(
        genreId = "discover_applied_genre_id",
        releaseYear = "discover_applied_release_year",
        minimumRating = "discover_applied_minimum_rating",
        sort = "discover_applied_sort",
    )

data class DiscoverUiState(
    val draftFilters: DiscoverFilters = DiscoverFilters(),
    val appliedFilters: DiscoverFilters = DiscoverFilters(),
    val genres: ImmutableList<MovieGenre> = persistentListOf(),
    val isLoadingGenres: Boolean = true,
    val hasGenreError: Boolean = false,
)

@HiltViewModel
class DiscoverViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // Restored before the paging pipeline is observed so the first query already uses the
        // filters the user had applied. Paging data itself is never persisted.
        private val _uiState =
            MutableStateFlow(
                DiscoverUiState(
                    draftFilters = savedStateHandle.readFilters(DRAFT_FILTER_KEYS),
                    appliedFilters = savedStateHandle.readFilters(APPLIED_FILTER_KEYS),
                ),
            )
        val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()
        private var genresJob: Job? = null

        @OptIn(ExperimentalCoroutinesApi::class)
        val discoverResults: Flow<PagingData<Movie>> =
            _uiState
                .map { it.appliedFilters }
                .distinctUntilChanged()
                .flatMapLatest { filters -> repository.getDiscoverMovies(filters) }
                // Cache the complete filter pipeline so recollection reuses it while
                // flatMapLatest can cancel the previous query when filters change.
                .cachedIn(viewModelScope)

        init {
            loadGenres()
        }

        fun onGenreSelected(genreId: Int?) = updateDraft { it.copy(genreId = genreId) }

        fun onReleaseYearSelected(year: Int?) = updateDraft { it.copy(releaseYear = year) }

        fun onMinimumRatingSelected(rating: Double?) = updateDraft { it.copy(minimumVoteAverage = rating) }

        fun onSortSelected(sort: DiscoverSortOption) = updateDraft { it.copy(sort = sort) }

        fun beginFilterEditing() {
            syncDraftToApplied()
        }

        fun discardFilterEdits() {
            syncDraftToApplied()
        }

        fun applyFilters() {
            _uiState.update { state -> state.copy(appliedFilters = state.draftFilters) }
            persistApplied(_uiState.value.appliedFilters)
        }

        fun resetFilters() {
            val defaults = DiscoverFilters()
            _uiState.update { it.copy(draftFilters = defaults, appliedFilters = defaults) }
            persistDraft(defaults)
            persistApplied(defaults)
        }

        fun retryGenres() = loadGenres()

        private fun updateDraft(transform: (DiscoverFilters) -> DiscoverFilters) {
            _uiState.update { state -> state.copy(draftFilters = transform(state.draftFilters)) }
            persistDraft(_uiState.value.draftFilters)
        }

        private fun syncDraftToApplied() {
            _uiState.update { state -> state.copy(draftFilters = state.appliedFilters) }
            persistDraft(_uiState.value.draftFilters)
        }

        private fun persistDraft(filters: DiscoverFilters) {
            savedStateHandle.writeFilters(DRAFT_FILTER_KEYS, filters)
        }

        private fun persistApplied(filters: DiscoverFilters) {
            savedStateHandle.writeFilters(APPLIED_FILTER_KEYS, filters)
        }

        private fun loadGenres() {
            genresJob?.cancel()
            _uiState.update { it.copy(isLoadingGenres = true, hasGenreError = false) }
            genresJob =
                viewModelScope.launch {
                    try {
                        // Fetched outside the state update: the update lambda can be retried and
                        // must not overwrite filter edits made while the request is in flight.
                        val genres = repository.getMovieGenres().toImmutableList()
                        _uiState.update { it.copy(genres = genres, isLoadingGenres = false) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoadingGenres = false, hasGenreError = true) }
                    }
                }
        }
    }

private fun SavedStateHandle.readFilters(keys: FilterKeys): DiscoverFilters =
    DiscoverFilters(
        genreId = get<Int>(keys.genreId),
        releaseYear = get<Int>(keys.releaseYear),
        minimumVoteAverage = get<Double>(keys.minimumRating),
        sort = readSort(keys.sort),
    )

private fun SavedStateHandle.readSort(sortKey: String): DiscoverSortOption {
    val savedName = get<String>(sortKey) ?: return DiscoverSortOption.POPULARITY
    return DiscoverSortOption.entries.firstOrNull { it.name == savedName } ?: DiscoverSortOption.POPULARITY
}

private fun SavedStateHandle.writeFilters(
    keys: FilterKeys,
    filters: DiscoverFilters,
) {
    this[keys.genreId] = filters.genreId
    this[keys.releaseYear] = filters.releaseYear
    this[keys.minimumRating] = filters.minimumVoteAverage
    this[keys.sort] = filters.sort.name
}
