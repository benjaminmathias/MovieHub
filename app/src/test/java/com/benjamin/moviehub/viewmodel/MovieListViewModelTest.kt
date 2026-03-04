package com.benjamin.moviehub.viewmodel

import androidx.paging.PagingData
import app.cash.turbine.test
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.list.MovieListUiState
import com.benjamin.moviehub.ui.list.MovieListViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MovieRepository = mockk()
    private lateinit var viewModel: MovieListViewModel

    @Before
    fun setup() {
        coEvery { repository.getPagedMovies(any()) } returns flowOf(PagingData.empty())
        coEvery { repository.getFavoriteMovies() } returns emptyFlow()
        viewModel = MovieListViewModel(repository)
    }

    @Test
    fun `search query should be debounced`() =
        runTest {
            val job =
                launch {
                    viewModel.pagedMovies.collect()
                }

            // On tape "A", puis "Av", puis "Ava"
            viewModel.onSearchQueryChanged("A")
            advanceTimeBy(100)
            viewModel.onSearchQueryChanged("Av")
            advanceTimeBy(100)
            viewModel.onSearchQueryChanged("Ava")

            // À ce stade (200ms écoulées), le repo ne doit pas avoir été appelé avec "A" ou "Av"
            // On avance le temps pour dépasser les 500ms du dernier changement
            advanceTimeBy(600)

            // Le repo doit avoir été appelé avec la dernière valeur "Ava"
            coVerify { repository.getPagedMovies("Ava") }

            // On vérifie qu'il n'a pas été appelé pour les étapes intermédiaires
            coVerify(exactly = 0) { repository.getPagedMovies("A") }
            coVerify(exactly = 0) { repository.getPagedMovies("Av") }

            job.cancel()
        }

    @Test
    fun `retryGlobal should trigger repository reload without changing query`() =
        runTest {
            val job = launch { viewModel.pagedMovies.collect() }

            // État initial (debounce passé)
            advanceTimeBy(600)

            // Le repo a dû être appelé une première fois avec la query vide ""
            coVerify(exactly = 1) { repository.getPagedMovies("") }

            // ACTION : On simule le clic sur Retry
            viewModel.retryGlobal()

            // On laisse un tout petit peu de temps pour que le combine/flatMapLatest réagisse
            advanceTimeBy(100)

            // VERIFICATION : Le repo doit avoir été appelé une DEUXIÈME fois
            coVerify(exactly = 2) { repository.getPagedMovies("") }

            job.cancel()
        }

    @Test
    fun `uiState should update search query immediately`() =
        runTest {
            viewModel.uiState.test {
                // État initial
                val initialState = awaitItem() as MovieListUiState.Success
                assert(initialState.searchQuery == "")

                // Action
                viewModel.onSearchQueryChanged("Batman")

                // Vérification immédiate (pas besoin d'advanceTimeBy ici car le uiState.map n'a pas de debounce)
                val updatedState = awaitItem() as MovieListUiState.Success
                assert(updatedState.searchQuery == "Batman")
            }
        }
}
