package com.macasteglione.keepsafe.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * Monitor for network connectivity changes.
 *
 * Registers a callback to listen for internet-capable network transitions
 * (WiFi and Cellular) while ignoring VPN-type network changes to avoid
 * reconnection loops.
 */
class NetworkMonitor(
    private val context: Context,
    private val onNetworkChanged: () -> Unit
) {
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var currentNetwork: Network? = null

    /**
     * Starts monitoring network connectivity changes.
     */
    fun startMonitoring() {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handleNetworkChange(network)
            }

            override fun onLost(network: Network) {
                if (currentNetwork == network) {
                    currentNetwork = null
                }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                // IMPORTANT: Ignore changes coming from our own VPN to avoid loops
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return

                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                ) {
                    handleNetworkChange(network)
                }
            }
        }

        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
    }

    /**
     * Handles detected network change events.
     *
     * @param network The network that became active or changed capabilities.
     */
    private fun handleNetworkChange(network: Network) {
        if (currentNetwork != null && currentNetwork != network) {
            Log.w("NetworkMonitor", "Network changed from $currentNetwork to $network")
            currentNetwork = network
            onNetworkChanged()
        } else if (currentNetwork == null) {
            currentNetwork = network
            onNetworkChanged()
        }
    }

    /**
     * Stops network monitoring and cleans up resources.
     */
    fun stopMonitoring() {
        networkCallback?.let { callback ->
            connectivityManager?.unregisterNetworkCallback(callback)
        }
        networkCallback = null
        connectivityManager = null
        currentNetwork = null
    }
}
