package com.benjamin.moviehub.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.benjamin.moviehub.domain.connectivity.ConnectivityObserver
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class NetworkConnectivityObserver
    internal constructor(
        private val registrar: NetworkCallbackRegistrar,
    ) : ConnectivityObserver {
        @Inject
        constructor(
            @ApplicationContext context: Context,
        ) : this(
            AndroidNetworkCallbackRegistrar(
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
            ),
        )

        override fun observe(): Flow<ConnectivityStatus> =
            callbackFlow {
                // Seed the tracked networks from a snapshot taken before registration. Without
                // it, a callback for a second, unvalidated network arriving before the first
                // network's callbacks would momentarily drop an already AVAILABLE state.
                // A null value marks a network announced by onAvailable whose capabilities are
                // not known yet; the key alone still counts as a tracked network.
                val capabilitiesByNetwork = mutableMapOf<Network, NetworkCapabilities?>()
                registrar.initialCapabilities().forEach { (network, capabilities) ->
                    capabilitiesByNetwork[network] = capabilities
                }

                fun currentStatus(): ConnectivityStatus {
                    val hasValidatedNetwork =
                        capabilitiesByNetwork.values.any { capabilities ->
                            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                        }
                    return if (hasValidatedNetwork) ConnectivityStatus.AVAILABLE else ConnectivityStatus.UNAVAILABLE
                }

                trySend(currentStatus())

                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            super.onAvailable(network)
                            // Track the network without emitting and without overwriting capabilities
                            // already known from the snapshot or an earlier callback: capabilities are
                            // still unknown, so the aggregate state must stay as it was.
                            if (network !in capabilitiesByNetwork) {
                                capabilitiesByNetwork[network] = null
                            }
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            networkCapabilities: NetworkCapabilities,
                        ) {
                            super.onCapabilitiesChanged(network, networkCapabilities)
                            capabilitiesByNetwork[network] = networkCapabilities
                            trySend(currentStatus())
                        }

                        override fun onLost(network: Network) {
                            super.onLost(network)
                            capabilitiesByNetwork.remove(network)
                            if (capabilitiesByNetwork.isEmpty()) {
                                trySend(ConnectivityStatus.LOST)
                            } else {
                                trySend(currentStatus())
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
                        // Match the INTERNET-only snapshot, including validated VPN networks.
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                clearCapabilities()
                            } else {
                                removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                                removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                                removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                            }
                        }.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()

                registrar.register(request, callback)

                awaitClose {
                    registrar.unregister(callback)
                }
            }.distinctUntilChanged()
    }

/**
 * ConnectivityManager operations used by [NetworkConnectivityObserver]. Kept behind an
 * interface so the observer can be driven with controlled callbacks in instrumented tests.
 */
internal interface NetworkCallbackRegistrar {
    /**
     * Snapshot of the currently available networks that satisfy the observer's request,
     * paired with their capabilities, taken before the callback is registered.
     */
    fun initialCapabilities(): Map<Network, NetworkCapabilities>

    fun register(
        request: NetworkRequest,
        callback: ConnectivityManager.NetworkCallback,
    )

    fun unregister(callback: ConnectivityManager.NetworkCallback)
}

private class AndroidNetworkCallbackRegistrar(
    private val connectivityManager: ConnectivityManager,
) : NetworkCallbackRegistrar {
    override fun initialCapabilities(): Map<Network, NetworkCapabilities> =
        // Seed only the active network: allNetworks can include lingering background networks
        // that Android excludes from this app's callback and will never report as lost.
        listOfNotNull(connectivityManager.activeNetwork)
            .mapNotNull { network ->
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@mapNotNull null
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    network to capabilities
                } else {
                    null
                }
            }.toMap()

    override fun register(
        request: NetworkRequest,
        callback: ConnectivityManager.NetworkCallback,
    ) {
        connectivityManager.registerNetworkCallback(request, callback)
    }

    override fun unregister(callback: ConnectivityManager.NetworkCallback) {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}
