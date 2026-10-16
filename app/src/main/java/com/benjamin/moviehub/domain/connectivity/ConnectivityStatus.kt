package com.benjamin.moviehub.domain.connectivity

enum class ConnectivityStatus {
    UNKNOWN,
    AVAILABLE,
    UNAVAILABLE,
    LOST,
}

val ConnectivityStatus.isOffline: Boolean
    get() = this == ConnectivityStatus.LOST || this == ConnectivityStatus.UNAVAILABLE
