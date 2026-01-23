package com.macasteglione.keepsafe.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.macasteglione.keepsafe.R
import com.macasteglione.keepsafe.core.dns.DnsConfiguration
import com.macasteglione.keepsafe.core.network.NetworkMonitor
import com.macasteglione.keepsafe.data.VpnStateManager
import com.macasteglione.keepsafe.ui.MainActivity
import com.macasteglione.keepsafe.ui.UiConstants

/**
 * DNS VPN Service that establishes a local VPN connection for DNS filtering.
 *
 * This service creates a VPN interface that intercepts DNS requests and routes them
 * through configured DNS servers (OpenDNS Family Shield). It provides persistent
 * content filtering protection and automatically reconnects on network changes.
 *
 * Key features:
 * - Establishes VPN tunnel for DNS traffic interception
 * - Monitors network changes and reconnects automatically
 * - Runs as foreground service with persistent notification
 * - Provides secure DNS filtering for parental control
 */
@SuppressLint("VpnServicePolicy")
class DnsVpnService : VpnService() {

    private val tag = "DnsVpnService"

    // VPN interface management
    private var vpnInterface: ParcelFileDescriptor? = null

    // Network monitoring for reconnection
    private var networkMonitor: NetworkMonitor? = null

    // Flag to prevent multiple simultaneous reconnection attempts
    private var isReconnecting = false

    companion object {
        // Constants moved to UiConstants for centralization
    }

    /**
     * Called when the service is first created.
     *
     * Initializes network monitoring to handle automatic reconnection
     * when network conditions change.
     */
    override fun onCreate() {
        super.onCreate()
        setupNetworkMonitoring()
    }

    /**
     * Handles start commands for different VPN operations.
     *
     * Processes different action intents:
     * - ACTION_STOP_VPN: Stops the VPN service
     * - ACTION_RECONNECT: Forces VPN reconnection
     * - Default: Starts new VPN connection
     *
     * @param intent The intent containing the action
     * @param flags Additional flags
     * @param startId Unique ID for this start request
     * @return Service start mode (STICKY to restart on system kill)
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            UiConstants.ACTION_STOP_VPN -> {
                stopVpnService()
                return START_NOT_STICKY
            }

            UiConstants.ACTION_RECONNECT -> {
                reconnectVpn()
                return START_STICKY
            }

            else -> {
                startVpnConnection()
                return START_STICKY
            }
        }
    }

    /**
     * Sets up network monitoring for automatic VPN reconnection.
     *
     * Creates a NetworkMonitor instance that watches for network changes
     * and triggers VPN reconnection when connectivity changes occur.
     */
    private fun setupNetworkMonitoring() {
        networkMonitor = NetworkMonitor(this) {
            if (!isReconnecting) {
                Log.d(tag, "Network change detected, reconnecting...")
                reconnectVpn()
            }
        }
        networkMonitor?.startMonitoring()
    }

    /**
     * Starts the VPN connection process as a foreground service.
     *
     * This method:
     * 1. Promotes service to foreground with notification
     * 2. Establishes the VPN connection
     * 3. Handles any connection errors gracefully
     */
    private fun startVpnConnection() {
        try {
            startForeground(UiConstants.NOTIFICATION_ID, buildNotification())
            establishVpnConnection()
        } catch (e: Exception) {
            Log.e(tag, "Error starting VPN", e)
            stopVpnService()
        }
    }

    /**
     * Establishes the actual VPN connection using Android VpnService.
     *
     * Creates a VPN interface that routes DNS traffic through configured
     * DNS servers. Sets up the VPN tunnel with proper configuration
     * for both IPv4 and IPv6 traffic.
     */
    private fun establishVpnConnection() {
        try {
            // Close any existing connection
            vpnInterface?.close()

            // Get DNS server configuration
            val (primaryDns, secondaryDns) = DnsConfiguration.getDnsServers()

            // Build VPN configuration
            val builder = Builder()
                .setSession(DnsConfiguration.VPN_SESSION_NAME)
                .addAddress(DnsConfiguration.VPN_ADDRESS, DnsConfiguration.VPN_PREFIX_LENGTH)
                .addDnsServer(primaryDns)
                .addDnsServer(secondaryDns)
                .setMtu(DnsConfiguration.VPN_MTU)
                .setBlocking(false) // Non-blocking mode for better performance

            // Configure address families (IPv4 and IPv6 support)
            configureAddressFamilies(builder)

            // Establish the VPN interface
            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(tag, "Failed to establish VPN interface - permission may be required")
                stopVpnService()
                return
            }

            // Save VPN address for UI display and update state
            val vpnAddress = VpnStateManager.getVpnInterfaceAddress() ?: DnsConfiguration.VPN_ADDRESS
            saveVpnAddress(vpnAddress)
            VpnStateManager.setVpnActive(this, true)

            Log.d(tag, "VPN connection established successfully with address: $vpnAddress")

        } catch (e: SecurityException) {
            Log.e(tag, "Security exception during VPN establishment: ${e.message}")
            stopVpnService()
        } catch (e: IllegalStateException) {
            Log.e(tag, "Illegal state during VPN establishment: ${e.message}")
            stopVpnService()
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error during VPN establishment: ${e.message}", e)
            stopVpnService()
        }
    }

    /**
     * Configures address families for the VPN builder.
     *
     * Attempts to configure both IPv4 and IPv6 support. Gracefully handles
     * cases where certain address families are not supported.
     *
     * @param builder The VPN builder to configure
     */
    private fun configureAddressFamilies(builder: Builder) {
        try {
            builder.allowFamily(android.system.OsConstants.AF_INET)
            Log.d(tag, "IPv4 address family configured")
        } catch (e: Exception) {
            Log.w(tag, "Could not configure IPv4 address family: ${e.message}")
        }

        try {
            builder.allowFamily(android.system.OsConstants.AF_INET6)
            Log.d(tag, "IPv6 address family configured")
        } catch (e: Exception) {
            Log.w(tag, "Could not configure IPv6 address family: ${e.message}")
        }
    }

    /**
     * Reconnects the VPN connection with retry logic.
     *
     * Implements a robust reconnection mechanism that:
     * 1. Sets reconnection flag to prevent multiple attempts
     * 2. Waits briefly before attempting reconnection
     * 3. Retries once more after a longer delay if first attempt fails
     * 4. Logs definitive failures
     */
    private fun reconnectVpn() {
        isReconnecting = true
        Thread.sleep(500) // Brief pause before reconnection

        try {
            establishVpnConnection()
        } catch (_: Exception) {
            // First attempt failed, wait longer and try again
            Thread.sleep(2000)
            try {
                establishVpnConnection()
            } catch (e2: Exception) {
                Log.e(tag, "Definitive reconnection failure", e2)
            }
        } finally {
            isReconnecting = false
        }
    }

    /**
     * Stops the VPN service and cleans up resources.
     *
     * Performs complete cleanup:
     * 1. Stops network monitoring
     * 2. Removes foreground notification
     * 3. Closes VPN interface
     * 4. Updates VPN state to inactive
     * 5. Stops the service
     */
    private fun stopVpnService() {
        // Stop network monitoring
        networkMonitor?.stopMonitoring()
        networkMonitor = null

        // Remove foreground notification
        stopForeground(true)

        // Close VPN interface
        vpnInterface?.close()
        vpnInterface = null

        // Update state and stop service
        VpnStateManager.setVpnActive(this, false)
        stopSelf()

        Log.d(tag, "VPN service stopped")
    }

    /**
     * Called when the service is being destroyed.
     *
     * Ensures proper cleanup of VPN resources before service termination.
     */
    override fun onDestroy() {
        stopVpnService()
        super.onDestroy()
    }

    /**
     * Called when VPN permission is revoked by the system or user.
     *
     * Immediately stops the VPN service when permission is revoked.
     */
    override fun onRevoke() {
        stopVpnService()
        super.onRevoke()
    }

    /**
     * Saves VPN interface address and connection timestamp.
     *
     * Stores the VPN address for UI display and records when the
     * connection was established for uptime tracking.
     *
     * @param address The VPN interface IP address
     */
    private fun saveVpnAddress(address: String) {
        val prefs = getSharedPreferences("vpn_prefs", MODE_PRIVATE)
        prefs.edit {
            putString("vpn_address", address)
            putLong("vpn_connected_time", System.currentTimeMillis())
        }
    }

    /**
     * Builds and returns the foreground service notification.
     *
     * Creates a persistent notification that shows while the VPN service is active.
     * The notification provides user feedback about the protection status and
     * allows quick access to the main app.
     *
     * @return Configured notification for foreground service
     */
    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.CHANNEL_ID,
                "Protección DNS KeepSafe",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra cuando KeepSafe está protegiendo tu conexión"
                setShowBadge(false) // Don't show badge on app icon
                enableLights(false) // No notification lights
                enableVibration(false) // No vibration for ongoing service
            }
            manager.createNotificationChannel(channel)
        }

        // Create intent to open main activity when notification tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        return NotificationCompat.Builder(this, UiConstants.CHANNEL_ID)
            .setContentTitle("KeepSafe Activo")
            .setContentText("DNS protegido con OpenDNS Family Shield")
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false) // Cannot be dismissed by user
            .setOngoing(true) // Ongoing service indicator
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}