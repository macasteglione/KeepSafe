package com.macasteglione.keepsafe

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.macasteglione.keepsafe.ui.theme.KeepSafeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val vpnRunningState = mutableStateOf(false)

    @SuppressLint("AutoboxingStateValueProperty")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        setTheme(R.style.Theme_KeepSafe)

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

                val dnsAddress = remember { mutableStateOf("No disponible") }

                LaunchedEffect(vpnRunningState.value) {
                    if (vpnRunningState.value) {
                        kotlinx.coroutines.delay(500)
                        val prefs = context.getSharedPreferences("vpn_prefs", Context.MODE_PRIVATE)
                        dnsAddress.value =
                            prefs.getString("vpn_address", "No disponible") ?: "No disponible"
                    } else {
                        dnsAddress.value = "No disponible"
                    }
                }

                val dnsPingMs = remember { mutableIntStateOf(0) }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val ms = getPingTime("1.1.1.1")
                        withContext(Dispatchers.Main) {
                            dnsPingMs.value = ms
                        }
                    }
                }

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
                    },
                    dnsName = "OpenDNS",
                    dnsAddress = dnsAddress.value,
                    dnsPingMs = dnsPingMs.intValue
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

fun getPingTime(host: String): Int {
    val start = System.currentTimeMillis()
    val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 $host")
    val result = process.waitFor()
    val end = System.currentTimeMillis()
    return if (result == 0) (end - start).toInt() else -1
}

@Composable
fun DNSChangerScreen(
    isVpnActive: Boolean,
    dnsName: String,
    dnsAddress: String?,
    dnsPingMs: Int,
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

    val statusText = if (isVpnActive) "Connected" else "Disconnected"
    val accentGreen = Color(0xFF4CAF50)
    val accentRed = Color(0xFFE57373)
    val cardColor = Color(0xFF2A2A2A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2F))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Botón circular grande para conectar/desconectar
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(if (isVpnActive) accentGreen else Color.DarkGray)
                .clickable { onToggleVpn() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVpnActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = statusText,
            color = if (isVpnActive) accentGreen else accentRed,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        dnsName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("$dnsPingMs ms", color = Color.White)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Address:", color = Color.Gray)
                    Text(dnsAddress ?: "No disponible", color = Color.Gray)
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status:", color = Color.Gray)
                    Text(
                        text = if (isVpnActive) "CONNECTED" else "DISCONNECTED",
                        color = if (isVpnActive) accentGreen else accentRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Diálogo para establecer contraseña
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

    // Diálogo para ingresar contraseña al desconectar
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
