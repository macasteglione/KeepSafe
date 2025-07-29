package org.macasteglione.keepsafe

import androidx.compose.runtime.mutableStateOf
import org.macasteglione.keepsafe.password.PasswordManager
import org.macasteglione.keepsafe.vpn.VpnController
import org.macasteglione.keepsafe.vpn.VpnUtils

class MainScreenViewModel(
    private val passwordManager: PasswordManager,
    private val vpnController: VpnController,
    private val vpnUtils: VpnUtils
) {
    val isVpnActive = mutableStateOf(false)
    val dnsAddress = mutableStateOf<String?>(null)
    val dnsPingMs = mutableStateOf(0)

    val showSetPassword = mutableStateOf(false)
    val showPasswordPrompt = mutableStateOf(false)
    val enteredPassword = mutableStateOf("")

    fun init() {
        isVpnActive.value = vpnController.isVpnRunning()
        showSetPassword.value = !passwordManager.isSet()
        updateDnsAddress()
        updatePing()
    }

    fun updateDnsAddress() {
        dnsAddress.value = vpnUtils.getVpnInterfaceAddress() ?: "N/A"
    }

    fun updatePing() {
        dnsPingMs.value = vpnUtils.ping("1.1.1.1")
    }

    fun onToggleVpn() {
        if (isVpnActive.value) {
            showPasswordPrompt.value = true
        } else {
            vpnController.startVpn()
            isVpnActive.value = true
            updateDnsAddress()
        }
    }

    fun onEnteredPasswordChange(value: String) {
        enteredPassword.value = value
    }

    fun onValidatePassword(): Boolean {
        val valid = passwordManager.validate(enteredPassword.value)
        if (valid) {
            vpnController.stopVpn()
            isVpnActive.value = false
            showPasswordPrompt.value = false
        }
        return valid
    }

    fun onSetPassword(password: String) {
        passwordManager.save(password)
        showSetPassword.value = false
        vpnController.requestDeviceAdmin()
    }
}