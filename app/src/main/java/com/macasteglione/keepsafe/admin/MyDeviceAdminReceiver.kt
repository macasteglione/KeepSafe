package com.macasteglione.keepsafe.admin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.macasteglione.keepsafe.ui.PasswordValidationActivity

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "KeepSafe activado como administrador del dispositivo",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)

        // Lanzar validación de contraseña
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
    }
}