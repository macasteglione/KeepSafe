package org.macasteglione.keepsafe

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import org.macasteglione.keepsafe.deviceAdmin.AndroidDeviceAdminHandler
import org.macasteglione.keepsafe.password.AndroidSecurePreferences
import org.macasteglione.keepsafe.password.PasswordManager
import org.macasteglione.keepsafe.password.PasswordValidationScreen
import org.macasteglione.keepsafe.ui.theme.KeepSafeTheme

class PasswordValidationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KeepSafeTheme {
                val context = LocalContext.current
                val passwordManager = PasswordManager(AndroidSecurePreferences(context))
                val adminHandler = AndroidDeviceAdminHandler(context)

                PasswordValidationScreen(
                    passwordManager = passwordManager,
                    deviceAdminHandler = adminHandler,
                    onPasswordCorrect = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.keepsafe_disabled),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                )
            }
        }
    }
}