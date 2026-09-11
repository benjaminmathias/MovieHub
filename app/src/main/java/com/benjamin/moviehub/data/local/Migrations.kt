package com.benjamin.moviehub.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
                    "`movieId` INTEGER NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`pageOrder` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`movieId`, `category`))",
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

            val movieColumns = columnNames(db, "movies")

            if ("isPopular" in movieColumns && "pageOrder" in movieColumns) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `movie_categories` (`movieId`, `category`, `pageOrder`) " +
                        "SELECT `id`, 'POPULAR', `pageOrder` FROM `movies` WHERE `isPopular` = 1",
                )
            }

            if (movieColumns.isEmpty()) {
                // No movies table to preserve: create the current schema directly.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `movies` (" +
                        "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, " +
                        "`posterPath` TEXT, `backdropPath` TEXT, `voteAverage` REAL NOT NULL, " +
                        "`releaseDate` TEXT NOT NULL, `genreIds` TEXT NOT NULL, " +
                        "`isFavorite` INTEGER NOT NULL, `isSearchResult` INTEGER NOT NULL, " +
                        "`runtimeMinutes` INTEGER, PRIMARY KEY(`id`))",
                )
                return
            }

            // SQLite cannot drop columns in place, so rebuild `movies` without the legacy ones.
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `movies_new` (" +
                    "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, " +
                    "`posterPath` TEXT, `backdropPath` TEXT, `voteAverage` REAL NOT NULL, " +
                    "`releaseDate` TEXT NOT NULL, `genreIds` TEXT NOT NULL, " +
                    "`isFavorite` INTEGER NOT NULL, `isSearchResult` INTEGER NOT NULL, " +
                    "`runtimeMinutes` INTEGER, PRIMARY KEY(`id`))",
            )
            val runtimeColumn = if ("runtimeMinutes" in movieColumns) "`runtimeMinutes`" else "NULL"
            db.execSQL(
                "INSERT OR REPLACE INTO `movies_new` (" +
                    "`id`, `title`, `overview`, `posterPath`, `backdropPath`, `voteAverage`, " +
                    "`releaseDate`, `genreIds`, `isFavorite`, `isSearchResult`, `runtimeMinutes`) " +
                    "SELECT `id`, `title`, `overview`, `posterPath`, `backdropPath`, `voteAverage`, " +
                    "`releaseDate`, `genreIds`, `isFavorite`, `isSearchResult`, $runtimeColumn FROM `movies`",
            )
            db.execSQL("DROP TABLE `movies`")
            db.execSQL("ALTER TABLE `movies_new` RENAME TO `movies`")
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

            if ("isSearchResult" !in columnNames(db, "movies")) return

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `movies_new` (" +
                    "`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `overview` TEXT NOT NULL, " +
                    "`posterPath` TEXT, `backdropPath` TEXT, `voteAverage` REAL NOT NULL, " +
                    "`releaseDate` TEXT NOT NULL, `genreIds` TEXT NOT NULL, " +
                    "`isFavorite` INTEGER NOT NULL, `runtimeMinutes` INTEGER, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "INSERT OR REPLACE INTO `movies_new` (" +
                    "`id`, `title`, `overview`, `posterPath`, `backdropPath`, `voteAverage`, " +
                    "`releaseDate`, `genreIds`, `isFavorite`, `runtimeMinutes`) " +
                    "SELECT `id`, `title`, `overview`, `posterPath`, `backdropPath`, `voteAverage`, " +
                    "`releaseDate`, `genreIds`, `isFavorite`, `runtimeMinutes` FROM `movies`",
            )
            db.execSQL("DROP TABLE `movies`")
            db.execSQL("ALTER TABLE `movies_new` RENAME TO `movies`")
        }
    }

private fun columnNames(
    db: SupportSQLiteDatabase,
    table: String,
): Set<String> {
    val columns = mutableSetOf<String>()
    db.query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex >= 0) columns += cursor.getString(nameIndex)
        }
    }
    return columns
}
