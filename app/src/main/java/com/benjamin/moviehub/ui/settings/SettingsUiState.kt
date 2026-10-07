package com.benjamin.moviehub.ui.settings

enum class SettingsActionErrorCode {
    UPDATE_THEME,
}

sealed interface SettingsActionErrorState {
    data object None : SettingsActionErrorState

    data class Failure(
        val code: SettingsActionErrorCode,
    ) : SettingsActionErrorState
}

sealed interface ImageCacheResult {
    data object Idle : ImageCacheResult

    data object Cleared : ImageCacheResult

    data class Failed(
        val code: ImageCacheErrorCode,
    ) : ImageCacheResult
}

enum class ImageCacheErrorCode {
    CLEAR_CACHE,
}
