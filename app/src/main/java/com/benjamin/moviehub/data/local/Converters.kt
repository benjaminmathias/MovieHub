package com.benjamin.moviehub.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromGenreIds(genreIds: List<Int>?): String {
        return genreIds?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toGenreIds(data: String): List<Int> {
        return if (data.isEmpty()) {
            emptyList()
        } else {
            data.split(",").map { it.toInt() }
        }
    }
}