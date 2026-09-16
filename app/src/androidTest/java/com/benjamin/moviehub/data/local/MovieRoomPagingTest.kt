package com.benjamin.moviehub.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.RemoteMediator
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.data.paging.MovieRemoteMediator
import com.benjamin.moviehub.data.paging.SearchMovieRemoteMediator
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import com.benjamin.moviehub.data.remote.movieDto
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.emptyPagingState
import com.benjamin.moviehub.inMemoryDatabase
import com.benjamin.moviehub.movieEntity
import com.benjamin.moviehub.pagingState
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
        database = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun remoteKey_isStoredIndependentlyPerFeed() =
        runBlocking {
            val dao = database.movieDao()

            dao.upsertRemoteKey(RemoteKey(MovieCategory.POPULAR.key, nextKey = 2))
            dao.upsertRemoteKey(RemoteKey(MovieCategory.UPCOMING.key, nextKey = null))

            assertEquals(2, dao.getRemoteKey(MovieCategory.POPULAR.key)?.nextKey)
            assertEquals(null, dao.getRemoteKey(MovieCategory.UPCOMING.key)?.nextKey)
            assertEquals(null, dao.getRemoteKey("SEARCH:test"))

            dao.upsertRemoteKey(RemoteKey(MovieCategory.POPULAR.key, nextKey = 5))
            assertEquals(5, dao.getRemoteKey(MovieCategory.POPULAR.key)?.nextKey)
        }

    @Test
    fun setFavorite_insertsThenUpdatesMovie() =
        runBlocking {
            val dao = database.movieDao()
            val movie = movieEntity(id = 42)

            dao.setLibraryFlag(movie, isFavorite = true)
            assertEquals(true, dao.getMovieById(42)?.isFavorite)

            dao.setLibraryFlag(movie, isFavorite = false)
            assertEquals(false, dao.getMovieById(42)?.isFavorite)
        }

    @Test
    fun libraryFlags_areIndependent() =
        runBlocking {
            val dao = database.movieDao()
            val movie = movieEntity(id = 43)
            dao.setLibraryFlag(movie, isFavorite = true)
            dao.setLibraryFlag(movie, isWatchlist = true)
            assertTrue(dao.getMovieById(43)?.isWatchlist == true)
            assertTrue(dao.getMovieById(43)?.isWatched == false)
            dao.setLibraryFlag(movie, isWatched = true)
            assertTrue(dao.getMovieById(43)?.let { it.isFavorite && !it.isWatchlist && it.isWatched } == true)
            dao.setLibraryFlag(movie, isWatchlist = false)
            assertTrue(dao.getMovieById(43)?.let { it.isFavorite && !it.isWatchlist && it.isWatched } == true)
            dao.setLibraryFlag(movie, isWatched = false)
            assertTrue(dao.getMovieById(43)?.let { it.isFavorite && !it.isWatchlist && !it.isWatched } == true)
        }

    @Test
    fun upsertMovieDetails_preservesAllLocalFlagsAndRuntime() =
        runBlocking {
            val dao = database.movieDao()
            dao.insertMovie(
                movieEntity(
                    id = 42,
                    isFavorite = true,
                    isWatchlist = true,
                    isWatched = true,
                    runtimeMinutes = 137,
                ),
            )

            val refreshed = movieEntity(id = 42).copy(title = "Refreshed", runtimeMinutes = null)

            val merged = dao.upsertMovieDetails(refreshed)

            assertEquals("Refreshed", merged.title)
            assertEquals(true, merged.isFavorite)
            assertEquals(false, merged.isWatchlist)
            assertEquals(true, merged.isWatched)
            assertEquals(137, merged.runtimeMinutes)
            assertEquals(merged, dao.getMovieById(42))
        }

    @Test
    fun refreshingSearch_keepsOnlyCurrentQueryCacheAndReferencedMovies() =
        runBlocking {
            val dao = database.movieDao()
            dao.upsertMovies(listOf(movieEntity(200, isFavorite = true), movieEntity(300)))
            dao.insertCategoryMovies(
                listOf(MovieCategoryEntity(movieId = 300, category = MovieCategory.POPULAR.key, pageOrder = 0)),
            )
            val api =
                FakeMovieApiService(
                    popularPages = emptyMap(),
                    searchPages =
                        mapOf(
                            "alpha" to mapOf(1 to listOf(movieDto(100), movieDto(200), movieDto(300))),
                            "beta" to mapOf(1 to listOf(movieDto(400))),
                        ),
                )

            SearchMovieRemoteMediator(api, database, " alpha ").load(LoadType.REFRESH, emptyPagingState())
            SearchMovieRemoteMediator(api, database, "beta").load(LoadType.REFRESH, emptyPagingState())

            assertEquals(emptyList<Int>(), dao.getSearchResultMovieIds("alpha"))
            assertEquals(listOf(400), dao.getSearchResultMovieIds("beta"))
            assertEquals(null, dao.getMovieById(100))
            assertEquals(true, dao.getMovieById(200)?.isFavorite)
            assertEquals(300, dao.getMovieById(300)?.id)
            assertEquals(null, dao.getRemoteKey("SEARCH:alpha"))
            assertEquals(2, dao.getRemoteKey("SEARCH:beta")?.nextKey)
        }

    @Test
    fun refreshingSearch_dropsRemovedMoviesButKeepsFavoritesAndCategoryCache() =
        runBlocking {
            val dao = database.movieDao()
            dao.upsertMovies(
                listOf(
                    movieEntity(100, isWatchlist = true),
                    movieEntity(101, isWatched = true),
                    movieEntity(200, isFavorite = true),
                    movieEntity(300),
                ),
            )
            dao.insertSearchResults(
                listOf(
                    MovieSearchResultEntity(queryKey = "alpha", movieId = 100, pageOrder = 0),
                    MovieSearchResultEntity(queryKey = "alpha", movieId = 101, pageOrder = 1),
                    MovieSearchResultEntity(queryKey = "alpha", movieId = 200, pageOrder = 2),
                ),
            )
            dao.insertCategoryMovies(
                listOf(MovieCategoryEntity(movieId = 300, category = MovieCategory.POPULAR.key, pageOrder = 0)),
            )

            val api = FakeMovieApiService(searchPages = mapOf("alpha" to mapOf(1 to listOf(movieDto(200)))))
            SearchMovieRemoteMediator(api, database, "alpha").load(LoadType.REFRESH, emptyPagingState())

            assertEquals(true, dao.getMovieById(100)?.isWatchlist)
            assertEquals(true, dao.getMovieById(101)?.isWatched)
            assertEquals(true, dao.getMovieById(200)?.isFavorite)
            assertEquals(300, dao.getMovieById(300)?.id)
            assertEquals(listOf(200), dao.getSearchResultMovieIds("alpha"))
        }

    @Test
    fun categoryRefresh_clearsOnlyItsKeysAndPreservesFavorite() =
        runBlocking {
            val dao = database.movieDao()
            dao.insertMovie(movieEntity(id = 1, isFavorite = true, isWatchlist = true, isWatched = true, runtimeMinutes = 137))
            dao.upsertRemoteKey(RemoteKey(MovieCategory.POPULAR.key, nextKey = 9))

            val mediator = MovieRemoteMediator(FakeMovieApiService(mapOf(1 to listOf(movieDto(1)))), database, MovieCategory.POPULAR)
            val result = mediator.load(LoadType.REFRESH, emptyPagingState())

            assertTrue(result is RemoteMediator.MediatorResult.Success)
            assertEquals(2, dao.getRemoteKey(MovieCategory.POPULAR.key)?.nextKey)
            assertEquals(true, dao.getMovieById(1)?.isFavorite)
            assertEquals(false, dao.getMovieById(1)?.isWatchlist)
            assertEquals(true, dao.getMovieById(1)?.isWatched)
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
            val result = mediator.load(LoadType.APPEND, pagingState(movieEntity(10)))

            assertTrue(result is RemoteMediator.MediatorResult.Success)
            assertEquals(listOf(1, 2), api.nowPlayingPagesRequested)
            assertEquals(emptyList<Int>(), api.popularPagesRequested)
            assertEquals(3, database.movieDao().getRemoteKey(MovieCategory.NOW_PLAYING.key)?.nextKey)
        }

    @Test
    fun searchPagination_loadsNextPage() =
        runBlocking {
            val api = FakeMovieApiService(mapOf(1 to listOf(movieDto(10)), 2 to listOf(movieDto(11))))
            val mediator = SearchMovieRemoteMediator(api, database, "test")

            mediator.load(LoadType.REFRESH, emptyPagingState())
            val result = mediator.load(LoadType.APPEND, pagingState(movieEntity(10)))

            assertTrue(result is RemoteMediator.MediatorResult.Success)
            assertEquals(listOf(1, 2), api.searchPagesRequested)
            assertEquals(3, database.movieDao().getRemoteKey("SEARCH:test")?.nextKey)
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
                dao
                    .getCategoryMoviesPaging(MovieCategory.POPULAR.key)
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
            assertEquals(2, dao.getRemoteKey(MovieCategory.UPCOMING.key)?.nextKey)
        }
}
