package com.benjamin.moviehub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MovieEntity::class], version = 2, exportSchema = false)
abstract class TestMovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
}