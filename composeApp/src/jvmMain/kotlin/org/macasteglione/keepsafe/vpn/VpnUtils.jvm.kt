package org.macasteglione.keepsafe.vpn

actual object VpnUtils {
    actual fun getVpnInterfaceAddress(): String? {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        for (networkInterface in interfaces) {
            if (networkInterface.name.startsWith("tun")) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        }
        return null
    }
}