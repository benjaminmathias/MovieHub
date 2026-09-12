package com.benjamin.moviehub.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val _favoriteActionErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val favoriteActionErrors = _favoriteActionErrors.asSharedFlow()

        /** One cached paging flow per home category, all shown on the same home screen. */
        val categoryMovies: Map<MovieCategory, Flow<PagingData<Movie>>> =
            MovieCategory.entries.associateWith { category ->
                repository.getCategoryMovies(category).cachedIn(viewModelScope)
            }

        /** Featured movie shown in the hero banner (first item of the popular feed). */
        val heroMovie: StateFlow<Movie?> =
            repository
                .getHeroMovie(MovieCategory.POPULAR)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )

        fun onToggleFavorite(
            movie: Movie,
            isFavorite: Boolean,
        ) {
            viewModelScope.launch {
                try {
                    repository.setFavorite(movie, isFavorite)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _favoriteActionErrors.tryEmit(Unit)
                }
            }
        }
    }
