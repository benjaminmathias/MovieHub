package com.benjamin.moviehub.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MovieDaoTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: TestMovieDatabase

    @Inject
    lateinit var dao: MovieDao

    @Before
    fun init() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndFilterPopularMovies() = runTest {
        // Given
        val popularMovie = MovieTestData.popularMovie
        val searchMovie = MovieTestData.searchMovie

        // When
        dao.insertMovies(listOf(popularMovie, searchMovie))

        // Then
        dao.getPopularMovies().test {
            val list = awaitItem()
            assert(list.size == 1)
            assert(list[0].id == popularMovie.id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}