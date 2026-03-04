package com.benjamin.moviehub.core.util

import android.content.Context
import android.content.Intent
import com.benjamin.moviehub.domain.model.Movie

/**
 * Share a movie through an intent, with the movie title and matching TMDB url
 */
fun shareMovie(
    context: Context,
    movie: Movie,
) {
    val sendIntent: Intent =
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Hey, regarde ce film sur MovieHub: ${movie.title} \n ${movie.webUrl}",
            )
            type = "text/plain"
        }

    val shareIntent = Intent.createChooser(sendIntent, "Partager via")
    context.startActivity(shareIntent)
}
