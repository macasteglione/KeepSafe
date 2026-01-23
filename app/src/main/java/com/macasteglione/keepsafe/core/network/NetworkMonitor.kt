package com.macasteglione.keepsafe.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * Network connectivity monitor for VPN reconnection.
 *
 * Monitors network state changes and triggers VPN reconnection when
 * network connectivity changes occur. This ensures the VPN tunnel
 * remains active across network transitions (WiFi to mobile data, etc.).
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
     *
     * Registers a network callback that listens for internet-capable
     * networks (WiFi and cellular) and triggers reconnection when
     * network changes are detected.
     */
    fun startMonitoring() {
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            /**
             * Called when a new network becomes available.
             */
            override fun onAvailable(network: Network) {
                handleNetworkChange(network)
            }

            /**
             * Called when a network is lost.
             */
            override fun onLost(network: Network) {
                if (currentNetwork == network) {
                    currentNetwork = null
                }
            }

            /**
             * Called when network capabilities change.
             */
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                // Check for WiFi or cellular transport (validation)
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                handleNetworkChange(network)
            }
        }

        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
    }

    /**
     * Handles network change events.
     *
     * Triggers the network change callback when the active network
     * changes, but only if there was a previous network connection.
     * This prevents unnecessary reconnections on initial connection.
     *
     * @param network The new network that became active
     */
    private fun handleNetworkChange(network: Network) {
        if (currentNetwork != null && currentNetwork != network) {
            onNetworkChanged()
        }
        currentNetwork = network
    }

    /**
     * Stops network monitoring and cleans up resources.
     *
     * Unregisters the network callback and clears all references
     * to prevent memory leaks and unnecessary callbacks.
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