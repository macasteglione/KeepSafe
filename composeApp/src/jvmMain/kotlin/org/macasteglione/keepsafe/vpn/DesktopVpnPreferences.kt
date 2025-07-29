package org.macasteglione.keepsafe.vpn

import java.util.prefs.Preferences

class DesktopVpnPreferences : VpnPreferences {
    private val prefs = Preferences.userRoot().node("vpn_prefs")

    override fun setVpnActive(active: Boolean) {
        prefs.putBoolean("vpn_active", active)
    }

    override fun isVpnActive(): Boolean {
        return prefs.getBoolean("vpn_active", false)
    }
}