package com.benjamin.moviehub.viewmodel

import androidx.paging.PagingData
import app.cash.turbine.test
import com.benjamin.moviehub.domain.repository.MovieRepository
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
import org.junit.Assert.assertEquals
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
    fun `search query is initially empty and updates immediately`() =
        runTest {
            assertEquals("", viewModel.searchQuery.value)
            viewModel.onSearchQueryChanged("Batman")
            assertEquals("Batman", viewModel.searchQuery.value)
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
    fun `identical query is not reloaded`() =
        runTest {
            val job = launch { viewModel.pagedMovies.collect() }
            viewModel.onSearchQueryChanged("Ava")
            advanceTimeBy(600)
            viewModel.onSearchQueryChanged("Ava")
            advanceTimeBy(600)
            coVerify(exactly = 1) { repository.getPagedMovies("Ava") }
            job.cancel()
        }

    @Test
    fun `paged flow is collected from repository for latest query`() =
        runTest {
            val job = launch { viewModel.pagedMovies.test { awaitItem() } }
            advanceTimeBy(600)
            coVerify(exactly = 1) { repository.getPagedMovies("") }
            job.cancel()
        }

    @Test
    fun `blank search query is treated as popular movies`() =
        runTest {
            val job = launch { viewModel.pagedMovies.collect() }

            viewModel.onSearchQueryChanged("   ")
            advanceTimeBy(600)

            coVerify(exactly = 1) { repository.getPagedMovies("") }
            job.cancel()
        }
}
