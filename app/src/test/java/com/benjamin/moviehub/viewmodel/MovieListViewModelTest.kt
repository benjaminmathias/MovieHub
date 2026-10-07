package com.benjamin.moviehub.viewmodel

import androidx.paging.PagingData
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.domain.repository.LibraryRepository
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.ui.list.FavoriteActionErrorCode
import com.benjamin.moviehub.ui.list.FavoriteActionErrorState
import com.benjamin.moviehub.ui.list.HeroMovieErrorCode
import com.benjamin.moviehub.ui.list.HeroMovieUiState
import com.benjamin.moviehub.ui.list.MovieListViewModel
import com.benjamin.moviehub.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
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
    private val libraryRepository: LibraryRepository = mockk()
    private lateinit var viewModel: MovieListViewModel

    @Before
    fun setup() {
        coEvery { repository.getCategoryMovies(any()) } returns flowOf(PagingData.empty())
        coEvery { repository.getHeroMovie(any()) } returns flowOf(null)
        viewModel = MovieListViewModel(repository, libraryRepository)
    }

    @Test
    fun `hero read failure becomes an error and retry restarts the room flow`() =
        runTest {
            val hero = movie()
            val failedHero = flow<Movie?> { throw IOException("offline") }
            coEvery { repository.getHeroMovie(MovieCategory.POPULAR) } returns failedHero andThen flowOf(hero)
            val heroViewModel = MovieListViewModel(repository, libraryRepository)
            val job = launch { heroViewModel.heroMovieState.collect() }
            advanceUntilIdle()

            assertEquals(
                HeroMovieUiState.Error(HeroMovieErrorCode.LOAD_FEATURED_MOVIE),
                heroViewModel.heroMovieState.value,
            )

            heroViewModel.retryHero()
            advanceUntilIdle()

            assertEquals(HeroMovieUiState.Success(hero), heroViewModel.heroMovieState.value)
            coVerify(exactly = 2) { repository.getHeroMovie(MovieCategory.POPULAR) }

            job.cancel()
        }

    @Test
    fun `favorite failure stays pending until acknowledged and repeated failures coalesce`() =
        runTest {
            val target = movie()
            coEvery { libraryRepository.toggleFavorite(target) } throws IOException("offline")

            viewModel.onToggleFavorite(target)
            advanceUntilIdle()
            assertEquals(
                FavoriteActionErrorState.Failure(FavoriteActionErrorCode.UPDATE_FAVORITE),
                viewModel.favoriteActionError.value,
            )

            // A second failure before acknowledgement must not create a second message.
            viewModel.onToggleFavorite(target)
            advanceUntilIdle()
            assertEquals(
                FavoriteActionErrorState.Failure(FavoriteActionErrorCode.UPDATE_FAVORITE),
                viewModel.favoriteActionError.value,
            )

            viewModel.acknowledgeFavoriteError()
            assertEquals(FavoriteActionErrorState.None, viewModel.favoriteActionError.value)
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
            genreIds = persistentListOf(),
            genres = persistentListOf(),
        )
}
