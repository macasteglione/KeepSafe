package org.macasteglione.keepsafe.deviceAdmin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.macasteglione.keepsafe.PasswordValidationActivity
import org.macasteglione.keepsafe.R

class MyDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, context.getString(R.string.active_as_admin), Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        val passwordIntent = Intent(context, PasswordValidationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(passwordIntent)
    }
}