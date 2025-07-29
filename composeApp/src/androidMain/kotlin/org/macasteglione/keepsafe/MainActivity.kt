package org.macasteglione.keepsafe

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.macasteglione.keepsafe.password.AndroidSecurePreferences
import org.macasteglione.keepsafe.password.PasswordManager
import org.macasteglione.keepsafe.ui.theme.DNSChangerScreen
import org.macasteglione.keepsafe.ui.theme.KeepSafeTheme
import org.macasteglione.keepsafe.vpn.AndroidVpnController
import org.macasteglione.keepsafe.vpn.VpnUtils

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainScreenViewModel

    @SuppressLint("AutoboxingStateValueProperty")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        setTheme(R.style.Theme_KeepSafe)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeepSafeTheme {
                val context = LocalContext.current

                viewModel = remember {
                    MainScreenViewModel(
                        passwordManager = PasswordManager(AndroidSecurePreferences(context)),
                        vpnController = AndroidVpnController(context as Activity),
                        vpnUtils = VpnUtils
                    )
                }

                LaunchedEffect(Unit) {
                    viewModel.init()
                }

                DNSChangerScreen(
                    isVpnActive = viewModel.isVpnActive.value,
                    dnsAddress = viewModel.dnsAddress.value,
                    dnsPingMs = viewModel.dnsPingMs.value,
                    onToggleVpn = {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            startActivityForResult(intent, 100)
                        } else {
                            viewModel.onToggleVpn()
                        }
                    },
                    showSetPasswordDialog = viewModel.showSetPassword.value,
                    onSetPassword = {
                        viewModel.onSetPassword(it)
                        AndroidVpnController(context).requestDeviceAdmin()
                    },
                    showPasswordPrompt = viewModel.showPasswordPrompt.value,
                    onValidatePassword = {
                        viewModel.onValidatePassword()
                    },
                    enteredPassword = viewModel.enteredPassword.value,
                    onEnteredPasswordChange = {
                        viewModel.onEnteredPasswordChange(it)
                    },
                    onDismissPasswordPrompt = {
                        viewModel.onEnteredPasswordChange("")
                        viewModel.showPasswordPrompt.value = false
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            viewModel.onToggleVpn()
        }
    }
}
