package com.benjamin.moviehub.core.util

import androidx.annotation.StringRes
import com.benjamin.moviehub.R

enum class AppTheme(
    @param:StringRes val labelRes: Int,
) {
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
    SYSTEM(R.string.theme_system),
    ;

    fun isDark(systemInDarkTheme: Boolean): Boolean =
        when (this) {
            LIGHT -> false
            DARK -> true
            SYSTEM -> systemInDarkTheme
        }
}
