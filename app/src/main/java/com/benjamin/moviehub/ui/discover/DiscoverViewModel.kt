package com.benjamin.moviehub.ui.discover

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

data class DiscoverUiState(
    val draftFilters: DiscoverFilters = DiscoverFilters(),
    val appliedFilters: DiscoverFilters = DiscoverFilters(),
    val genres: List<MovieGenre> = emptyList(),
    val isLoadingGenres: Boolean = true,
    val hasGenreError: Boolean = false,
)

@HiltViewModel
class DiscoverViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DiscoverUiState())
        val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val discoverResults: Flow<PagingData<Movie>> =
            _uiState
                .map { it.appliedFilters }
                .distinctUntilChanged()
                .flatMapLatest { filters ->
                    repository.getDiscoverMovies(filters).cachedIn(viewModelScope)
                }.cachedIn(viewModelScope)

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
        }

        fun resetFilters() {
            val defaults = DiscoverFilters()
            _uiState.update { it.copy(draftFilters = defaults, appliedFilters = defaults) }
        }

        fun retryGenres() = loadGenres()

        private fun updateDraft(transform: (DiscoverFilters) -> DiscoverFilters) {
            _uiState.update { state -> state.copy(draftFilters = transform(state.draftFilters)) }
        }

        private fun syncDraftToApplied() {
            _uiState.update { state -> state.copy(draftFilters = state.appliedFilters) }
        }

        private fun loadGenres() {
            _uiState.update { it.copy(isLoadingGenres = true, hasGenreError = false) }
            viewModelScope.launch {
                try {
                    _uiState.update {
                        it.copy(
                            genres = repository.getMovieGenres(),
                            isLoadingGenres = false,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoadingGenres = false, hasGenreError = true) }
                }
            }
        }
    }
