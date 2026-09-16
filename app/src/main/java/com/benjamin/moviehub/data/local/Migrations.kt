package com.benjamin.moviehub.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val MOVIES_COLUMNS_V2 =
    "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, " +
        "`posterPath` TEXT, `backdropPath` TEXT, `voteAverage` REAL NOT NULL, " +
        "`releaseDate` TEXT NOT NULL, `genreIds` TEXT NOT NULL, " +
        "`isFavorite` INTEGER NOT NULL, `isSearchResult` INTEGER NOT NULL, `runtimeMinutes` INTEGER"

private const val MOVIES_COLUMNS_V3 =
    "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, " +
        "`posterPath` TEXT, `backdropPath` TEXT, `voteAverage` REAL NOT NULL, " +
        "`releaseDate` TEXT NOT NULL, `genreIds` TEXT NOT NULL, " +
        "`isFavorite` INTEGER NOT NULL, `runtimeMinutes` INTEGER"

private const val MOVIES_COLUMNS_CSV =
    "`id`, `title`, `overview`, `posterPath`, `backdropPath`, `voteAverage`, " +
        "`releaseDate`, `genreIds`, `isFavorite`, `isSearchResult`, `runtimeMinutes`"

/**
 * Adds the normalized `movie_categories` association (plus its `(category, pageOrder)` index) and
 * removes the legacy popular-only columns (`isPopular`, `pageOrder`) from `movies`.
 *
 * Deliberately tolerant: it inspects the existing schema so it can migrate any version-1 database,
 * including older ones potentially restored by Android Auto Backup.
 */
internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `movie_categories` (" +
                    "`movieId` INTEGER NOT NULL, `category` TEXT NOT NULL, " +
                    "`pageOrder` INTEGER NOT NULL, PRIMARY KEY(`movieId`, `category`))",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_movie_categories_category_pageOrder` " +
                    "ON `movie_categories` (`category`, `pageOrder`)",
            )
            // Be robust to a v1 database that predates these tables.
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `remote_keys` (" +
                    "`movieId` INTEGER NOT NULL, `prevKey` INTEGER, `nextKey` INTEGER, " +
                    "`type` TEXT NOT NULL, PRIMARY KEY(`movieId`, `type`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `movie_search_results` (" +
                    "`queryKey` TEXT NOT NULL, `movieId` INTEGER NOT NULL, `pageOrder` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`queryKey`, `movieId`))",
            )

            val movieColumns = db.columnNames("movies")

            if ("isPopular" in movieColumns && "pageOrder" in movieColumns) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `movie_categories` (`movieId`, `category`, `pageOrder`) " +
                        "SELECT `id`, 'POPULAR', `pageOrder` FROM `movies` WHERE `isPopular` = 1",
                )
            }

            if (movieColumns.isEmpty()) {
                // No movies table to preserve: create the current schema directly.
                db.createMoviesTable(MOVIES_COLUMNS_V2)
                return
            }

            // SQLite cannot drop columns in place, so rebuild `movies` without the legacy ones.
            db.createMoviesTable(MOVIES_COLUMNS_V2, table = "movies_new")
            val runtimeColumn = if ("runtimeMinutes" in movieColumns) "`runtimeMinutes`" else "NULL"
            db.execSQL(
                "INSERT OR REPLACE INTO `movies_new` ($MOVIES_COLUMNS_CSV) " +
                    "SELECT `id`, `title`, `overview`, `posterPath`, `backdropPath`, `voteAverage`, " +
                    "`releaseDate`, `genreIds`, `isFavorite`, `isSearchResult`, $runtimeColumn FROM `movies`",
            )
            db.renameMoviesTable()
        }
    }

/**
 * Collapses `remote_keys` to a single pagination cursor per feed and drops the
 * now-unused `isSearchResult` flag from `movies`.
 *
 * The per-feed cursor is the highest page reached: a NULL `nextKey` is only ever
 * written on a feed's final page, so any NULL row means the feed is exhausted.
 * Dropping `isSearchResult` is safe because search membership is already tracked
 * by `movie_search_results`.
 */
internal val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `remote_keys_new` (" +
                    "`type` TEXT NOT NULL, `nextKey` INTEGER, PRIMARY KEY(`type`))",
            )
            db.execSQL(
                "INSERT OR REPLACE INTO `remote_keys_new` (`type`, `nextKey`) " +
                    "SELECT `type`, CASE " +
                    "WHEN SUM(CASE WHEN `nextKey` IS NULL THEN 1 ELSE 0 END) > 0 THEN NULL " +
                    "ELSE MAX(`nextKey`) END " +
                    "FROM `remote_keys` GROUP BY `type`",
            )
            db.execSQL("DROP TABLE `remote_keys`")
            db.execSQL("ALTER TABLE `remote_keys_new` RENAME TO `remote_keys`")

            if ("isSearchResult" !in db.columnNames("movies")) return

            db.createMoviesTable(MOVIES_COLUMNS_V3, table = "movies_new")
            db.execSQL(
                "INSERT OR REPLACE INTO `movies_new` (" +
                    "`id`, `title`, `overview`, `posterPath`, `backdropPath`, `voteAverage`, " +
                    "`releaseDate`, `genreIds`, `isFavorite`, `runtimeMinutes`) " +
                    "SELECT `id`, `title`, `overview`, `posterPath`, `backdropPath`, `voteAverage`, " +
                    "`releaseDate`, `genreIds`, `isFavorite`, `runtimeMinutes` FROM `movies`",
            )
            db.renameMoviesTable()
        }
    }

internal val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val columns = db.columnNames("movies")
            if ("isWatchlist" !in columns) {
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `isWatchlist` INTEGER NOT NULL DEFAULT 0")
            }
            if ("isWatched" !in columns) {
                db.execSQL("ALTER TABLE `movies` ADD COLUMN `isWatched` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

internal val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Resolve the only invalid historical combination deterministically: watched wins.
            db.execSQL("UPDATE `movies` SET `isWatchlist` = 0 WHERE `isWatchlist` = 1 AND `isWatched` = 1")
        }
    }

/**
 * Adds the localized genre dictionary table. Empty on creation: it is populated
 * on the first `genre/movie/list` refresh, and the static mapper fallback covers
 * the window before that.
 */
internal val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `genres` (" +
                    "`id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

private fun SupportSQLiteDatabase.createMoviesTable(
    columns: String,
    table: String = "movies",
) = execSQL("CREATE TABLE IF NOT EXISTS `$table` ($columns, PRIMARY KEY(`id`))")

private fun SupportSQLiteDatabase.renameMoviesTable() {
    execSQL("DROP TABLE `movies`")
    execSQL("ALTER TABLE `movies_new` RENAME TO `movies`")
}

private fun SupportSQLiteDatabase.columnNames(table: String): Set<String> {
    val columns = mutableSetOf<String>()
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex >= 0) columns += cursor.getString(nameIndex)
        }
    }
    return columns
}
