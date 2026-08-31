package com.benjamin.moviehub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MovieEntity::class, MovieRemoteKey::class, MovieSearchResultEntity::class],
    version = 1,
    exportSchema = false,
)
// runtimeMinutes is intentionally a fresh-install-only schema change for now.
@TypeConverters(Converters::class)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
}
