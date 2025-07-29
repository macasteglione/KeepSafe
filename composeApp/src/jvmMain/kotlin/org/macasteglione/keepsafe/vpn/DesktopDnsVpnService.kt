package org.macasteglione.keepsafe.vpn

import javax.swing.JOptionPane

class DesktopDnsVpnService {
    private var vpnRunning: Boolean = false

    fun startVpn() {
        if (vpnRunning) {
            println("VPN ya está corriendo.")
            return
        }

        println("⏳ Iniciando VPN (simulado)...")

        // Aquí podrías invocar procesos reales si querés (ej: OpenVPN, WireGuard)
        vpnRunning = true

        // Guardamos la IP (simulada o real)
        saveVpnAddress()

        // Mostrar notificación (popup básico)
        showNotification("VPN iniciada", "La VPN DNS ahora está activa.")
    }

    fun stopVpn() {
        if (!vpnRunning) return

        println("🛑 Deteniendo VPN (simulado)...")
        vpnRunning = false

        // Limpiar preferencias
        DesktopVpnPreferences().setVpnActive(false)

        showNotification("VPN detenida", "La VPN DNS fue desactivada.")
    }

    private fun saveVpnAddress() {
        val vpnAddress = VpnUtils.getVpnInterfaceAddress() ?: "0.0.0.0"
        DesktopVpnPreferences().setVpnActive(true)
        println("Dirección de interfaz VPN: $vpnAddress")
    }

    private fun showNotification(title: String, message: String) {
        // Usamos un simple JOptionPane como notificación emergente
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE)
    }
}