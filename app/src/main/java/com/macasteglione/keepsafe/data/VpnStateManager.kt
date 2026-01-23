package com.macasteglione.keepsafe.data

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.edit
import com.macasteglione.keepsafe.service.DnsVpnService
import kotlinx.coroutines.runBlocking

/**
 * VPN state management utility class.
 *
 * Handles persistence and retrieval of VPN connection state, service status,
 * and connection information. Uses SharedPreferences for state storage
 * and system services for real-time status checking.
 */
object VpnStateManager {

    // SharedPreferences keys for VPN state storage
    private const val PREFS_NAME = "vpn_prefs"
    private const val KEY_VPN_ACTIVE = "vpn_active"
    private const val KEY_VPN_ADDRESS = "vpn_address"
    private const val KEY_VPN_START_TIME = "vpn_start_time"

    /**
     * Sets the VPN active state and manages related metadata.
     *
     * Updates the VPN active flag and manages connection timestamps.
     * When activating, records the start time. When deactivating,
     * clears address and timing data.
     *
     * @param context Android context for preferences access
     * @param active Whether VPN should be marked as active
     */
    fun setVpnActive(context: Context, active: Boolean) = runBlocking {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_VPN_ACTIVE, active)
            if (active) {
                putLong(KEY_VPN_START_TIME, System.currentTimeMillis())
            } else {
                remove(KEY_VPN_START_TIME)
                remove(KEY_VPN_ADDRESS)
            }
        }
    }

    /**
     * Checks if the VPN service is actually running in the system.
     *
     * Queries the ActivityManager for running services to verify
     * that the DnsVpnService is currently active, providing
     * real-time status rather than cached state.
     *
     * @param context Android context for system service access
     * @return true if VPN service is running, false otherwise
     */
    fun isVpnReallyActive(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        return runningServices.any {
            it.service.className == DnsVpnService::class.java.name
        }
    }

    /**
     * Gets the cached VPN active state from preferences.
     *
     * Returns the stored VPN state, which may not reflect real-time
     * service status. Use isVpnReallyActive() for current status.
     *
     * @param context Android context for preferences access
     * @return true if VPN was marked as active, false otherwise
     */
    fun getVpnState(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VPN_ACTIVE, false)
    }

    /**
     * Retrieves the VPN interface IP address from system network interfaces.
     *
     * Scans network interfaces for TUN (VPN) interfaces and returns
     * the IPv4 address. Used for displaying connection information
     * in the UI.
     *
     * @return VPN interface IP address, or null if not found
     */
    fun getVpnInterfaceAddress(): String? {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                // Look for TUN interfaces (VPN tunnels)
                if (networkInterface.name.startsWith("tun")) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        // Return first non-loopback IPv4 address
                        if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                            return addr.hostAddress
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            // Return null on any error (permission issues, etc.)
            null
        }
    }

    /**
     * Gets the saved VPN address from preferences.
     *
     * Returns the cached VPN interface address for UI display.
     * Falls back to "No disponible" if no address is stored.
     *
     * @param context Android context for preferences access
     * @return VPN address string for display
     */
    fun getSavedVpnAddress(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VPN_ADDRESS, "No disponible") ?: "No disponible"
    }

}