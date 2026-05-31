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
import com.macasteglione.keepsafe.R
import com.macasteglione.keepsafe.core.dns.DnsConfiguration
import com.macasteglione.keepsafe.core.dns.NextDnsLinkedIpUpdater
import com.macasteglione.keepsafe.core.network.NetworkMonitor
import com.macasteglione.keepsafe.core.network.RootUtils
import com.macasteglione.keepsafe.data.VpnStateManager
import com.macasteglione.keepsafe.ui.MainActivity
import com.macasteglione.keepsafe.ui.UiConstants
import kotlin.concurrent.thread

/**
 * Background service that manages the DNS VPN tunnel.
 *
 * This service handles the lifecycle of the VpnService, including establishment,
 * reconnection on network changes, and integration with Root-level DNS fixes.
 */
@SuppressLint("VpnServicePolicy")
class DnsVpnService : VpnService() {

    private val tag = "DnsVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null
    private var networkMonitor: NetworkMonitor? = null
    private var isReconnecting = false

    override fun onCreate() {
        super.onCreate()
        setupNetworkMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Immediate foreground notification to comply with Android 8.0+ requirements
        startForeground(UiConstants.NOTIFICATION_ID, buildNotification())

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
                if (!VpnStateManager.isVpnReallyActive(this) || vpnInterface == null) {
                    thread { establishVpnConnection() }
                }
                return START_STICKY
            }
        }
    }

    private fun setupNetworkMonitoring() {
        networkMonitor = NetworkMonitor(this) {
            if (!isReconnecting) {
                reconnectVpn()
            }
        }
        networkMonitor?.startMonitoring()
    }

    /**
     * Configures and establishes the VPN tunnel.
     * Uses a transparent approach where DNS is redirected via Root iptables
     * to avoid "No Internet" system errors while keeping the VPN icon active.
     */
    private fun establishVpnConnection() {
        try {
            vpnInterface?.close()

            // Apply Root Fix in background (intercepts port 53 traffic)
            thread { RootUtils.applyRootDnsFix() }

            val builder = Builder().apply {
                setSession(DnsConfiguration.VPN_SESSION_NAME)
                addAddress(DnsConfiguration.VPN_ADDRESS, DnsConfiguration.VPN_PREFIX_LENGTH)
                setMtu(DnsConfiguration.VPN_MTU)
                setBlocking(false)
                
                // Configure address families for better compatibility
                try {
                    allowFamily(android.system.OsConstants.AF_INET)
                    allowFamily(android.system.OsConstants.AF_INET6)
                } catch (_: Exception) {}

                // Disallow this app to avoid loops when updating Linked IP
                try {
                    addDisallowedApplication(packageName)
                } catch (_: Exception) {}
            }

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                VpnStateManager.setVpnActive(this, true)
                Log.d(tag, "✅ VPN established (Transparent DNS mode)")
                NextDnsLinkedIpUpdater.updateLinkedIp()
            }

        } catch (e: Exception) {
            Log.e(tag, "Failed to establish VPN: ${e.message}")
        }
    }

    private fun reconnectVpn() {
        if (isReconnecting) return
        isReconnecting = true
        Log.w(tag, "Reconnecting VPN due to network change...")

        thread {
            try {
                vpnInterface?.close()
                vpnInterface = null
                Thread.sleep(1000)
                establishVpnConnection()
                Thread.sleep(1000)
            } catch (e: Exception) {
                Log.e(tag, "Reconnection failed: ${e.message}")
            } finally {
                isReconnecting = false
            }
        }
    }

    private fun stopVpnService() {
        networkMonitor?.stopMonitoring()
        
        // Clean up root rules when stopping
        thread { RootUtils.clearRootDnsSettings() }

        stopForeground(true)
        vpnInterface?.close()
        vpnInterface = null
        VpnStateManager.setVpnActive(this, false)
        stopSelf()
        Log.d(tag, "VPN service stopped and Root rules cleared")
    }

    override fun onDestroy() {
        stopVpnService()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpnService()
        super.onRevoke()
    }

    /**
     * Builds the foreground notification for the VPN service.
     */
    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.CHANNEL_ID,
                "KeepSafe Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN tunnel protecting your DNS queries"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, UiConstants.CHANNEL_ID)
            .setContentTitle("KeepSafe Active")
            .setContentText("Your connection is protected by NextDNS")
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
