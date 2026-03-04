package com.benjamin.moviehub.domain.connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    fun observe(): Flow<ConnectivityStatus>
}
