package com.benjamin.moviehub.data.local

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.RemoteMediator
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.data.paging.MovieRemoteMediator
import com.benjamin.moviehub.data.paging.SearchMovieRemoteMediator
import com.benjamin.moviehub.data.remote.FakeMovieApiService
import com.benjamin.moviehub.data.remote.movieDto
import com.benjamin.moviehub.domain.model.MovieCategory
import com.benjamin.moviehub.emptyPagingState
import com.benjamin.moviehub.inMemoryDatabase
import com.benjamin.moviehub.insertMalformedMovieForTest
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
class MovieRemoteMediatorTest {
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
    fun refreshingSearch_withMoreThanSqliteVariables_deletesEveryOrphan() =
        runBlocking {
            val dao = database.movieDao()
            val orphanCount = 1_200
            val orphans = (1..orphanCount).map { index -> movieEntity(id = 10_000 + index) }
            orphans.chunked(500).forEach { batch -> dao.upsertMovies(batch) }
            dao.insertSearchResults(
                orphans.mapIndexed { index, entity ->
                    MovieSearchResultEntity(queryKey = "big", movieId = entity.id, pageOrder = index)
                },
            )

            val api = FakeMovieApiService(searchPages = mapOf("big" to mapOf(1 to listOf(movieDto(500_000)))))
            SearchMovieRemoteMediator(api, database, "big").load(LoadType.REFRESH, emptyPagingState())

            assertEquals(listOf(500_000), dao.getSearchResultMovieIds("big"))
            val remainingOrphans = mutableListOf<MovieEntity>()
            for (idBatch in orphans.map { it.id }.chunked(500)) {
                remainingOrphans += dao.getMoviesByIds(idBatch)
            }
            assertTrue(remainingOrphans.isEmpty())
            assertEquals(500_000, dao.getMovieById(500_000)?.id)
        }

    @Test
    fun categoryRefresh_clearsOnlyItsKeysAndPreservesFavorite() =
        runBlocking {
            val dao = database.movieDao()
            insertMalformedMovieForTest(
                database,
                movieEntity(id = 1, isFavorite = true, isWatchlist = true, isWatched = true, runtimeMinutes = 137),
            )
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
}
