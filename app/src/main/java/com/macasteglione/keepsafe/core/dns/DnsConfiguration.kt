package com.macasteglione.keepsafe.core.dns

object DnsConfiguration {

    const val PRIMARY_DNS = "208.67.222.123"
    const val SECONDARY_DNS = "208.67.220.123"

    const val PRIMARY_DNS_IPV6 = "2620:119:35::123"
    const val SECONDARY_DNS_IPV6 = "2620:119:53::123"

    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_PREFIX_LENGTH = 24
    const val VPN_SESSION_NAME = "KeepSafe DNS Protection"
    const val VPN_MTU = 1400

    fun getDnsServers(): Pair<String, String> {
        return Pair(PRIMARY_DNS, SECONDARY_DNS)
    }
}
