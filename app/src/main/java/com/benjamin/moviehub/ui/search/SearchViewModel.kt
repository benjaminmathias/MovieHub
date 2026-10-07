package com.benjamin.moviehub.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

private const val SEARCH_QUERY_KEY = "search_query"

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // Single source of truth for the raw query, restored before the pipeline is observed so a
        // restored query searches immediately.
        val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(SEARCH_QUERY_KEY, "")

        private val _activeSearchQuery = MutableStateFlow("")

        /** The normalized query whose search has actually launched. */
        val activeSearchQuery: StateFlow<String> = _activeSearchQuery.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        val searchResults: Flow<PagingData<Movie>> =
            searchQuery
                .debounce { query -> if (query.isBlank()) 0L else 500L }
                .map(String::trim)
                .distinctUntilChanged()
                .onEach { _activeSearchQuery.value = it }
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        flowOf(PagingData.empty())
                    } else {
                        repository.searchMovies(query)
                    }
                }.cachedIn(viewModelScope)

        fun onSearchQueryChanged(newQuery: String) {
            // Persist the raw query so the field is restored exactly as typed.
            savedStateHandle[SEARCH_QUERY_KEY] = newQuery
        }
    }
