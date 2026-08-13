package com.benjamin.moviehub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MovieEntity::class, MovieRemoteKey::class, MovieSearchResultEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    companion object {
        val MIGRATION_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS remote_keys_new (
                            movieId INTEGER NOT NULL,
                            prevKey INTEGER,
                            nextKey INTEGER,
                            type TEXT NOT NULL,
                            PRIMARY KEY(movieId, type)
                        )
                        """.trimIndent(),
                    )
                    database.execSQL(
                        """
                        INSERT INTO remote_keys_new (movieId, prevKey, nextKey, type)
                        SELECT movieId, prevKey, nextKey, type FROM remote_keys
                        """.trimIndent(),
                    )
                    database.execSQL("DROP TABLE remote_keys")
                    database.execSQL("ALTER TABLE remote_keys_new RENAME TO remote_keys")
                }
            }

        val MIGRATION_3_4: Migration =
            object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS movie_search_results (
                            queryKey TEXT NOT NULL,
                            movieId INTEGER NOT NULL,
                            pageOrder INTEGER NOT NULL,
                            PRIMARY KEY(queryKey, movieId)
                        )
                        """.trimIndent(),
                    )
                }
            }
    }
}
