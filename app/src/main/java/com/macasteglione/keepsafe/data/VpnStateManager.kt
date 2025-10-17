package com.macasteglione.keepsafe.data

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.edit
import com.macasteglione.keepsafe.service.DnsVpnService
import kotlinx.coroutines.runBlocking

object VpnStateManager {

    private const val PREFS_NAME = "vpn_prefs"
    private const val KEY_VPN_ACTIVE = "vpn_active"
    private const val KEY_VPN_ADDRESS = "vpn_address"
    private const val KEY_VPN_START_TIME = "vpn_start_time"

    fun setVpnActive(context: Context, active: Boolean) = runBlocking {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_VPN_ACTIVE, active)
            if (active) {
                putLong(KEY_VPN_START_TIME, System.currentTimeMillis())
            } else {
                remove(KEY_VPN_START_TIME)
                remove(KEY_VPN_ADDRESS)
            }
        }
    }

    fun isVpnReallyActive(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        return runningServices.any {
            it.service.className == DnsVpnService::class.java.name
        }
    }

    fun getVpnState(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VPN_ACTIVE, false)
    }

    fun getVpnInterfaceAddress(): String? {
        return try {
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
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getSavedVpnAddress(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VPN_ADDRESS, "No disponible") ?: "No disponible"
    }

    fun getVpnUptime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val startTime = prefs.getLong(KEY_VPN_START_TIME, 0)
        return if (startTime > 0) {
            System.currentTimeMillis() - startTime
        } else {
            0L
        }
    }

    fun clearVpnData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            clear()
        }
    }
}