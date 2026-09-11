package com.benjamin.moviehub.viewmodel

import androidx.paging.PagingData
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.list.MovieListViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: MovieRepository = mockk()
    private lateinit var viewModel: MovieListViewModel

    @Before
    fun setup() {
        coEvery { repository.getCategoryMovies(any()) } returns flowOf(PagingData.empty())
        coEvery { repository.getHeroMovie(any()) } returns flowOf(null)
        viewModel = MovieListViewModel(repository)
    }

    @Test
    fun `category movies expose the four home categories`() {
        assertEquals(MovieCategory.entries.toSet(), viewModel.categoryMovies.keys)
    }

    @Test
    fun `each home category is loaded independently`() =
        runTest {
            val jobs = viewModel.categoryMovies.values.map { flow -> launch { flow.collect() } }

            MovieCategory.entries.forEach { category ->
                coVerify { repository.getCategoryMovies(category) }
            }

            jobs.forEach { it.cancel() }
        }

    @Test
    fun `hero movie is loaded from the popular feed`() =
        runTest {
            val hero = movie()
            coEvery { repository.getHeroMovie(MovieCategory.POPULAR) } returns flowOf(hero)
            val heroViewModel = MovieListViewModel(repository)

            val job = launch { heroViewModel.heroMovie.collect() }
            advanceUntilIdle()

            assertEquals(hero, heroViewModel.heroMovie.value)
            coVerify { repository.getHeroMovie(MovieCategory.POPULAR) }

            job.cancel()
        }

    @Test
    fun `toggle favorite is persisted through the repository`() =
        runTest {
            val target = movie()
            coEvery { repository.toggleFavorite(target, true) } just runs

            viewModel.onToggleFavorite(target, true)
            advanceUntilIdle()

            coVerify { repository.toggleFavorite(target, true) }
        }

    @Test
    fun `toggle favorite failure emits an error`() =
        runTest {
            val target = movie()
            coEvery { repository.toggleFavorite(target, true) } throws IOException("offline")
            val errors = mutableListOf<Unit>()
            val job = launch { viewModel.favoriteActionErrors.collect { errors += it } }
            advanceUntilIdle()

            viewModel.onToggleFavorite(target, true)
            advanceUntilIdle()

            assertEquals(1, errors.size)
            job.cancel()
        }

    private fun movie() =
        Movie(
            id = 42,
            title = "Hero",
            overview = "o",
            posterPath = "/p.jpg",
            backdropPath = "/b.jpg",
            voteAverage = 8.0,
            releaseDate = "2024-01-01",
            webUrl = null,
            isFavorite = false,
            genreIds = emptyList(),
            genres = emptyList(),
        )
}
