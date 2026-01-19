package com.benjamin.moviehub.ui.movie_list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.ui.movie_list.MovieListUiState
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

   /* var searchQuery by mutableStateOf("")
        private set

    var uiState by mutableStateOf<MovieListUiState>(MovieListUiState.Loading)
        private set
        */


    private val _uiState = MutableStateFlow<MovieListUiState>(MovieListUiState.Loading)
    val uiState : StateFlow<MovieListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadPopularMovies()
    }

    private fun loadPopularMovies() {
        viewModelScope.launch {
            try {
                val movies = repository.getPopularMovies()
                //uiState = MovieListUiState.Success(movies)
                _uiState.value = MovieListUiState.Success(movies)
            } catch (e: Exception) {
                Log.e("MovieListViewModel", "Erreur de chargement", e)
                //uiState = MovieListUiState.Error("Impossible de récupérer les films. Vérifiez votre connexion")
                _uiState.value = MovieListUiState.Error("Impossible de récupérer les films. Vérifiez votre connexion")
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        //searchQuery = newQuery
        _searchQuery.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            loadPopularMovies()
        } else {
            searchJob = viewModelScope.launch {
                delay(500)
                searchMovies(newQuery)
            }
        }
    }

    private fun searchMovies(query: String) {
        viewModelScope.launch {
            try {
                //uiState = MovieListUiState.Loading
                _uiState.value = MovieListUiState.Loading
                val movies = repository.getSearchedMovies(query)
                //uiState = MovieListUiState.Success(movies)
                _uiState.value = MovieListUiState.Success(movies)
            } catch (e: Exception) {
                //uiState = MovieListUiState.Error("Aucun résultat trouvé")
                _uiState.value = MovieListUiState.Error("Aucun résultat trouvé")
            }
        }
    }
}