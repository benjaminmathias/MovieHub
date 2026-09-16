package com.benjamin.moviehub.domain.connectivity

enum class ConnectivityStatus {
    UNKNOWN,
    AVAILABLE,
    UNAVAILABLE,
    LOST,
}

/** True when the device has no usable connection (never, unavailable or lost). */
val ConnectivityStatus.isOffline: Boolean
    get() = this == ConnectivityStatus.LOST || this == ConnectivityStatus.UNAVAILABLE
