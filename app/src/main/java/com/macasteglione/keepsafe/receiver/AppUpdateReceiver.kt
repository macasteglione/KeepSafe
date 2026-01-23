package com.macasteglione.keepsafe.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.macasteglione.keepsafe.data.VpnStateManager
import com.macasteglione.keepsafe.service.DnsVpnService
import com.macasteglione.keepsafe.ui.UiConstants

/**
 * Detecta cuando la app se actualiza y reactiva el VPN automáticamente
 */
class AppUpdateReceiver : BroadcastReceiver() {

    private val tag = "AppUpdateReceiver"



    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // La app fue actualizada (reemplazada)
                // Verificar si el VPN estaba activo antes
                Handler(Looper.getMainLooper()).postDelayed({
                    restartVpnIfNeeded(context)
                }, UiConstants.VPN_RESTART_DELAY_MS)
            }

            Intent.ACTION_PACKAGE_REPLACED -> {
                val packageName = intent.data?.schemeSpecificPart

                if (packageName == context.packageName) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        restartVpnIfNeeded(context)
                    }, UiConstants.VPN_RESTART_DELAY_MS)
                }
            }
        }
    }

    private fun restartVpnIfNeeded(context: Context) {
        // Verificar si el VPN debería estar activo
        val shouldBeActive = VpnStateManager.getVpnState(context)

        if (shouldBeActive) {
            // Verificar permiso VPN
            val vpnIntent = VpnService.prepare(context)

            if (vpnIntent == null) {
                // Tenemos permiso, iniciar VPN
                val serviceIntent = Intent(context, DnsVpnService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                // Mostrar notificación recordatoria
                showReactivationNotification(context)
            }
        } else {
            Log.d(tag, "VPN no estaba activo, no se reactiva")
        }
    }

    private fun showReactivationNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager

        val channelId = "vpn_reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Recordatorios VPN",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, com.macasteglione.keepsafe.ui.MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.macasteglione.keepsafe.R.drawable.ic_vpn)
            .setContentTitle("Reactiva KeepSafe")
            .setContentText("La protección VPN se detuvo. Toca para reactivar.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(UiConstants.REACTIVATION_NOTIFICATION_ID, notification)
    }
}