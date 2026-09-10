package com.benjamin.moviehub.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.data.paging.MovieRemoteMediator
import com.benjamin.moviehub.data.paging.SearchMovieRemoteMediator
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import com.benjamin.moviehub.data.remote.movieDto
import com.benjamin.moviehub.domain.model.MovieCategory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
    fun remoteKeys_allowSameMovieIdForDifferentCategories() =
        runBlocking {
            val dao = database.movieDao()

            dao.insertAllKeys(
                listOf(
                    MovieRemoteKey(42, null, 2, MovieCategory.POPULAR.key),
                    MovieRemoteKey(42, null, 3, MovieCategory.UPCOMING.key),
                    MovieRemoteKey(42, null, 4, "SEARCH:test"),
                ),
            )

            assertEquals(2, dao.getRemoteKeysForMovieId(42, MovieCategory.POPULAR.key)?.nextKey)
            assertEquals(3, dao.getRemoteKeysForMovieId(42, MovieCategory.UPCOMING.key)?.nextKey)
            assertEquals(4, dao.getRemoteKeysForMovieId(42, "SEARCH:test")?.nextKey)
            assertEquals(1, dao.getRemoteKeysCountByType(MovieCategory.POPULAR.key))
            assertEquals(1, dao.getRemoteKeysCountByType(MovieCategory.UPCOMING.key))
        }

    @Test
    fun setFavorite_insertsThenUpdatesMovie() =
        runBlocking {
            val dao = database.movieDao()
            val movie = movieEntity(id = 42)

            dao.setFavorite(movie, true)
            assertEquals(true, dao.getMovieById(42)?.isFavorite)

            dao.setFavorite(movie, false)
            assertEquals(false, dao.getMovieById(42)?.isFavorite)
        }

    @Test
    fun refreshingSearch_onlyReplacesCurrentQueryCache() =
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
    fun categoryRefresh_clearsOnlyItsKeysAndPreservesFavorite() =
        runBlocking {
            val dao = database.movieDao()
            dao.insertMovie(movieEntity(id = 1, isFavorite = true, runtimeMinutes = 137))
            dao.insertAllKeys(
                listOf(
                    MovieRemoteKey(1, null, 2, MovieCategory.POPULAR.key),
                    MovieRemoteKey(99, 1, 3, MovieCategory.POPULAR.key),
                ),
            )

            val mediator =
                MovieRemoteMediator(
                    FakeMovieApiService(mapOf(1 to listOf(movieDto(1)))),
                    database,
                    MovieCategory.POPULAR,
                )
            val result = mediator.load(LoadType.REFRESH, emptyPagingState())

            assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
            assertEquals(null, dao.getRemoteKeysForMovieId(99, MovieCategory.POPULAR.key))
            assertEquals(true, dao.getMovieById(1)?.isFavorite)
            assertEquals(137, dao.getMovieById(1)?.runtimeMinutes)
            assertEquals(listOf(1), dao.getCategoryMovieIds(MovieCategory.POPULAR.key))
        }

    @Test
    fun categoryPagination_loadsNextPageForItsCategoryOnly() =
        runBlocking {
            val api =
                FakeMovieApiService(
                    popularPages = mapOf(1 to listOf(movieDto(1)), 2 to listOf(movieDto(2))),
                    nowPlayingPages = mapOf(1 to listOf(movieDto(10)), 2 to listOf(movieDto(11))),
                )
            val mediator = MovieRemoteMediator(api, database, MovieCategory.NOW_PLAYING)

            mediator.load(LoadType.REFRESH, emptyPagingState())
            val state = pagingState(movieEntity(10))
            val result = mediator.load(LoadType.APPEND, state)

            assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
            assertEquals(listOf(1, 2), api.nowPlayingPagesRequested)
            assertEquals(emptyList<Int>(), api.popularPagesRequested)
            assertEquals(2, database.movieDao().getRemoteKeysCountByType(MovieCategory.NOW_PLAYING.key))
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

    @Test
    fun categoryMembership_isIndependentAndOrdered() =
        runBlocking {
            val dao = database.movieDao()
            dao.upsertMovies(listOf(movieEntity(1), movieEntity(2), movieEntity(3)))
            dao.insertCategoryMovies(
                listOf(
                    MovieCategoryEntity(movieId = 2, category = MovieCategory.POPULAR.key, pageOrder = 0),
                    MovieCategoryEntity(movieId = 1, category = MovieCategory.POPULAR.key, pageOrder = 1),
                    MovieCategoryEntity(movieId = 3, category = MovieCategory.UPCOMING.key, pageOrder = 0),
                ),
            )

            assertEquals(listOf(2, 1), dao.getCategoryMovieIds(MovieCategory.POPULAR.key))
            assertEquals(listOf(3), dao.getCategoryMovieIds(MovieCategory.UPCOMING.key))

            val page =
                dao.getCategoryMoviesPaging(MovieCategory.POPULAR.key)
                    .load(LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)) as LoadResult.Page
            assertEquals(listOf(2, 1), page.data.map { it.id })
        }

    @Test
    fun movieCanBelongToSeveralCategoriesWithDifferentOrder() =
        runBlocking {
            val dao = database.movieDao()
            dao.upsertMovies(listOf(movieEntity(7), movieEntity(8)))
            dao.insertCategoryMovies(
                listOf(
                    MovieCategoryEntity(movieId = 7, category = MovieCategory.POPULAR.key, pageOrder = 0),
                    MovieCategoryEntity(movieId = 8, category = MovieCategory.POPULAR.key, pageOrder = 1),
                    MovieCategoryEntity(movieId = 8, category = MovieCategory.TOP_RATED.key, pageOrder = 0),
                    MovieCategoryEntity(movieId = 7, category = MovieCategory.TOP_RATED.key, pageOrder = 1),
                ),
            )

            assertEquals(listOf(7, 8), dao.getCategoryMovieIds(MovieCategory.POPULAR.key))
            assertEquals(listOf(8, 7), dao.getCategoryMovieIds(MovieCategory.TOP_RATED.key))
        }

    @Test
    fun refreshingOneCategory_doesNotClearAnother() =
        runBlocking {
            val dao = database.movieDao()
            val api =
                FakeMovieApiService(
                    popularPages = mapOf(1 to listOf(movieDto(1))),
                    upcomingPages = mapOf(1 to listOf(movieDto(50))),
                )

            MovieRemoteMediator(api, database, MovieCategory.UPCOMING).load(LoadType.REFRESH, emptyPagingState())
            MovieRemoteMediator(api, database, MovieCategory.POPULAR).load(LoadType.REFRESH, emptyPagingState())
            MovieRemoteMediator(api, database, MovieCategory.POPULAR).load(LoadType.REFRESH, emptyPagingState())

            assertEquals(listOf(50), dao.getCategoryMovieIds(MovieCategory.UPCOMING.key))
            assertEquals(listOf(1), dao.getCategoryMovieIds(MovieCategory.POPULAR.key))
            assertEquals(1, dao.getRemoteKeysCountByType(MovieCategory.UPCOMING.key))
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
        isSearchResult: Boolean = false,
        runtimeMinutes: Int? = null,
    ) = MovieEntity(
        id = id,
        title = "Movie $id",
        overview = "Overview",
        posterPath = null,
        backdropPath = null,
        voteAverage = 7.0,
        releaseDate = "2020-01-01",
        isFavorite = isFavorite,
        isSearchResult = isSearchResult,
        runtimeMinutes = runtimeMinutes,
    )
}
