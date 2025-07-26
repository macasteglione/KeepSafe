package com.macasteglione.keepsafe

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macasteglione.keepsafe.ui.theme.KeepSafeTheme

class PasswordValidationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KeepSafeTheme {
                val context = LocalContext.current

                PasswordValidationScreen(
                    onPasswordCorrect = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.keepsafe_disabled),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    },
                    onPasswordWrong = {
                        Toast.makeText(
                            context,
                            context.getString(R.string.incorrect_password),
                            Toast.LENGTH_LONG
                        ).show()
                        reactivateDeviceAdmin()
                    }
                )
            }
        }
    }

    private fun reactivateDeviceAdmin() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val comp = ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (!dpm.isAdminActive(comp)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.app_admin_privileges)
                )
            }
            startActivity(intent)
        }
    }
}

@Composable
fun PasswordValidationScreen(
    onPasswordCorrect: () -> Unit,
    onPasswordWrong: () -> Unit
) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf("") }
    var wrongPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2F))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.enter_password_for_uninstall), color = Color.White)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = entered,
            onValueChange = {
                entered = it
                wrongPassword = false
            },
            label = { Text(stringResource(R.string.password)) }
        )

        if (wrongPassword) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.incorrect_password),
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            val isValid = PasswordManager.validatePassword(context, entered)
            if (isValid) {
                onPasswordCorrect()
            } else {
                wrongPassword = true
                onPasswordWrong()
            }
        }) {
            Text(stringResource(R.string.confirm))
        }
    }
}
