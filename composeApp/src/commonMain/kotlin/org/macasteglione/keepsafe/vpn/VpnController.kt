package org.macasteglione.keepsafe.vpn

interface VpnController {
    fun startVpn()
    fun stopVpn()
    fun isVpnRunning(): Boolean
    fun requestDeviceAdmin()
}
