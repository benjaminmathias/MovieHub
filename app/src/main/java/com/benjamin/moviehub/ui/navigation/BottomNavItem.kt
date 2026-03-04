package com.benjamin.moviehub.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.benjamin.moviehub.R

sealed class BottomNavItem(
    val route: Route,
    val icon: ImageVector,
    val labelRes: Int,
) {
    data object Home : BottomNavItem(Route.List, Icons.Default.Home, R.string.home_tab)

    data object Favorite : BottomNavItem(Route.FavoriteList, Icons.Default.Favorite, R.string.favorite_tab)
}
