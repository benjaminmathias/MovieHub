package com.benjamin.moviehub.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.benjamin.moviehub.core.util.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferenceRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private object Keys {
        val THEME = stringPreferencesKey("app_theme")
    }

    val theme: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[Keys.THEME] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                AppTheme.SYSTEM
            }
        }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME] = theme.name
        }
    }
}