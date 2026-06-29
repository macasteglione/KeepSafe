package com.macasteglione.keepsafe.core.network

import android.util.Log
import com.macasteglione.keepsafe.core.dns.DnsConfiguration
import java.io.DataOutputStream
import java.io.File

object RootUtils {
    private const val TAG = "RootUtils"

    fun isRootAvailable(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/sd/xbin/su")
        return paths.any { File(it).exists() }
    }

    fun applyRootDnsFix() {
        if (!isRootAvailable()) return

        val primaryV4 = DnsConfiguration.PRIMARY_DNS
        val secondaryV4 = DnsConfiguration.SECONDARY_DNS
        val primaryV6 = DnsConfiguration.PRIMARY_DNS_IPV6
        val secondaryV6 = DnsConfiguration.SECONDARY_DNS_IPV6

        val commands = listOf(
            "settings put global private_dns_mode off",
            "settings put global private_dns_specifier \"\"",

            "iptables -t nat -F OUTPUT 2>/dev/null",
            "iptables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination $primaryV4:53",
            "iptables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination $primaryV4:53",
            "iptables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination $secondaryV4:53",
            "iptables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination $secondaryV4:53",

            "ip6tables -t nat -F OUTPUT 2>/dev/null",
            "ip6tables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination [$primaryV6]:53",
            "ip6tables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination [$primaryV6]:53",
            "ip6tables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination [$secondaryV6]:53",
            "ip6tables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination [$secondaryV6]:53"
        )

        executeCommands(commands)
        Log.d(TAG, "Root DNS redirection rules applied")
    }

    fun clearRootDnsSettings() {
        if (!isRootAvailable()) return

        val commands = listOf(
            "settings put global private_dns_mode opportunistic",
            "iptables -t nat -F OUTPUT 2>/dev/null",
            "ip6tables -t nat -F OUTPUT 2>/dev/null"
        )

        executeCommands(commands)
        Log.d(TAG, "Root settings restored to defaults")
    }

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
