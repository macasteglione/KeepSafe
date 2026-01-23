package com.macasteglione.keepsafe.core.dns

/**
 * DNS configuration constants and utilities.
 *
 * Defines the DNS servers used for content filtering and VPN tunnel
 * configuration parameters. Currently configured to use CleanBrowsing
 * family-friendly DNS servers for parental control.
 *
 * Alternative DNS providers (commented out):
 * - OpenDNS Family Shield: Blocks adult content
 * - CleanBrowsing: Family-friendly filtering
 */
object DnsConfiguration {

    // DNS server configuration - CleanBrowsing Family Filter
    // Provides content filtering for adult and malicious content
    const val PRIMARY_DNS = "185.228.168.168"
    const val SECONDARY_DNS = "185.228.169.168"

    // VPN tunnel configuration
    const val VPN_ADDRESS = "10.0.0.2"          // Local VPN interface IP
    const val VPN_PREFIX_LENGTH = 24            // Subnet prefix length
    const val VPN_SESSION_NAME = "KeepSafe DNS Protection"  // VPN session name
    const val VPN_MTU = 1500                    // Maximum transmission unit

    // Route configuration (currently unused - routes all traffic)
    // const val ROUTE_ADDRESS = "0.0.0.0"
    // const val ROUTE_PREFIX = 0

    /**
     * Returns the configured DNS server pair.
     *
     * @return Pair of primary and secondary DNS server addresses
     */
    fun getDnsServers(): Pair<String, String> {
        return Pair(PRIMARY_DNS, SECONDARY_DNS)
    }
}