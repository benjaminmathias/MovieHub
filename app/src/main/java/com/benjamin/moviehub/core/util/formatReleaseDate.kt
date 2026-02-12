package com.benjamin.moviehub.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String?.formatReleaseDate(): String {

    if (this.isNullOrBlank()) return "Date inconnue"

    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        val date: Date? = inputFormat.parse(this)

        date?.let { outputFormat.format(it) } ?: this

    } catch (e: Exception) {
        this.take(4)
    }
}