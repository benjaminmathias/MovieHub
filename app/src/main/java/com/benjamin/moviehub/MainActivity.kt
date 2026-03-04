package com.benjamin.moviehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.core.util.AppTheme
import com.benjamin.moviehub.domain.worker.SyncMoviesWorker
import com.benjamin.moviehub.ui.MainViewModel
import com.benjamin.moviehub.ui.components.NetworkStatusBar
import com.benjamin.moviehub.ui.navigation.NavigationRoot
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

        val syncWorkRequest =
            PeriodicWorkRequestBuilder<SyncMoviesWorker>(
                1,
                TimeUnit.DAYS,
            ).setConstraints(constraints)
                .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "daily_movie_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest,
        )

        setContent {
            val appTheme by mainViewModel.theme.collectAsStateWithLifecycle()
            val networkStatus by mainViewModel.networkStatus.collectAsStateWithLifecycle()

            val useDarkTheme =
                when (appTheme) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }

            MovieHubTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavigationRoot()

                        NetworkStatusBar(
                            status = networkStatus,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}
