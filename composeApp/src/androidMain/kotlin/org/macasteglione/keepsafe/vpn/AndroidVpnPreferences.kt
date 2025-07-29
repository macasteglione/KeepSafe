package org.macasteglione.keepsafe.vpn

import android.content.Context
import androidx.core.content.edit

class AndroidVpnPreferences(context: Context) : VpnPreferences {
    private val prefs = context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)

    override fun setVpnActive(active: Boolean) {
        prefs.edit { putBoolean("vpn_active", active) }
    }

    override fun isVpnActive(): Boolean {
        return prefs.getBoolean("vpn_active", false)
    }
}