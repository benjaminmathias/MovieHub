package com.benjamin.moviehub.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.benjamin.moviehub.domain.connectivity.ConnectivityObserver
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class NetworkConnectivityObserver
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ConnectivityObserver {
        private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        override fun observe(): Flow<ConnectivityStatus> =
            callbackFlow {
                trySend(currentStatus())
                val networks = mutableSetOf<Network>()

                fun emitCurrentStatus() {
                    val hasValidatedNetwork =
                        networks.any { network ->
                            connectivityManager
                                .getNetworkCapabilities(network)
                                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                        }
                    trySend(if (hasValidatedNetwork) ConnectivityStatus.AVAILABLE else ConnectivityStatus.UNAVAILABLE)
                }

                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            super.onAvailable(network)
                            networks += network
                            emitCurrentStatus()
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            networkCapabilities: NetworkCapabilities,
                        ) {
                            super.onCapabilitiesChanged(network, networkCapabilities)
                            networks += network
                            emitCurrentStatus()
                        }

                        override fun onLost(network: Network) {
                            super.onLost(network)
                            networks -= network
                            if (networks.isEmpty()) {
                                trySend(ConnectivityStatus.LOST)
                            } else {
                                emitCurrentStatus()
                            }
                        }

                        override fun onUnavailable() {
                            super.onUnavailable()
                            trySend(ConnectivityStatus.UNAVAILABLE)
                        }
                    }

                val request =
                    NetworkRequest
                        .Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()

                connectivityManager.registerNetworkCallback(request, callback)

                awaitClose {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }.distinctUntilChanged()

        private fun currentStatus(): ConnectivityStatus {
            val network = connectivityManager.activeNetwork ?: return ConnectivityStatus.UNAVAILABLE
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) {
                ConnectivityStatus.AVAILABLE
            } else {
                ConnectivityStatus.UNAVAILABLE
            }
        }
    }
