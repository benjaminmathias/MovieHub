package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.core.util.AppTheme
import kotlinx.coroutines.flow.Flow

/** Repository contract for persisted user preferences. */
interface UserPreferencesRepository {
    val theme: Flow<AppTheme>

    suspend fun setTheme(theme: AppTheme)
}
