package org.macasteglione.keepsafe.vpn

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import org.macasteglione.keepsafe.MainActivity
import org.macasteglione.keepsafe.R

class AndroidDnsVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_VPN") {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(1, buildNotification())

        val builder = Builder()
            .setSession("DNS Changer")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("208.67.222.222")
            .addDnsServer("208.67.220.220")
            .allowFamily(OsConstants.AF_INET)
            .allowFamily(OsConstants.AF_INET6)

        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        saveVpnAddress()
        return START_STICKY
    }

    private fun saveVpnAddress() {
        val vpnAddress = VpnUtils.getVpnInterfaceAddress() ?: "0.0.0.0"
        getSharedPreferences("vpn_prefs", MODE_PRIVATE).edit {
            putString("vpn_address", vpnAddress)
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "vpn_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                channelId,
                "VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN service running"
                manager.createNotificationChannel(this)
            }
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

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_content_title))
            .setContentText(getString(R.string.notification_content_description))
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

    private fun stopVpn() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        vpnInterface?.close()
        vpnInterface = null
        AndroidVpnPreferences(this).setVpnActive(false)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}