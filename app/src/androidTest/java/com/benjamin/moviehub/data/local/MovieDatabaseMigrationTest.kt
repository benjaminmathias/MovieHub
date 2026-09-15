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
import org.junit.Assert.assertTrue
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
    fun migrate1To5_preservesMovieAndMigratesPopularMembership() =
        runBlocking {
            createVersionOneDatabase(includeRuntimeColumn = true)

            val database = openMigratedDatabase()
            try {
                val dao = database.movieDao()

                val favorite = dao.getMovieById(1)
                assertNotNull(favorite)
                assertEquals(true, favorite?.isFavorite)
                assertEquals(false, favorite?.isWatchlist)
                assertEquals(false, favorite?.isWatched)
                assertEquals(120, favorite?.runtimeMinutes)

                // Legacy popular order is preserved in the association table.
                assertEquals(listOf(3, 1), dao.getCategoryMovieIds("POPULAR"))
                assertEquals(2, dao.getRemoteKey("POPULAR")?.nextKey)

                // Existing search associations survive the movies table rebuild.
                assertEquals(listOf(1), dao.getSearchResultMovieIds("matrix"))
            } finally {
                database.close()
            }
        }

    @Test
    fun migrate1To5_withoutRuntimeColumn_opensAndPreservesData() =
        runBlocking {
            createVersionOneDatabase(includeRuntimeColumn = false)

            val database = openMigratedDatabase()
            try {
                val dao = database.movieDao()

                val favorite = dao.getMovieById(1)
                assertNotNull(favorite)
                assertEquals(true, favorite?.isFavorite)
                assertEquals(false, favorite?.isWatchlist)
                assertEquals(false, favorite?.isWatched)
                // The legacy column never existed, so runtime stays null rather than being invented.
                assertEquals(null, favorite?.runtimeMinutes)

                assertEquals(listOf(3, 1), dao.getCategoryMovieIds("POPULAR"))
                assertEquals(2, dao.getRemoteKey("POPULAR")?.nextKey)
                assertEquals(listOf(1), dao.getSearchResultMovieIds("matrix"))
            } finally {
                database.close()
            }
        }

    @Test
    fun migrate2To5_collapsesRemoteKeysAndAddsLibraryFlags() =
        runBlocking {
            createVersionTwoDatabase()

            val database = openMigratedDatabase()
            try {
                val dao = database.movieDao()

                // A feed that reached its last page (NULL nextKey) stays exhausted.
                assertEquals(null, dao.getRemoteKey("POPULAR")?.nextKey)
                // A partially loaded feed keeps its next page.
                assertEquals(4, dao.getRemoteKey("UPCOMING")?.nextKey)
                assertEquals("Kept", dao.getMovieById(1)?.title)
                assertTrue(dao.getMovieById(1)?.isFavorite == true)
                assertEquals(false, dao.getMovieById(1)?.isWatchlist)
                assertEquals(false, dao.getMovieById(1)?.isWatched)

                assertTrue("isSearchResult" !in movieColumns(database))
            } finally {
                database.close()
            }
        }

    @Test
    fun migrate3To5_addsIndependentLibraryFlags() =
        runBlocking {
            createVersionThreeDatabase()
            val database = openMigratedDatabase()
            try {
                val movie = database.movieDao().getMovieById(1)
                assertEquals(true, movie?.isFavorite)
                assertEquals(false, movie?.isWatchlist)
                assertEquals(false, movie?.isWatched)
            } finally {
                database.close()
            }
        }

    private fun openMigratedDatabase(): MovieDatabase =
        Room
            .databaseBuilder(context, MovieDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    @Test
    fun migrate4To5_resolvesConflictingLibraryFlagsKeepingWatched() =
        runBlocking {
            createVersionFourDatabaseWithConflict()
            val database = openMigratedDatabase()
            try {
                val movie = database.movieDao().getMovieById(1)
                assertEquals(true, movie?.isFavorite)
                assertEquals(false, movie?.isWatchlist)
                assertEquals(true, movie?.isWatched)
            } finally {
                database.close()
            }
        }

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

    private fun createVersionTwoDatabase() {
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(2) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    "CREATE TABLE IF NOT EXISTS `movies` (" +
                                        "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, " +
                                        "`posterPath` TEXT, `backdropPath` TEXT, `voteAverage` REAL NOT NULL, " +
                                        "`releaseDate` TEXT NOT NULL, `genreIds` TEXT NOT NULL, " +
                                        "`isFavorite` INTEGER NOT NULL, `isSearchResult` INTEGER NOT NULL, " +
                                        "`runtimeMinutes` INTEGER, PRIMARY KEY(`id`))",
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
                                db.execSQL(
                                    "CREATE TABLE IF NOT EXISTS `movie_categories` (" +
                                        "`movieId` INTEGER NOT NULL, `category` TEXT NOT NULL, " +
                                        "`pageOrder` INTEGER NOT NULL, PRIMARY KEY(`movieId`, `category`))",
                                )
                                db.execSQL(
                                    "CREATE INDEX IF NOT EXISTS `index_movie_categories_category_pageOrder` " +
                                        "ON `movie_categories` (`category`, `pageOrder`)",
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
                "INSERT INTO `movies` (" +
                    "`id`,`title`,`overview`,`posterPath`,`backdropPath`,`voteAverage`,`releaseDate`," +
                    "`genreIds`,`isFavorite`,`isSearchResult`,`runtimeMinutes`) " +
                    "VALUES (1,'Kept','o',NULL,NULL,7.0,'2020-01-01','28',1,1,137)",
            )
            // Same feed: an early page (nextKey=2) then the final page (nextKey NULL) -> exhausted.
            db.execSQL("INSERT INTO `remote_keys` (`movieId`,`prevKey`,`nextKey`,`type`) VALUES (1,NULL,2,'POPULAR')")
            db.execSQL("INSERT INTO `remote_keys` (`movieId`,`prevKey`,`nextKey`,`type`) VALUES (2,1,NULL,'POPULAR')")
            // Another feed still has a next page.
            db.execSQL("INSERT INTO `remote_keys` (`movieId`,`prevKey`,`nextKey`,`type`) VALUES (3,NULL,4,'UPCOMING')")
        }
        helper.close()
    }

    private fun createVersionThreeDatabase() {
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(3) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    "CREATE TABLE `movies` (" +
                                        "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, " +
                                        "`posterPath` TEXT, `backdropPath` TEXT, `voteAverage` REAL NOT NULL, " +
                                        "`releaseDate` TEXT NOT NULL, `genreIds` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, " +
                                        "`runtimeMinutes` INTEGER, PRIMARY KEY(`id`))",
                                )
                                db.execSQL("CREATE TABLE `remote_keys` (`type` TEXT NOT NULL, `nextKey` INTEGER, PRIMARY KEY(`type`))")
                                db.execSQL(
                                    "CREATE TABLE `movie_search_results` (`queryKey` TEXT NOT NULL, `movieId` INTEGER NOT NULL, `pageOrder` INTEGER NOT NULL, PRIMARY KEY(`queryKey`, `movieId`))",
                                )
                                db.execSQL(
                                    "CREATE TABLE `movie_categories` (`movieId` INTEGER NOT NULL, `category` TEXT NOT NULL, `pageOrder` INTEGER NOT NULL, PRIMARY KEY(`movieId`, `category`))",
                                )
                                db.execSQL(
                                    "CREATE INDEX `index_movie_categories_category_pageOrder` ON `movie_categories` (`category`, `pageOrder`)",
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
                "INSERT INTO `movies` (`id`,`title`,`overview`,`voteAverage`,`releaseDate`,`genreIds`,`isFavorite`,`runtimeMinutes`) VALUES (1,'Kept','o',7.0,'2020-01-01','28',1,120)",
            )
        }
        helper.close()
    }

    private fun createVersionFourDatabaseWithConflict() {
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(4) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    "CREATE TABLE `movies` (" +
                                        "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, " +
                                        "`posterPath` TEXT, `backdropPath` TEXT, `voteAverage` REAL NOT NULL, " +
                                        "`releaseDate` TEXT NOT NULL, `genreIds` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, " +
                                        "`isWatchlist` INTEGER NOT NULL, `isWatched` INTEGER NOT NULL, `runtimeMinutes` INTEGER, PRIMARY KEY(`id`))",
                                )
                                db.execSQL("CREATE TABLE `remote_keys` (`type` TEXT NOT NULL, `nextKey` INTEGER, PRIMARY KEY(`type`))")
                                db.execSQL(
                                    "CREATE TABLE `movie_search_results` (`queryKey` TEXT NOT NULL, `movieId` INTEGER NOT NULL, `pageOrder` INTEGER NOT NULL, PRIMARY KEY(`queryKey`, `movieId`))",
                                )
                                db.execSQL(
                                    "CREATE TABLE `movie_categories` (`movieId` INTEGER NOT NULL, `category` TEXT NOT NULL, `pageOrder` INTEGER NOT NULL, PRIMARY KEY(`movieId`, `category`))",
                                )
                                db.execSQL(
                                    "CREATE INDEX `index_movie_categories_category_pageOrder` ON `movie_categories` (`category`, `pageOrder`)",
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
                "INSERT INTO `movies` " +
                    "(`id`,`title`,`overview`,`voteAverage`,`releaseDate`,`genreIds`,`isFavorite`,`isWatchlist`,`isWatched`) " +
                    "VALUES (1,'Conflict','o',7.0,'2020-01-01','28',1,1,1)",
            )
        }
        helper.close()
    }

    private fun movieColumns(database: MovieDatabase): Set<String> {
        val columns = mutableSetOf<String>()
        database.openHelper.writableDatabase.query("PRAGMA table_info(`movies`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns += cursor.getString(nameIndex)
            }
        }
        return columns
    }
}
