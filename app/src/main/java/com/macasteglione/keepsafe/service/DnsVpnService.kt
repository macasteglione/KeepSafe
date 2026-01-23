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

@SuppressLint("VpnServicePolicy")
class DnsVpnService : VpnService() {

    private val tag = "DnsVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null
    private var networkMonitor: NetworkMonitor? = null
    private var isReconnecting = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_channel"
        const val ACTION_STOP_VPN = "STOP_VPN"
        const val ACTION_RECONNECT = "RECONNECT_VPN"
    }

    override fun onCreate() {
        super.onCreate()
        setupNetworkMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_VPN -> {
                stopVpnService()
                return START_NOT_STICKY
            }

            ACTION_RECONNECT -> {
                reconnectVpn()
                return START_STICKY
            }

            else -> {
                startVpnConnection()
                return START_STICKY
            }
        }
    }

    private fun setupNetworkMonitoring() {
        networkMonitor = NetworkMonitor(this) {
            if (!isReconnecting) {
                Log.d(tag, "Cambio de red detectado, reconectando...")
                reconnectVpn()
            }
        }
        networkMonitor?.startMonitoring()
    }

    private fun startVpnConnection() {
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            establishVpnConnection()
        } catch (e: Exception) {
            Log.e(tag, "Error al iniciar VPN", e)
            stopVpnService()
        }
    }

    private fun establishVpnConnection() {
        // Cerrar conexión anterior si existe
        vpnInterface?.close()
        val (primaryDns, secondaryDns) = DnsConfiguration.getDnsServers()

        val builder = Builder()
            .setSession(DnsConfiguration.VPN_SESSION_NAME)
            .addAddress(DnsConfiguration.VPN_ADDRESS, DnsConfiguration.VPN_PREFIX_LENGTH)
            .addDnsServer(primaryDns)
            .addDnsServer(secondaryDns)
            .setMtu(DnsConfiguration.VPN_MTU)
            .setBlocking(false)

        try {
            builder.allowFamily(android.system.OsConstants.AF_INET)
            builder.allowFamily(android.system.OsConstants.AF_INET6)
        } catch (e: Exception) {
            Log.w(tag, "No se pudieron configurar familias de direcciones: ${e.message}")
        }

        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            stopVpnService()
            return
        }

        val vpnAddress = VpnStateManager.getVpnInterfaceAddress() ?: DnsConfiguration.VPN_ADDRESS
        saveVpnAddress(vpnAddress)
        VpnStateManager.setVpnActive(this, true)
    }

    private fun reconnectVpn() {
        isReconnecting = true
        Thread.sleep(500)

        try {
            establishVpnConnection()
        } catch (_: Exception) {
            Thread.sleep(2000)
            try {
                establishVpnConnection()
            } catch (e2: Exception) {
                Log.e(tag, "Fallo definitivo en la reconexión", e2)
            }
        } finally {
            isReconnecting = false
        }
    }

    private fun stopVpnService() {
        networkMonitor?.stopMonitoring()
        networkMonitor = null

        stopForeground(STOP_FOREGROUND_REMOVE)

        vpnInterface?.close()
        vpnInterface = null

        VpnStateManager.setVpnActive(this, false)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpnService()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpnService()
        super.onRevoke()
    }

    private fun saveVpnAddress(address: String) {
        val prefs = getSharedPreferences("vpn_prefs", MODE_PRIVATE)
        prefs.edit {
            putString("vpn_address", address)
            putLong("vpn_connected_time", System.currentTimeMillis())
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Protección DNS KeepSafe",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra cuando KeepSafe está protegiendo tu conexión"
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
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KeepSafe Activo")
            .setContentText("DNS protegido con OpenDNS Family Shield")
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}