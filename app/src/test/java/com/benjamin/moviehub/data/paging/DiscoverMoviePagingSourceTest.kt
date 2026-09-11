package com.benjamin.moviehub.data.paging

import androidx.paging.PagingSource
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.data.remote.MovieResponse
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverMoviePagingSourceTest {
    @Test
    fun `filters map to TMDB discover parameters`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            coEvery {
                apiService.discoverMovies(any(), any(), any(), any(), any())
            } returns MovieResponse(
                movies =
                    listOf(
                        MovieDto(
                            id = 1,
                            title = "Film",
                            posterPath = null,
                            backdropPath = null,
                            voteAverage = 7.0,
                        ),
                    ),
                totalPages = 1,
            )
            val source =
                DiscoverMoviePagingSource(
                    apiService = apiService,
                    filters =
                        DiscoverFilters(
                            genreId = 28,
                            releaseYear = 2020,
                            minimumVoteAverage = 7.0,
                            sort = DiscoverSortOption.RATING,
                        ),
                )

            val result = source.load(PagingSource.LoadParams.Refresh(null, 20, false))

            assertTrue(result is PagingSource.LoadResult.Page)
            coVerify {
                apiService.discoverMovies(
                    genreId = 28,
                    releaseYear = 2020,
                    minimumVoteAverage = 7.0,
                    sortBy = "vote_average.desc",
                    page = 1,
                )
            }
        }
}
