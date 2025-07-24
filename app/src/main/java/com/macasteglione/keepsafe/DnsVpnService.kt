package com.macasteglione.keepsafe

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat

class DnsVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_VPN") {
            Log.d("DnsVpnService", "Deteniendo servicio VPN")
            stopForeground(STOP_FOREGROUND_REMOVE)
            vpnInterface?.close()
            vpnInterface = null
            VpnStateManager.setVpnActive(this, false)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(1, buildNotification())

        val builder = Builder()
            .setSession("DNS Changer")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("208.67.222.222")
            .addDnsServer("208.67.220.220")
            .allowFamily(android.system.OsConstants.AF_INET)
            .allowFamily(android.system.OsConstants.AF_INET6)

        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            Log.e(
                "DnsVpnService",
                "Error: establish() devolvió null. ¿Permiso VPN aceptado?"
            )
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("DnsVpnService", "onDestroy called")
        stopForeground(STOP_FOREGROUND_REMOVE)
        vpnInterface?.close()
        vpnInterface = null
        VpnStateManager.setVpnActive(this, false)
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "vpn_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "VPN", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("KeepSafe VPN activo")
            .setContentText("Protegiendo tu DNS con OpenDNS")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .build()
    }
}
