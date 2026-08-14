package com.benjamin.moviehub.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.util.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val imageCacheState by viewModel.imageCacheState.collectAsState()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val imageCacheClearedMessage = stringResource(R.string.image_cache_cleared)
    val imageCacheClearFailedMessage = stringResource(R.string.image_cache_clear_failed)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection(title = stringResource(R.string.appearance_section)) {
                ThemeSelector(
                    currentTheme = currentTheme,
                    onThemeSelected = viewModel::updateTheme,
                )
            }

            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.storage_section)) {
                SettingsItem(
                    title = stringResource(R.string.clear_image_cache),
                    subtitle = stringResource(R.string.clear_image_cache_description),
                    icon = Icons.Default.Delete,
                    onClick = {
                        viewModel.clearImageCache()
                    },
                )
            }

            HorizontalDivider()

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.about_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    LaunchedEffect(imageCacheState) {
        when (imageCacheState) {
            ImageCacheState.Success -> {
                snackbarHostState.showSnackbar(imageCacheClearedMessage)
                viewModel.resetImageCacheState()
            }

            ImageCacheState.Error -> {
                snackbarHostState.showSnackbar(imageCacheClearFailedMessage)
                viewModel.resetImageCacheState()
            }

            ImageCacheState.Idle, ImageCacheState.Loading -> Unit
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
fun ThemeSelector(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
) {
    Column {
        ThemeRadioButton(
            selected = currentTheme == AppTheme.SYSTEM,
            text = stringResource(R.string.theme_system),
            onClick = { onThemeSelected(AppTheme.SYSTEM) },
        )
        ThemeRadioButton(
            selected = currentTheme == AppTheme.LIGHT,
            text = stringResource(R.string.theme_light),
            onClick = { onThemeSelected(AppTheme.LIGHT) },
        )
        ThemeRadioButton(
            selected = currentTheme == AppTheme.DARK,
            text = stringResource(R.string.theme_dark),
            onClick = { onThemeSelected(AppTheme.DARK) },
        )
    }
}

@Composable
fun ThemeRadioButton(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
