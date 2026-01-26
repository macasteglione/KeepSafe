package com.macasteglione.keepsafe.admin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import android.widget.Toast
import com.macasteglione.keepsafe.ui.PasswordValidationActivity

/**
 * Device administrator receiver for parental control features.
 *
 * Handles device admin activation/deactivation and applies security
 * restrictions to prevent uninstallation and enforce parental controls.
 * Provides comprehensive device management capabilities for content filtering.
 */
class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    /**
     * Called when device administrator is enabled.
     *
     * Shows confirmation toast when admin privileges are granted.
     */
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "KeepSafe activado como administrador del dispositivo",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Called when device administrator is disabled.
     *
     * Launches password validation activity to confirm admin deactivation.
     * This prevents unauthorized removal of parental controls.
     */
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)

        // Launch password validation before allowing admin deactivation
        val passwordIntent = Intent(context, PasswordValidationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(passwordIntent)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "ADVERTENCIA: Desactivar KeepSafe eliminará la protección de contenido peligroso. ¿Estás seguro?"
    }

    companion object {
        fun isAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = ComponentName(context, MyDeviceAdminReceiver::class.java)
            return dpm.isAdminActive(component)
        }

        fun requestAdminActivation(context: Context) {
            if (isAdminActive(context)) {
                return
            }

            val component = ComponentName(context, MyDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "KeepSafe necesita permisos de administrador para:\n\n" +
                            "• Prevenir desinstalación no autorizada\n" +
                            "• Mantener protección continua contra contenido peligroso\n" +
                            "• Proteger a menores de sitios inapropiados\n\n" +
                            "La aplicación NO podrá:\n" +
                            "• Acceder a tus datos personales\n" +
                            "• Borrar información del dispositivo\n" +
                            "• Realizar cambios sin tu consentimiento"
                )
            }
            context.startActivity(intent)
        }

        fun deactivateAdmin(context: Context) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (dpm.isAdminActive(component)) {
                dpm.removeActiveAdmin(component)
                Log.d("DeviceAdminHelper", "Administrador desactivado exitosamente")
                Toast.makeText(
                    context,
                    "KeepSafe desactivado. Ahora puedes desinstalar la aplicación.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        fun applyMaximumRestrictions(context: Context) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = ComponentName(context, MyDeviceAdminReceiver::class.java)

            // Verificar si es Device Owner
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                Log.w("DeviceAdmin", "No es Device Owner, no se pueden aplicar restricciones")
                return
            }

            try {
                // Bugfix de eliminar aplicaciones
                dpm.clearUserRestriction(component, UserManager.DISALLOW_UNINSTALL_APPS)

                // Bloquear configuración VPN (Android 7.0+)
                dpm.addUserRestriction(component, UserManager.DISALLOW_CONFIG_VPN)

                // Bloquear modo seguro
                dpm.addUserRestriction(component, UserManager.DISALLOW_SAFE_BOOT)

                // Bloquear factory reset desde configuración
                dpm.addUserRestriction(component, UserManager.DISALLOW_FACTORY_RESET)

                // Bloquear agregar usuarios
                dpm.addUserRestriction(component, UserManager.DISALLOW_ADD_USER)

                // NUEVO: Bloquear instalación de apps desconocidas
                dpm.addUserRestriction(component, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)

                // Bloquear desinstalación de KeepSafe
                dpm.setUninstallBlocked(component, context.packageName, true)
            } catch (e: Exception) {
                Log.e("DeviceAdmin", "Error aplicando restricciones: ${e.message}")
            }
        }
    }
}