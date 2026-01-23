package com.macasteglione.keepsafe.core.dns

object DnsConfiguration {
    // OpenDNS
    //const val PRIMARY_DNS = "208.67.222.123"
    //const val SECONDARY_DNS = "208.67.220.123"

    // CleanBrowsing
    const val PRIMARY_DNS = "185.228.168.168"
    const val SECONDARY_DNS = "185.228.169.168"

    // Configuración VPN
    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_PREFIX_LENGTH = 24
    const val VPN_SESSION_NAME = "KeepSafe DNS Protection"
    const val VPN_MTU = 1500

    // Configuración de rutas
    //const val ROUTE_ADDRESS = "0.0.0.0"
    //const val ROUTE_PREFIX = 0

    fun getDnsServers(): Pair<String, String> {
        return Pair(PRIMARY_DNS, SECONDARY_DNS)
    }
}