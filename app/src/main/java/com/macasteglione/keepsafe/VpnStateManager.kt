package com.macasteglione.keepsafe

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.runBlocking

object VpnStateManager {
    fun setVpnActive(context: Context, active: Boolean) = runBlocking {
        val prefs = context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
        prefs.edit { putBoolean("vpn_active", active) }
    }

    fun isVpnReallyActive(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)

        return runningServices.any { it.service.className == DnsVpnService::class.java.name }
    }

    fun getVpnInterfaceAddress(): String? {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        for (networkInterface in interfaces) {
            if (networkInterface.name.startsWith("tun")) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        }
        return null
    }
}