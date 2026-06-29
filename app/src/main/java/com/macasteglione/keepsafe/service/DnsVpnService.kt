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

    private fun establishVpnConnection() {
        try {
            vpnInterface?.close()

            val (primaryDns, secondaryDns) = DnsConfiguration.getDnsServers()

            thread { RootUtils.applyRootDnsFix() }

            val builder = Builder()
                .setSession(DnsConfiguration.VPN_SESSION_NAME)
                .addAddress(DnsConfiguration.VPN_ADDRESS, DnsConfiguration.VPN_PREFIX_LENGTH)
                .addDnsServer(primaryDns)
                .addDnsServer(secondaryDns)
                .setMtu(DnsConfiguration.VPN_MTU)
                .setBlocking(false)

            configureAddressFamilies(builder)

            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {}

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                VpnStateManager.setVpnActive(this, true)
                Log.d(tag, "VPN established (Transparent DNS mode)")
                NextDnsLinkedIpUpdater.updateLinkedIp()

                val vpnAddress = VpnStateManager.getVpnInterfaceAddress() ?: DnsConfiguration.VPN_ADDRESS
                saveVpnAddress(vpnAddress)
                Log.d(tag, "VPN connection established successfully with address: $vpnAddress")
            }

        } catch (e: SecurityException) {
            Log.e(tag, "Security exception during VPN establishment: ${e.message}")
            stopVpnService()
        } catch (e: IllegalStateException) {
            Log.e(tag, "Illegal state during VPN establishment: ${e.message}")
            stopVpnService()
        } catch (e: Exception) {
            Log.e(tag, "Failed to establish VPN: ${e.message}")
        }
    }

    private fun configureAddressFamilies(builder: Builder) {
        try {
            builder.allowFamily(android.system.OsConstants.AF_INET)
            builder.allowFamily(android.system.OsConstants.AF_INET6)
        } catch (_: Exception) {}
    }

    private fun saveVpnAddress(address: String) {
        val prefs = getSharedPreferences("vpn_prefs", MODE_PRIVATE)
        prefs.edit().putString("vpn_address", address).apply()
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

    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.CHANNEL_ID,
                "KeepSafe Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when KeepSafe is protecting your connection"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, UiConstants.CHANNEL_ID)
            .setContentTitle("KeepSafe Active")
            .setContentText("Your connection is protected by OpenDNS Family Shield")
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
