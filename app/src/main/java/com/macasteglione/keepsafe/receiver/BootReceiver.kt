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

class BootReceiver : BroadcastReceiver() {
    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "Broadcast recibido: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(tag, "Dispositivo iniciado, verificando VPN...")

                val wasVpnActive = VpnStateManager.getVpnState(context)

                Log.d(tag, "Estado VPN guardado: $wasVpnActive")

                if (wasVpnActive) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startVpnService(context)
                    }, UiConstants.VPN_START_DELAY_MS)
                }
            }
        }
    }

    private fun startVpnService(context: Context) {
        try {
            val vpnIntent = VpnService.prepare(context)

            if (vpnIntent == null) {
                Log.d(tag, "Permiso VPN ya concedido, iniciando servicio...")

                val serviceIntent = Intent(context, DnsVpnService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                Log.d(tag, "Servicio VPN iniciado")
            } else {
                Log.w(tag, "Se necesita permiso VPN, no se puede auto-iniciar")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error al iniciar VPN: ${e.message}", e)
        }
    }
}