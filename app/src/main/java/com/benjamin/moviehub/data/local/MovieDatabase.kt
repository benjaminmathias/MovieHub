package com.benjamin.moviehub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MovieEntity::class,
        RemoteKey::class,
        MovieSearchResultEntity::class,
        MovieCategoryEntity::class,
        GenreEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
}
