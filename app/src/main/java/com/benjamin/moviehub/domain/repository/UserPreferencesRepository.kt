package com.benjamin.moviehub.domain.repository

import com.benjamin.moviehub.core.util.AppTheme
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val theme: Flow<AppTheme>

    suspend fun setTheme(theme: AppTheme)
}
