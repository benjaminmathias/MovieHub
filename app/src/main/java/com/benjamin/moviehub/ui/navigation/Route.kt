package com.benjamin.moviehub.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object List : Route

    @Serializable
    data object FavoriteList : Route

    @Serializable
    data object Search : Route

    @Serializable
    data class Detail(
        val movieId: Int,
    ) : Route

    @Serializable
    data object Settings : Route
}
