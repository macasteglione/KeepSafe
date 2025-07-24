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
}