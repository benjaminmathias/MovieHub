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
            createVersionOneDatabase()

            val database =
                Room
                    .databaseBuilder(context, MovieDatabase::class.java, dbName)
                    .addMigrations(MIGRATION_1_2)
                    .build()

            try {
                val dao = database.movieDao()

                val favorite = dao.getMovieById(1)
                assertNotNull(favorite)
                assertEquals(true, favorite?.isFavorite)
                assertEquals(120, favorite?.runtimeMinutes)

                // Legacy popular membership and order are preserved in the association table.
                assertEquals(listOf(1), dao.getCategoryMovieIds("POPULAR"))
                assertEquals(1, dao.getRemoteKeysCountByType("POPULAR"))
            } finally {
                database.close()
            }
        }

    private fun createVersionOneDatabase() {
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
                                        "`pageOrder` INTEGER NOT NULL, `runtimeMinutes` INTEGER, " +
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
            db.execSQL(
                "INSERT INTO `movies` (`id`,`title`,`overview`,`posterPath`,`backdropPath`,`voteAverage`," +
                    "`releaseDate`,`genreIds`,`isFavorite`,`isPopular`,`isSearchResult`,`pageOrder`,`runtimeMinutes`) " +
                    "VALUES (1,'Popular Favorite','o',NULL,NULL,7.0,'2020-01-01','28',1,1,0,3,120)",
            )
            db.execSQL(
                "INSERT INTO `movies` (`id`,`title`,`overview`,`posterPath`,`backdropPath`,`voteAverage`," +
                    "`releaseDate`,`genreIds`,`isFavorite`,`isPopular`,`isSearchResult`,`pageOrder`,`runtimeMinutes`) " +
                    "VALUES (2,'Plain','o',NULL,NULL,6.0,'2021-01-01','',0,0,0,-1,NULL)",
            )
            db.execSQL(
                "INSERT INTO `remote_keys` (`movieId`,`prevKey`,`nextKey`,`type`) VALUES (1,NULL,2,'POPULAR')",
            )
        }
        helper.close()
    }
}
