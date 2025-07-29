package org.macasteglione.keepsafe.deviceAdmin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.macasteglione.keepsafe.deviceAdmin.MyDeviceAdminReceiver
import org.macasteglione.keepsafe.R

class AndroidDeviceAdminHandler(private val context: Context) : DeviceAdminHandler {
    override fun onPasswordFailed() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val comp = ComponentName(context, MyDeviceAdminReceiver::class.java)

        if (!dpm.isAdminActive(comp)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.getString(R.string.app_admin_privileges)
                )
            }
            context.startActivity(intent)
        }
    }
}