package com.benjamin.moviehub.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.core.util.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UserPreferencesPersistenceTest {
    @Test
    fun themeSurvivesClosingAndReopeningTheStore() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val file = File(context.cacheDir, "theme-${UUID.randomUUID()}.preferences_pb")
            try {
                val firstJob = SupervisorJob()
                try {
                    val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO)) { file }
                    val repository = UserPreferencesRepositoryImpl(store)
                    assertEquals(AppTheme.SYSTEM, repository.theme.first())
                    repository.setTheme(AppTheme.DARK)
                    assertEquals(AppTheme.DARK, repository.theme.first())
                } finally {
                    firstJob.cancelAndJoin()
                }

                val reopenedJob = SupervisorJob()
                try {
                    val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(reopenedJob + Dispatchers.IO)) { file }
                    val repository = UserPreferencesRepositoryImpl(store)
                    assertEquals(AppTheme.DARK, repository.theme.first())
                    repository.setTheme(AppTheme.LIGHT)
                    assertEquals(AppTheme.LIGHT, repository.theme.first())
                    repository.setTheme(AppTheme.SYSTEM)
                    assertEquals(AppTheme.SYSTEM, repository.theme.first())
                } finally {
                    reopenedJob.cancelAndJoin()
                }
            } finally {
                file.delete()
            }
        }
}
