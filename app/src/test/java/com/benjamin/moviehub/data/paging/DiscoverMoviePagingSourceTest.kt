package com.benjamin.moviehub.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.data.remote.MovieResponse
import com.benjamin.moviehub.domain.model.DiscoverFilters
import com.benjamin.moviehub.domain.model.DiscoverSortOption
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverMoviePagingSourceTest {
    @Test
    fun `refresh without a key requests the first page`() =
        runTest {
            val apiService = mockApi(MovieResponse(movies = listOf(movieDto(1)), totalPages = 1))

            val result = source(apiService).load(refreshParams()) as PagingSource.LoadResult.Page

            assertEquals(listOf(1), result.data.map { it.id })
            coVerify { apiService.discoverMovies(null, null, null, null, "popularity.desc", 1) }
        }

    @Test
    fun `a non terminal page requests the next page`() =
        runTest {
            val apiService = mockApi(MovieResponse(movies = listOf(movieDto(1)), totalPages = 3))

            val result = source(apiService).load(appendParams(key = 1)) as PagingSource.LoadResult.Page

            assertEquals(2, result.nextKey)
        }

    @Test
    fun `a page at total pages has no next key`() =
        runTest {
            val apiService = mockApi(MovieResponse(movies = listOf(movieDto(3)), totalPages = 3))

            val result = source(apiService).load(appendParams(key = 3)) as PagingSource.LoadResult.Page

            assertEquals(null, result.nextKey)
        }

    @Test
    fun `duplicate ids across pages are dropped to keep grid keys unique`() =
        runTest {
            val apiService = mockApi(MovieResponse(movies = listOf(movieDto(1), movieDto(2)), totalPages = 3))
            val pagingSource = source(apiService)

            val first = pagingSource.load(refreshParams()) as PagingSource.LoadResult.Page
            val second = pagingSource.load(appendParams(key = 1)) as PagingSource.LoadResult.Page

            assertEquals(listOf(1, 2), first.data.map { it.id })
            assertEquals(emptyList<Int>(), second.data.map { it.id })
        }

    @Test
    fun `network errors become paging errors`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val error = IOException("offline")
            coEvery { apiService.discoverMovies(any(), any(), any(), any(), any(), any()) } throws error

            val result = source(apiService).load(refreshParams())

            assertTrue(result is PagingSource.LoadResult.Error)
            assertSame(error, (result as PagingSource.LoadResult.Error).throwable)
        }

    @Test
    fun `cancellation is propagated`() =
        runTest {
            val apiService = mockk<MovieApiService>()
            val cancellation = CancellationException("cancelled")
            coEvery { apiService.discoverMovies(any(), any(), any(), any(), any(), any()) } throws cancellation

            try {
                source(apiService).load(refreshParams())
                assertTrue("CancellationException was not propagated", false)
            } catch (actual: CancellationException) {
                assertSame(cancellation, actual)
            }
        }

    @Test
    fun `refresh key is derived from the closest page`() {
        val state =
            PagingState(
                pages =
                    listOf(
                        PagingSource.LoadResult.Page(
                            data = listOf(movieDto(1).toDomain()),
                            prevKey = 1,
                            nextKey = 3,
                        ),
                    ),
                anchorPosition = 0,
                config = PagingConfig(pageSize = 20),
                leadingPlaceholderCount = 0,
            )

        assertEquals(2, source(mockk()).getRefreshKey(state))
    }

    @Test
    fun `rating sort sends the minimum vote count`() =
        runTest {
            val apiService = mockApi(MovieResponse(totalPages = 1))
            val filters = DiscoverFilters(sort = DiscoverSortOption.RATING)

            source(apiService, filters).load(refreshParams())

            coVerify { apiService.discoverMovies(null, null, null, 200, "vote_average.desc", 1) }
        }

    @Test
    fun `popularity and release date sorts do not send a vote threshold`() =
        runTest {
            val apiService = mockApi(MovieResponse(totalPages = 1))

            source(apiService, DiscoverFilters(sort = DiscoverSortOption.POPULARITY)).load(refreshParams())
            source(apiService, DiscoverFilters(sort = DiscoverSortOption.RELEASE_DATE)).load(refreshParams())

            coVerify { apiService.discoverMovies(null, null, null, null, "popularity.desc", 1) }
            coVerify { apiService.discoverMovies(null, null, null, null, "primary_release_date.desc", 1) }
        }

    private fun source(
        apiService: MovieApiService,
        filters: DiscoverFilters = DiscoverFilters(),
    ) = DiscoverMoviePagingSource(apiService, filters)

    private fun mockApi(response: MovieResponse): MovieApiService {
        val apiService = mockk<MovieApiService>()
        coEvery { apiService.discoverMovies(any(), any(), any(), any(), any(), any()) } returns response
        return apiService
    }

    private fun refreshParams() = PagingSource.LoadParams.Refresh<Int>(null, 20, false)

    private fun appendParams(key: Int) = PagingSource.LoadParams.Append(key, 20, false)

    private fun movieDto(id: Int) =
        MovieDto(
            id = id,
            title = "Film $id",
            posterPath = null,
            backdropPath = null,
            voteAverage = 7.0,
        )
}
