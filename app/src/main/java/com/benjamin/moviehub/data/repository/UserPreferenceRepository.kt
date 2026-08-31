package com.benjamin.moviehub.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferenceRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : UserPreferencesRepository {
        private object Keys {
            val THEME = stringPreferencesKey("app_theme")
        }

        override val theme: Flow<AppTheme> =
            context.dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { preferences ->
                    val themeName = preferences[Keys.THEME] ?: AppTheme.SYSTEM.name
                    try {
                        AppTheme.valueOf(themeName)
                    } catch (e: IllegalArgumentException) {
                        AppTheme.SYSTEM
                    }
                }

        override suspend fun setTheme(theme: AppTheme) {
            context.dataStore.edit { preferences ->
                preferences[Keys.THEME] = theme.name
            }
        }
    }
