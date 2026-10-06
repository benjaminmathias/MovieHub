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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

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
    fun `a non terminal page requests the next one and the last page ends pagination`() =
        runTest {
            val middleApi = mockApi(MovieResponse(movies = listOf(movieDto(1)), totalPages = 3))
            assertEquals(2, (source(middleApi).load(appendParams(key = 1)) as PagingSource.LoadResult.Page).nextKey)

            val lastApi = mockApi(MovieResponse(movies = listOf(movieDto(3)), totalPages = 3))
            assertEquals(null, (source(lastApi).load(appendParams(key = 3)) as PagingSource.LoadResult.Page).nextKey)
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
    fun `rating sort sends a minimum vote count, other sorts do not`() =
        runTest {
            val apiService = mockApi(MovieResponse(totalPages = 1))

            source(apiService, DiscoverFilters(sort = DiscoverSortOption.RATING)).load(refreshParams())
            source(apiService, DiscoverFilters(sort = DiscoverSortOption.POPULARITY)).load(refreshParams())
            source(apiService, DiscoverFilters(sort = DiscoverSortOption.RELEASE_DATE)).load(refreshParams())

            coVerify { apiService.discoverMovies(null, null, null, 200, "vote_average.desc", 1) }
            coVerify { apiService.discoverMovies(null, null, null, null, "popularity.desc", 1) }
            coVerify { apiService.discoverMovies(null, null, null, null, "primary_release_date.desc", 1) }
        }

    @Test
    fun `page 499 below an explicit larger total still appends`() =
        runTest {
            val apiService = mockApi(MovieResponse(movies = listOf(movieDto(499)), totalPages = 1000))

            val page = source(apiService).load(appendParams(key = 499)) as PagingSource.LoadResult.Page

            assertEquals(500, page.nextKey)
        }

    @Test
    fun `page 500 ends pagination even when the reported total exceeds the cap`() =
        runTest {
            val apiService = mockApi(MovieResponse(movies = listOf(movieDto(500)), totalPages = 1000))

            val page = source(apiService).load(appendParams(key = 500)) as PagingSource.LoadResult.Page

            assertEquals(null, page.nextKey)
        }

    @Test
    fun `an omitted total keeps a full page open and ends a short page`() =
        runTest {
            val fullApi = mockApi(MovieResponse(movies = List(20) { movieDto(it) }, totalPages = null))
            assertEquals(2, (source(fullApi).load(refreshParams()) as PagingSource.LoadResult.Page).nextKey)

            val shortApi = mockApi(MovieResponse(movies = listOf(movieDto(1)), totalPages = null))
            assertEquals(null, (source(shortApi).load(refreshParams()) as PagingSource.LoadResult.Page).nextKey)
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
