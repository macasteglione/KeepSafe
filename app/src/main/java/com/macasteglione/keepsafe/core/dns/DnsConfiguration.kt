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

    const val PRIMARY_DNS = "208.67.222.123"
    const val SECONDARY_DNS = "208.67.220.123"

    // VPN tunnel configuration
    const val VPN_ADDRESS = "10.0.0.2"
    const val VPN_PREFIX_LENGTH = 24
    const val VPN_SESSION_NAME = "KeepSafe DNS Protection"
    const val VPN_MTU = 1500

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