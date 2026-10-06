package com.benjamin.moviehub.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.domain.repository.LibraryRepository
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Explicit state of the home hero banner. [Success] carries the movie, or null when the feed is legitimately empty. */
sealed interface HeroMovieUiState {
    data object Loading : HeroMovieUiState

    data class Success(
        val movie: Movie?,
    ) : HeroMovieUiState

    data object Error : HeroMovieUiState
}

@HiltViewModel
class MovieListViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
        private val libraryRepository: LibraryRepository,
    ) : ViewModel() {
        private val heroRetryTrigger = MutableStateFlow(0)
        private val _favoriteErrorPending = MutableStateFlow(false)

        /** A favorite write failed and its message has not been shown yet. */
        val favoriteErrorPending: StateFlow<Boolean> = _favoriteErrorPending.asStateFlow()

        /** One cached paging flow per home category, all shown on the same home screen. */
        val categoryMovies: Map<MovieCategory, Flow<PagingData<Movie>>> =
            MovieCategory.entries.associateWith { category ->
                repository.getCategoryMovies(category).cachedIn(viewModelScope)
            }

        /**
         * Featured movie shown in the hero banner. Read/mapping failures become [HeroMovieUiState.Error];
         * [retryHero] restarts the underlying Room flow on demand, without any automatic retry.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        val heroMovieState: StateFlow<HeroMovieUiState> =
            heroRetryTrigger
                .flatMapLatest {
                    repository
                        .getHeroMovie(MovieCategory.POPULAR)
                        .map<Movie?, HeroMovieUiState> { movie -> HeroMovieUiState.Success(movie) }
                        .onStart { emit(HeroMovieUiState.Loading) }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            emit(HeroMovieUiState.Error)
                        }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = HeroMovieUiState.Loading,
                )

        fun retryHero() {
            heroRetryTrigger.update { it + 1 }
        }

        fun onToggleFavorite(movie: Movie) {
            viewModelScope.launch {
                try {
                    libraryRepository.toggleFavorite(movie)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _favoriteErrorPending.value = true
                }
            }
        }

        /** Clears the pending favorite error once its message has finished being displayed. */
        fun acknowledgeFavoriteError() {
            _favoriteErrorPending.value = false
        }
    }
