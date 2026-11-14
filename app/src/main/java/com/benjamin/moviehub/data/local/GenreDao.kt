package com.benjamin.moviehub.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface GenreDao {
    @Query("SELECT * FROM genres")
    suspend fun getGenres(): List<GenreEntity>

    @Upsert
    suspend fun upsertGenres(genres: List<GenreEntity>)
}
