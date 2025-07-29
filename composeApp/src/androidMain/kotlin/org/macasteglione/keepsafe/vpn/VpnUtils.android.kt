package org.macasteglione.keepsafe.vpn

import java.net.NetworkInterface

actual object VpnUtils {
    actual fun getVpnInterfaceAddress(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (networkInterface in interfaces) {
            if (networkInterface.name.startsWith("tun")) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        }
        return null
    }

    actual fun ping(host: String): Int {
        val start = System.currentTimeMillis()
        val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 $host")
        val result = process.waitFor()
        val end = System.currentTimeMillis()
        return if (result == 0) (end - start).toInt() else -1
    }
}