package org.macasteglione.keepsafe.deviceAdmin

class DesktopDeviceAdminHandler : DeviceAdminHandler {
    override fun onPasswordFailed() {
        println("⚠️ No hay admin device en Desktop. Acción ignorada.")
    }
}