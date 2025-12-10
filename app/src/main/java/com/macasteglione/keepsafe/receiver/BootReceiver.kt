package com.macasteglione.keepsafe.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.macasteglione.keepsafe.data.VpnStateManager
import com.macasteglione.keepsafe.service.DnsVpnService

/**
 * Receiver que detecta cuando el dispositivo se reinicia
 * y reactiva el VPN automáticamente si estaba activo antes
 */
class BootReceiver : BroadcastReceiver() {

    private val tag = "BootReceiver"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(tag, "Dispositivo reiniciado, verificando VPN...")

            // Verificar si el VPN estaba activo antes del reinicio
            val wasVpnActive = VpnStateManager.getVpnState(context)

            if (wasVpnActive) {
                Log.d(tag, "VPN estaba activo, reiniciando servicio...")

                // Pequeño delay para que el sistema termine de iniciar
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val serviceIntent = Intent(context, DnsVpnService::class.java)
                    context.startForegroundService(serviceIntent)
                }, 3000) // 3 segundos de delay
            } else {
                Log.d(tag, "VPN no estaba activo, no se reinicia")
            }
        }
    }
}