package com.benjamin.moviehub.data.connectivity

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val STATUS_TIMEOUT_MS = 5_000L
private const val NO_STATUS_TIMEOUT_MS = 250L

@RunWith(AndroidJUnit4::class)
class NetworkConnectivityObserverTest {
    private val wifi = network(netId = 100)
    private val cellular = network(netId = 200)

    @Test
    fun registrationMatchesInternetNetworksWithoutImplicitRestrictions() =
        runBlocking {
            val harness = Harness(emptyMap())
            val job = harness.start(this)
            harness.awaitStatus()

            val requested = capabilitiesFromRequest(harness.request)
            assertTrue(requested.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            assertFalse(requested.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN))
            assertFalse(requested.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED))
            assertFalse(requested.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED))
            assertFalse(requested.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))

            job.cancelAndJoin()
        }

    @Test
    fun initialStatusReflectsThePlatformSnapshot() =
        runBlocking {
            val validatedHarness = Harness(mapOf(wifi to validated()))
            val validatedJob = validatedHarness.start(this)
            assertEquals(ConnectivityStatus.AVAILABLE, validatedHarness.awaitStatus())
            validatedJob.cancelAndJoin()

            val offlineHarness = Harness(emptyMap())
            val offlineJob = offlineHarness.start(this)
            assertEquals(ConnectivityStatus.UNAVAILABLE, offlineHarness.awaitStatus())
            offlineJob.cancelAndJoin()
        }

    @Test
    fun seededValidatedNetworkSurvivesAnotherNetworksUnvalidatedCallbacks() =
        runBlocking {
            val harness = Harness(mapOf(wifi to validated()))
            val job = harness.start(this)
            assertEquals(ConnectivityStatus.AVAILABLE, harness.awaitStatus())

            // A second network reporting unvalidated capabilities before wifi's own callback
            // must not drop the already AVAILABLE state.
            harness.callback.onAvailable(cellular)
            harness.callback.onCapabilitiesChanged(cellular, unvalidated())
            harness.awaitNoStatus()

            harness.callback.onLost(wifi)
            assertEquals(ConnectivityStatus.UNAVAILABLE, harness.awaitStatus())

            job.cancelAndJoin()
        }

    @Test
    fun callbackCapabilitiesDriveValidatedAndUnvalidatedTransitions() =
        runBlocking {
            val harness = Harness(emptyMap())
            val job = harness.start(this)
            assertEquals(ConnectivityStatus.UNAVAILABLE, harness.awaitStatus())

            harness.callback.onAvailable(wifi)
            harness.awaitNoStatus()

            harness.callback.onCapabilitiesChanged(wifi, unvalidated())
            harness.awaitNoStatus()

            harness.callback.onCapabilitiesChanged(wifi, validated())
            assertEquals(ConnectivityStatus.AVAILABLE, harness.awaitStatus())

            harness.callback.onCapabilitiesChanged(wifi, unvalidated())
            assertEquals(ConnectivityStatus.UNAVAILABLE, harness.awaitStatus())

            job.cancelAndJoin()
        }

    @Test
    fun twoNetworkHandoverStaysAvailableUntilTheLastNetworkIsLost() =
        runBlocking {
            val harness = Harness(emptyMap())
            val job = harness.start(this)
            assertEquals(ConnectivityStatus.UNAVAILABLE, harness.awaitStatus())

            harness.callback.onAvailable(wifi)
            harness.callback.onCapabilitiesChanged(wifi, validated())
            assertEquals(ConnectivityStatus.AVAILABLE, harness.awaitStatus())

            harness.callback.onAvailable(cellular)
            harness.awaitNoStatus()

            harness.callback.onCapabilitiesChanged(cellular, validated())
            harness.awaitNoStatus()

            harness.callback.onLost(wifi)
            harness.awaitNoStatus()

            harness.callback.onLost(cellular)
            assertEquals(ConnectivityStatus.LOST, harness.awaitStatus())

            job.cancelAndJoin()
        }

    @Test
    fun losingTheOnlyValidatedNetworkFallsBackToTheRemainingUnvalidatedOne() =
        runBlocking {
            val harness = Harness(emptyMap())
            val job = harness.start(this)
            assertEquals(ConnectivityStatus.UNAVAILABLE, harness.awaitStatus())

            harness.callback.onAvailable(wifi)
            harness.callback.onCapabilitiesChanged(wifi, validated())
            assertEquals(ConnectivityStatus.AVAILABLE, harness.awaitStatus())

            harness.callback.onAvailable(cellular)
            harness.callback.onCapabilitiesChanged(cellular, unvalidated())
            harness.awaitNoStatus()

            harness.callback.onLost(wifi)
            assertEquals(ConnectivityStatus.UNAVAILABLE, harness.awaitStatus())

            job.cancelAndJoin()
        }

    @Test
    fun collectionRegistersAndReleasesTheNetworkCallback() =
        runBlocking {
            val harness = Harness(mapOf(wifi to validated()))
            val job = harness.start(this)
            harness.awaitStatus()

            val registered = harness.callback
            job.cancelAndJoin()

            assertSame(registered, harness.unregisteredCallback)
        }

    private class Harness(
        initialCapabilities: Map<Network, NetworkCapabilities>,
    ) {
        private val registrar = FakeRegistrar(initialCapabilities)
        private val observer = NetworkConnectivityObserver(registrar)
        private val emissions = Channel<ConnectivityStatus>(Channel.UNLIMITED)

        val callback: ConnectivityManager.NetworkCallback
            get() = requireNotNull(registrar.registeredCallback) { "The flow did not register a callback" }

        val request: NetworkRequest
            get() = requireNotNull(registrar.registeredRequest)

        val unregisteredCallback: ConnectivityManager.NetworkCallback?
            get() = registrar.unregisteredCallback

        fun start(scope: CoroutineScope): Job = observer.observe().onEach { emissions.send(it) }.launchIn(scope)

        suspend fun awaitStatus(): ConnectivityStatus = withTimeout(STATUS_TIMEOUT_MS) { emissions.receive() }

        suspend fun awaitNoStatus() {
            assertNull(withTimeoutOrNull(NO_STATUS_TIMEOUT_MS) { emissions.receive() })
        }
    }

    private class FakeRegistrar(
        private val snapshot: Map<Network, NetworkCapabilities>,
    ) : NetworkCallbackRegistrar {
        @Volatile
        var registeredCallback: ConnectivityManager.NetworkCallback? = null
            private set

        var registeredRequest: NetworkRequest? = null
            private set

        @Volatile
        var unregisteredCallback: ConnectivityManager.NetworkCallback? = null
            private set

        override fun initialCapabilities(): Map<Network, NetworkCapabilities> = snapshot

        override fun register(
            request: NetworkRequest,
            callback: ConnectivityManager.NetworkCallback,
        ) {
            registeredRequest = request
            registeredCallback = callback
        }

        override fun unregister(callback: ConnectivityManager.NetworkCallback) {
            unregisteredCallback = callback
        }
    }

    private fun validated(): NetworkCapabilities = capabilities(validated = true)

    private fun unvalidated(): NetworkCapabilities = capabilities(validated = false)

    private fun capabilities(validated: Boolean): NetworkCapabilities {
        val builder = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (validated) builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return capabilitiesFromRequest(builder.build())
    }

    private fun capabilitiesFromRequest(request: NetworkRequest): NetworkCapabilities {
        val parcel = Parcel.obtain()
        return try {
            // NetworkRequest parcels its capabilities first. Use the public builder and
            // Parcelable factory because NetworkCapabilities mutators are not public APIs.
            request.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            NetworkCapabilities.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    /** Builds a real [Network] with a distinct id through the public Parcelable factory. */
    private fun network(netId: Int): Network {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeInt(netId)
            parcel.writeInt(0)
            parcel.setDataPosition(0)
            Network.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }
}
