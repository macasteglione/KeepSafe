package org.macasteglione.keepsafe.vpn

interface VpnPreferences {
    fun setVpnActive(active: Boolean)
    fun isVpnActive(): Boolean
}