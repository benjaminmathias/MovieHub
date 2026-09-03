package com.benjamin.moviehub.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromGenreIds(genreIds: List<Int>): String = genreIds.joinToString(",")

    @TypeConverter
    fun toGenreIds(data: String): List<Int> =
        if (data.isEmpty()) {
            emptyList()
        } else {
            // mapNotNull: une ligne corrompue ne doit jamais crasher la lecture Room.
            data.split(",").mapNotNull { it.toIntOrNull() }
        }
}
