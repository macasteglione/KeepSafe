package com.macasteglione.keepsafe

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.macasteglione.keepsafe.ui.theme.KeepSafeTheme

class MainActivity : ComponentActivity() {
    private val vpnRunningState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        vpnRunningState.value = VpnStateManager.isVpnReallyActive(this)

        setContent {
            KeepSafeTheme {
                val context = LocalContext.current
                var showSetPassword by remember {
                    mutableStateOf(!PasswordManager.isPasswordSet(context))
                }
                var enteredPassword by remember { mutableStateOf("") }
                var showPasswordPrompt by remember { mutableStateOf(false) }

                DNSChangerScreen(
                    isVpnActive = vpnRunningState.value,
                    onToggleVpn = {
                        if (!vpnRunningState.value) {
                            toggleVpn()
                        } else {
                            enteredPassword = ""
                            showPasswordPrompt = true
                        }
                    },
                    showSetPasswordDialog = showSetPassword,
                    onSetPassword = { pass ->
                        PasswordManager.savePassword(context, pass)
                        showSetPassword = false
                        enableDeviceAdmin()
                    },
                    showPasswordPrompt = showPasswordPrompt,
                    onValidatePassword = { pass ->
                        if (PasswordManager.validatePassword(context, pass)) {
                            val stopIntent = Intent(this, DnsVpnService::class.java).apply {
                                action = "STOP_VPN"
                            }
                            startService(stopIntent)
                            showPasswordPrompt = false
                            vpnRunningState.value = false
                            true
                        } else {
                            false
                        }
                    },
                    enteredPassword = enteredPassword,
                    onEnteredPasswordChange = { enteredPassword = it },
                    onDismissPasswordPrompt = {
                        enteredPassword = ""
                        showPasswordPrompt = false
                    }
                )
            }
        }
    }

    private fun enableDeviceAdmin() {
        val component = ComponentName(this, MyDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "KeepSafe necesita permisos para evitar desinstalación no autorizada."
            )
        }
        startActivity(intent)
    }

    private fun toggleVpn() {
        if (!vpnRunningState.value) {
            val intent = VpnService.prepare(this)
            if (intent != null)
                startActivityForResult(intent, 100)
            else
                onActivityResult(100, RESULT_OK, null)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startService(Intent(this, DnsVpnService::class.java))
            vpnRunningState.value = true
        }
    }
}

@Composable
fun DNSChangerScreen(
    isVpnActive: Boolean,
    onToggleVpn: () -> Unit,
    showSetPasswordDialog: Boolean,
    onSetPassword: (String) -> Unit,
    showPasswordPrompt: Boolean,
    onValidatePassword: (String) -> Boolean,
    enteredPassword: String,
    onEnteredPasswordChange: (String) -> Unit,
    onDismissPasswordPrompt: () -> Unit
) {
    var tempPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var wrongPassword by remember { mutableStateOf(false) }

    val statusText = if (isVpnActive) "VPN Activo (OpenDNS)" else "VPN Inactivo"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Estado del DNS Changer:",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isVpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onToggleVpn) {
            Text(if (isVpnActive) "Desactivar" else "Activar OpenDNS")
        }
    }

    if (showSetPasswordDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Configura una contraseña") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text("Contraseña") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar") }
                    )
                    errorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempPassword.isNotBlank() && tempPassword == confirmPassword) {
                        onSetPassword(tempPassword)
                    } else {
                        errorMessage = "Las contraseñas no coinciden"
                    }
                }) {
                    Text("Guardar")
                }
            }
        )
    }

    if (showPasswordPrompt) {
        AlertDialog(
            onDismissRequest = {
                onDismissPasswordPrompt()
                wrongPassword = false
            },
            title = { Text("Introduce la contraseña") },
            text = {
                Column {
                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = {
                            onEnteredPasswordChange(it)
                            wrongPassword = false
                        },
                        label = { Text("Contraseña") }
                    )
                    if (wrongPassword) {
                        Spacer(Modifier.height(8.dp))
                        Text("Contraseña incorrecta", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ok = onValidatePassword(enteredPassword)
                    wrongPassword = !ok
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismissPasswordPrompt()
                    wrongPassword = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}