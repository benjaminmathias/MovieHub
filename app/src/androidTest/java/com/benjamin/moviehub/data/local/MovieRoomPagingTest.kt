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

            dao.setFavorite(movie, true)
            assertEquals(true, dao.getMovieById(42)?.isFavorite)

            dao.setFavorite(movie, false)
            assertEquals(false, dao.getMovieById(42)?.isFavorite)
        }

    @Test
    fun upsertMovieDetails_preservesLocalFavoriteAndRuntime() =
        runBlocking {
            val dao = database.movieDao()
            dao.insertMovie(
                movieEntity(
                    id = 42,
                    isFavorite = true,
                    runtimeMinutes = 137,
                ),
            )

            val refreshed = movieEntity(id = 42).copy(title = "Refreshed", runtimeMinutes = null)

            val merged = dao.upsertMovieDetails(refreshed)

            assertEquals("Refreshed", merged.title)
            assertEquals(true, merged.isFavorite)
            assertEquals(137, merged.runtimeMinutes)
            assertEquals(merged, dao.getMovieById(42))
        }

    @Test
    fun refreshingSearch_keepsOnlyCurrentQueryCacheAndReferencedMovies() =
        runBlocking {
            val dao = database.movieDao()
            dao.upsertMovies(
                listOf(
                    movieEntity(200, isFavorite = true),
                    movieEntity(300),
                ),
            )
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
                    movieEntity(100),
                    movieEntity(200, isFavorite = true),
                    movieEntity(300),
                ),
            )
            dao.insertSearchResults(
                listOf(
                    MovieSearchResultEntity(queryKey = "alpha", movieId = 100, pageOrder = 0),
                    MovieSearchResultEntity(queryKey = "alpha", movieId = 200, pageOrder = 1),
                ),
            )
            dao.insertCategoryMovies(
                listOf(MovieCategoryEntity(movieId = 300, category = MovieCategory.POPULAR.key, pageOrder = 0)),
            )

            val api = FakeMovieApiService(searchPages = mapOf("alpha" to mapOf(1 to listOf(movieDto(200)))))
            SearchMovieRemoteMediator(api, database, "alpha").load(LoadType.REFRESH, emptyPagingState())

            assertEquals(null, dao.getMovieById(100))
            assertEquals(true, dao.getMovieById(200)?.isFavorite)
            assertEquals(300, dao.getMovieById(300)?.id)
            assertEquals(listOf(200), dao.getSearchResultMovieIds("alpha"))
        }

    @Test
    fun categoryRefresh_clearsOnlyItsKeysAndPreservesFavorite() =
        runBlocking {
            val dao = database.movieDao()
            dao.insertMovie(movieEntity(id = 1, isFavorite = true, runtimeMinutes = 137))
            dao.upsertRemoteKey(RemoteKey(MovieCategory.POPULAR.key, nextKey = 9))

            val mediator =
                MovieRemoteMediator(
                    FakeMovieApiService(mapOf(1 to listOf(movieDto(1)))),
                    database,
                    MovieCategory.POPULAR,
                )
            val result = mediator.load(LoadType.REFRESH, emptyPagingState())

            assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
            assertEquals(2, dao.getRemoteKey(MovieCategory.POPULAR.key)?.nextKey)
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
            assertEquals(3, database.movieDao().getRemoteKey(MovieCategory.NOW_PLAYING.key)?.nextKey)
        }

    @Test
    fun searchPagination_loadsNextPage() =
        runBlocking {
            val api = FakeMovieApiService(mapOf(1 to listOf(movieDto(10)), 2 to listOf(movieDto(11))))
            val mediator = SearchMovieRemoteMediator(api, database, "test")

            mediator.load(LoadType.REFRESH, emptyPagingState())
            val state = pagingState(movieEntity(10))
            val result = mediator.load(LoadType.APPEND, state)

            assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
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
            assertEquals(2, dao.getRemoteKey(MovieCategory.UPCOMING.key)?.nextKey)
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
        runtimeMinutes = runtimeMinutes,
    )
}
