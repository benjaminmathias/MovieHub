package com.benjamin.moviehub.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object List : Route, NavKey

    @Serializable
    data object FavoriteList : Route, NavKey

    @Serializable
    data class Detail(val movieId: Int) : Route, NavKey
}