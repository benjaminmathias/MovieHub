package com.benjamin.moviehub.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieDatabaseMigrationTest {
    private val dbName = "movie-hub-migration-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate1To2_preservesMovieAndMigratesPopularMembership() =
        runBlocking {
            createVersionOneDatabase(includeRuntimeColumn = true)

            val database = openMigratedDatabase()
            try {
                val dao = database.movieDao()

                val favorite = dao.getMovieById(1)
                assertNotNull(favorite)
                assertEquals(true, favorite?.isFavorite)
                assertEquals(120, favorite?.runtimeMinutes)

                // Legacy popular order is preserved in the association table.
                assertEquals(listOf(3, 1), dao.getCategoryMovieIds("POPULAR"))
                assertEquals(1, dao.getRemoteKeysCountByType("POPULAR"))

                // Existing search associations survive the movies table rebuild.
                assertEquals(listOf(1), dao.getSearchResultMovieIds("matrix"))
            } finally {
                database.close()
            }
        }

    @Test
    fun migrate1To2_withoutRuntimeColumn_opensAndPreservesData() =
        runBlocking {
            createVersionOneDatabase(includeRuntimeColumn = false)

            val database = openMigratedDatabase()
            try {
                val dao = database.movieDao()

                val favorite = dao.getMovieById(1)
                assertNotNull(favorite)
                assertEquals(true, favorite?.isFavorite)
                // The legacy column never existed, so runtime stays null rather than being invented.
                assertEquals(null, favorite?.runtimeMinutes)

                assertEquals(listOf(3, 1), dao.getCategoryMovieIds("POPULAR"))
                assertEquals(1, dao.getRemoteKeysCountByType("POPULAR"))
                assertEquals(listOf(1), dao.getSearchResultMovieIds("matrix"))
            } finally {
                database.close()
            }
        }

    private fun openMigratedDatabase(): MovieDatabase =
        Room
            .databaseBuilder(context, MovieDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .build()

    private fun createVersionOneDatabase(includeRuntimeColumn: Boolean) {
        val runtimeColumnDefinition = if (includeRuntimeColumn) ", `runtimeMinutes` INTEGER" else ""
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(1) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    "CREATE TABLE IF NOT EXISTS `movies` (" +
                                        "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                                        "`overview` TEXT NOT NULL, `posterPath` TEXT, `backdropPath` TEXT, " +
                                        "`voteAverage` REAL NOT NULL, `releaseDate` TEXT NOT NULL, " +
                                        "`genreIds` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, " +
                                        "`isPopular` INTEGER NOT NULL, `isSearchResult` INTEGER NOT NULL, " +
                                        "`pageOrder` INTEGER NOT NULL$runtimeColumnDefinition, " +
                                        "PRIMARY KEY(`id`))",
                                )
                                db.execSQL(
                                    "CREATE TABLE IF NOT EXISTS `remote_keys` (" +
                                        "`movieId` INTEGER NOT NULL, `prevKey` INTEGER, `nextKey` INTEGER, " +
                                        "`type` TEXT NOT NULL, PRIMARY KEY(`movieId`, `type`))",
                                )
                                db.execSQL(
                                    "CREATE TABLE IF NOT EXISTS `movie_search_results` (" +
                                        "`queryKey` TEXT NOT NULL, `movieId` INTEGER NOT NULL, " +
                                        "`pageOrder` INTEGER NOT NULL, PRIMARY KEY(`queryKey`, `movieId`))",
                                )
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    ).build(),
            )

        helper.writableDatabase.use { db ->
            val runtimeColumn = if (includeRuntimeColumn) ",`runtimeMinutes`" else ""

            fun insertMovie(
                id: Int,
                title: String,
                isFavorite: Int,
                isPopular: Int,
                pageOrder: Int,
                runtimeMinutes: Int?,
            ) {
                val runtimeValue = if (includeRuntimeColumn) ",${runtimeMinutes ?: "NULL"}" else ""
                db.execSQL(
                    "INSERT INTO `movies` (`id`,`title`,`overview`,`posterPath`,`backdropPath`,`voteAverage`," +
                        "`releaseDate`,`genreIds`,`isFavorite`,`isPopular`,`isSearchResult`,`pageOrder`$runtimeColumn) " +
                        "VALUES ($id,'$title','o',NULL,NULL,7.0,'2020-01-01','28',$isFavorite,$isPopular,0,$pageOrder$runtimeValue)",
                )
            }

            insertMovie(id = 1, title = "Popular Favorite", isFavorite = 1, isPopular = 1, pageOrder = 3, runtimeMinutes = 120)
            insertMovie(id = 2, title = "Plain", isFavorite = 0, isPopular = 0, pageOrder = -1, runtimeMinutes = null)
            // A second popular movie with a lower pageOrder proves ordering is carried over.
            insertMovie(id = 3, title = "Another Popular", isFavorite = 0, isPopular = 1, pageOrder = 1, runtimeMinutes = null)

            db.execSQL(
                "INSERT INTO `remote_keys` (`movieId`,`prevKey`,`nextKey`,`type`) VALUES (1,NULL,2,'POPULAR')",
            )
            db.execSQL(
                "INSERT INTO `movie_search_results` (`queryKey`,`movieId`,`pageOrder`) VALUES ('matrix',1,0)",
            )
        }
        helper.close()
    }
}
