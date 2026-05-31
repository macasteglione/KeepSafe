package com.macasteglione.keepsafe.core.dns

/**
 * Global DNS and VPN configuration constants.
 *
 * Defines the NextDNS endpoints and tunnel parameters used by the application.
 * Supports both IPv4 and IPv6 for full network coverage.
 */
object DnsConfiguration {

    // NextDNS Endpoints (Profile: 4162c1)
    const val PRIMARY_DNS = "45.90.28.155"
    const val SECONDARY_DNS = "45.90.30.155"

    // NextDNS IPv6 - Essential for modern mobile networks (4G/LTE/5G)
    const val PRIMARY_DNS_IPV6 = "2a07:a8c0::41:62c1"
    const val SECONDARY_DNS_IPV6 = "2a07:a8c1::41:62c1"

    // NextDNS DoT Hostname (for Private DNS settings)
    const val DOT_HOSTNAME = "4162c1.dns.nextdns.io"

    // VPN tunnel configuration
    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_PREFIX_LENGTH = 24
    const val VPN_SESSION_NAME = "KeepSafe DNS Protection"
    const val VPN_MTU = 1400 // Balanced MTU for WiFi and Mobile Data
}
