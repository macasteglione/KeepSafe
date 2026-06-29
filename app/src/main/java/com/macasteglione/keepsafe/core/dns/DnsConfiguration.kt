package com.macasteglione.keepsafe.core.dns

/**
 * Global DNS and VPN configuration constants.
 *
 * Defines the NextDNS endpoints and tunnel parameters used by the application.
 * Supports both IPv4 and IPv6 for full network coverage.
 */
object DnsConfiguration {
    const val PRIMARY_DNS = "208.67.222.123"
    const val SECONDARY_DNS = "208.67.220.123"

    // VPN tunnel configuration
    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_PREFIX_LENGTH = 24
    const val VPN_SESSION_NAME = "KeepSafe DNS Protection"
    const val VPN_MTU = 1500

    // const val ROUTE_ADDRESS = "0.0.0.0"
    // const val ROUTE_PREFIX = 0

    // VPN tunnel configuration
    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_PREFIX_LENGTH = 24
    const val VPN_SESSION_NAME = "KeepSafe DNS Protection"
    const val VPN_MTU = 1400 // Balanced MTU for WiFi and Mobile Data
}
