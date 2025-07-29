package org.macasteglione.keepsafe.vpn

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import org.macasteglione.keepsafe.R
import org.macasteglione.keepsafe.deviceAdmin.MyDeviceAdminReceiver

class AndroidVpnController(private val context: Context) : VpnController {
    override fun startVpn() {
        val intent = VpnService.prepare(context)
        if (intent != null && context is Activity) {
            context.startActivityForResult(intent, 100)
        } else {
            context.startService(Intent(context, AndroidDnsVpnService::class.java))
            AndroidVpnPreferences(context).setVpnActive(true)
        }
    }

    override fun stopVpn() {
        val stopIntent = Intent(context, AndroidDnsVpnService::class.java).apply {
            action = "STOP_VPN"
        }
        context.startService(stopIntent)
    }

    override fun isVpnRunning(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = am.getRunningServices(Integer.MAX_VALUE)
        return services.any { it.service.className == AndroidDnsVpnService::class.java.name }
    }

    override fun requestDeviceAdmin() {
        val component = ComponentName(context, MyDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.app_admin_privileges)
            )
        }
        context.startActivity(intent)
    }
}