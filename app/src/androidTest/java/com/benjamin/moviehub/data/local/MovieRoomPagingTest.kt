package com.benjamin.moviehub.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.data.paging.MovieRemoteMediator
import com.benjamin.moviehub.data.paging.SearchMovieRemoteMediator
import com.benjamin.moviehub.data.remote.ActorDto
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.remote.MovieCreditsDto
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.data.remote.MovieResponse
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private fun movieDto(id: Int) =
    MovieDto(
        id = id,
        title = "Movie $id",
        description = "Overview",
        posterPath = null,
        backdropPath = null,
        voteAverage = 7.0,
        releaseDate = "2020-01-01",
        genreIds = emptyList(),
    )

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalPagingApi::class)
class MovieRoomPagingTest {
    private lateinit var database: MovieDatabase

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    MovieDatabase::class.java,
                ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun remoteKeys_allowSameMovieIdForPopularAndSearch() =
        runBlocking {
            val dao = database.movieDao()

            dao.insertAllKeys(
                listOf(
                    MovieRemoteKey(42, null, 2, "POPULAR"),
                    MovieRemoteKey(42, null, 3, "SEARCH"),
                ),
            )

            assertEquals(2, dao.getRemoteKeysCountByType("POPULAR") + dao.getRemoteKeysCountByType("SEARCH"))
            assertEquals(2, dao.getRemoteKeysForMovieId(42, "POPULAR")?.nextKey)
            assertEquals(3, dao.getRemoteKeysForMovieId(42, "SEARCH")?.nextKey)
        }

    @Test
    fun differentSearchQueries_doNotShareCachedResults() =
        runBlocking {
            val api =
                FakeMovieApiService(
                    popularPages = emptyMap(),
                    searchPages =
                        mapOf(
                            "alpha" to mapOf(1 to listOf(movieDto(100))),
                            "beta" to mapOf(1 to listOf(movieDto(200))),
                        ),
                )

            SearchMovieRemoteMediator(api, database, " alpha ").load(LoadType.REFRESH, emptyPagingState())
            SearchMovieRemoteMediator(api, database, "beta").load(LoadType.REFRESH, emptyPagingState())

            assertEquals(listOf(100), database.movieDao().getSearchResultMovieIds("alpha"))
            assertEquals(listOf(200), database.movieDao().getSearchResultMovieIds("beta"))
        }

    @Test
    fun popularRefresh_clearsObsoleteKeysAndPreservesFavorite() =
        runBlocking {
            val dao = database.movieDao()
            dao.insertMovie(movieEntity(id = 1, isFavorite = true, isPopular = true))
            dao.insertAllKeys(
                listOf(
                    MovieRemoteKey(1, null, 2, "POPULAR"),
                    MovieRemoteKey(99, 1, 3, "POPULAR"),
                ),
            )

            val mediator = MovieRemoteMediator(FakeMovieApiService(mapOf(1 to listOf(movieDto(1)))), database)
            val result = mediator.load(LoadType.REFRESH, emptyPagingState())

            assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
            assertEquals(null, dao.getRemoteKeysForMovieId(99, "POPULAR"))
            assertEquals(true, dao.getMovieById(1)?.isFavorite)
            assertEquals(true, dao.getMovieById(1)?.isPopular)
        }

    @Test
    fun popularPagination_loadsNextPage() =
        runBlocking {
            val api = FakeMovieApiService(mapOf(1 to listOf(movieDto(1)), 2 to listOf(movieDto(2))))
            val mediator = MovieRemoteMediator(api, database)

            mediator.load(LoadType.REFRESH, emptyPagingState())
            val state = pagingState(movieEntity(1, isPopular = true))
            val result = mediator.load(LoadType.APPEND, state)

            assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
            assertEquals(listOf(1, 2), api.popularPagesRequested)
            assertEquals(2, database.movieDao().getRemoteKeysCountByType("POPULAR"))
        }

    @Test
    fun searchPagination_loadsNextPage() =
        runBlocking {
            val api = FakeMovieApiService(mapOf(1 to listOf(movieDto(10)), 2 to listOf(movieDto(11))))
            val mediator = SearchMovieRemoteMediator(api, database, "test")

            mediator.load(LoadType.REFRESH, emptyPagingState())
            val state = pagingState(movieEntity(10, isSearchResult = true))
            val result = mediator.load(LoadType.APPEND, state)

            assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
            assertEquals(listOf(1, 2), api.searchPagesRequested)
            assertEquals(2, database.movieDao().getRemoteKeysCountByType("SEARCH:test"))
        }

    private fun emptyPagingState(): PagingState<Int, MovieEntity> = PagingState(emptyList(), null, PagingConfig(pageSize = 1), 0)

    private fun pagingState(movie: MovieEntity): PagingState<Int, MovieEntity> =
        PagingState(
            pages = listOf(LoadResult.Page(data = listOf(movie), prevKey = null, nextKey = 2)),
            anchorPosition = null,
            config = PagingConfig(pageSize = 1),
            leadingPlaceholderCount = 0,
        )

    private fun movieEntity(
        id: Int,
        isFavorite: Boolean = false,
        isPopular: Boolean = false,
        isSearchResult: Boolean = false,
    ) = MovieEntity(
        id = id,
        title = "Movie $id",
        overview = "Overview",
        posterPath = null,
        backdropPath = null,
        voteAverage = 7.0,
        releaseDate = "2020-01-01",
        isFavorite = isFavorite,
        isPopular = isPopular,
        isSearchResult = isSearchResult,
    )

    private class FakeMovieApiService(
        private val popularPages: Map<Int, List<MovieDto>>,
        private val searchPages: Map<String, Map<Int, List<MovieDto>>> = emptyMap(),
    ) : MovieApiService {
        val popularPagesRequested = mutableListOf<Int>()
        val searchPagesRequested = mutableListOf<Int>()

        override suspend fun getPopularMovies(
            apiKey: String,
            language: String,
            page: Int,
        ): MovieResponse {
            popularPagesRequested += page
            return MovieResponse(popularPages[page].orEmpty())
        }

        override suspend fun searchMovies(
            apiKey: String,
            query: String,
            language: String,
            page: Int,
        ): MovieResponse {
            searchPagesRequested += page
            val requestedPages = searchPages[query.trim().lowercase()] ?: popularPages
            return MovieResponse(requestedPages[page].orEmpty())
        }

        override suspend fun getMovieDetails(
            movieId: Int,
            apiKey: String,
            language: String,
        ): MovieDto = movieDto(movieId)

        override suspend fun getMovieCredits(
            movieId: Int,
            apiKey: String,
            language: String,
        ): MovieCreditsDto = MovieCreditsDto(emptyList<ActorDto>())
    }
}
