package com.benjamin.moviehub.core.util

import java.text.SimpleDateFormat
import java.util.Locale

fun formatReleaseDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "Date inconnue"

    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH)
        val date = parser.parse(dateString)
        if (date != null) formatter.format(date) else dateString.take(4)
    } catch (e: Exception) {
        dateString.take(4)
    }
}