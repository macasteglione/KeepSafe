package org.macasteglione.keepsafe.vpn

expect object VpnUtils {
    fun getVpnInterfaceAddress(): String?
    fun ping(host: String): Int
}