package com.benjamin.moviehub.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        val pagedMovies =
            _searchQuery
                .debounce { query -> if (query.isEmpty()) 0L else 500L }
                .map(String::trim)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    repository.getPagedMovies(query)
                }.cachedIn(viewModelScope)

        fun onSearchQueryChanged(newQuery: String) {
            _searchQuery.value = newQuery
        }
    }
