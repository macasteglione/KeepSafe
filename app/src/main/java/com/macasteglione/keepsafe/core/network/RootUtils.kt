package com.macasteglione.keepsafe.core.network

import android.util.Log
import com.macasteglione.keepsafe.core.dns.DnsConfiguration
import java.io.DataOutputStream
import java.io.File

/**
 * Utility object for performing Root-level operations.
 *
 * Provides methods to check for root access and apply system-level DNS redirections
 * using iptables and system settings.
 */
object RootUtils {
    private const val TAG = "RootUtils"

    /**
     * Checks if root access (su) is available on the device.
     *
     * @return true if 'su' binary is found in common system paths.
     */
    fun isRootAvailable(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/sd/xbin/su")
        return paths.any { File(it).exists() }
    }

    /**
     * Applies a definitive DNS redirection using iptables.
     *
     * This method:
     * 1. Disables Android's native Private DNS to avoid conflicts.
     * 2. Redirects IPv4 DNS traffic (port 53) to NextDNS.
     * 3. Redirects IPv6 DNS traffic (port 53) to NextDNS.
     *
     * This bypasses ISP restrictions and prevents "No internet" errors on mobile data.
     */
    fun applyRootDnsFix() {
        if (!isRootAvailable()) return

        val primaryV4 = DnsConfiguration.PRIMARY_DNS
        val secondaryV4 = DnsConfiguration.SECONDARY_DNS
        val primaryV6 = DnsConfiguration.PRIMARY_DNS_IPV6
        val secondaryV6 = DnsConfiguration.SECONDARY_DNS_IPV6

        val commands = listOf(
            // Disable Private DNS
            "settings put global private_dns_mode off",
            "settings put global private_dns_specifier \"\"",
            
            // IPv4 Redirection (Primary & Secondary for redundancy)
            "iptables -t nat -F OUTPUT 2>/dev/null",
            "iptables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination $primaryV4:53",
            "iptables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination $primaryV4:53",
            "iptables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination $secondaryV4:53",
            "iptables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination $secondaryV4:53",
            
            // IPv6 Redirection (Crucial for 4G/LTE)
            "ip6tables -t nat -F OUTPUT 2>/dev/null",
            "ip6tables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination [$primaryV6]:53",
            "ip6tables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination [$primaryV6]:53",
            "ip6tables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination [$secondaryV6]:53",
            "ip6tables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination [$secondaryV6]:53"
        )

        executeCommands(commands)
        Log.d(TAG, "✅ Root DNS redirection rules applied")
    }

    /**
     * Clears root-applied DNS settings and restores system defaults.
     */
    fun clearRootDnsSettings() {
        if (!isRootAvailable()) return
        
        val commands = listOf(
            "settings put global private_dns_mode opportunistic",
            "iptables -t nat -F OUTPUT 2>/dev/null",
            "ip6tables -t nat -F OUTPUT 2>/dev/null"
        )

        executeCommands(commands)
        Log.d(TAG, "✅ Root settings restored to defaults")
    }

    /**
     * Internal helper to execute a list of commands as root.
     */
    private fun executeCommands(commands: List<String>) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (command in commands) {
                os.writeBytes("$command\n")
            }
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute root commands: ${e.message}")
        }
    }
}
