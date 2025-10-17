package com.macasteglione.keepsafe.ui

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
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.macasteglione.keepsafe.data.PasswordManager
import com.macasteglione.keepsafe.admin.MyDeviceAdminReceiver
import com.macasteglione.keepsafe.data.VpnStateManager
import com.macasteglione.keepsafe.service.DnsVpnService
import com.macasteglione.keepsafe.ui.theme.KeepSafeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val vpnRunningState = mutableStateOf(false)
    private val REQUEST_VPN_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        setTheme(com.macasteglione.keepsafe.R.style.Theme_KeepSafe)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        vpnRunningState.value = VpnStateManager.isVpnReallyActive(this)

        setContent {
            KeepSafeTheme {
                MainScreen(
                    vpnRunningState = vpnRunningState.value,
                    onToggleVpn = { handleVpnToggle() },
                    onVpnStateChanged = { vpnRunningState.value = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar estado del VPN al volver a la app
        vpnRunningState.value = VpnStateManager.isVpnReallyActive(this)
    }

    private fun handleVpnToggle() {
        if (!vpnRunningState.value) {
            startVpn()
        }
        // Para detener el VPN, el usuario debe ingresar la contraseña en el diálogo
    }

    private fun startVpn() {
        if (!PasswordManager.isPasswordSet(this)) {
            // No debería llegar aquí, pero por seguridad
            return
        }

        if (!MyDeviceAdminReceiver.isAdminActive(this)) {
            MyDeviceAdminReceiver.requestAdminActivation(this)
            return
        }

        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN_PERMISSION)
        } else {
            startVpnService()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN_PERMISSION && resultCode == RESULT_OK) {
            startVpnService()
        }
    }

    private fun startVpnService() {
        startService(Intent(this, DnsVpnService::class.java))
        vpnRunningState.value = true
    }
}

@Composable
fun MainScreen(
    vpnRunningState: Boolean,
    onToggleVpn: () -> Unit,
    onVpnStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current

    var showSetPassword by remember { mutableStateOf(!PasswordManager.isPasswordSet(context)) }
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var enteredPassword by remember { mutableStateOf("") }

    var dnsAddress by remember { mutableStateOf("No disponible") }
    var dnsPingMs by remember { mutableIntStateOf(0) }
    var isCheckingPing by remember { mutableStateOf(false) }

    // Actualizar dirección DNS cuando cambia el estado
    LaunchedEffect(vpnRunningState) {
        if (vpnRunningState) {
            delay(500)
            dnsAddress = VpnStateManager.getSavedVpnAddress(context)
        } else {
            dnsAddress = "No disponible"
        }
    }

    // Verificar ping periódicamente
    LaunchedEffect(vpnRunningState) {
        while (vpnRunningState) {
            isCheckingPing = true
            dnsPingMs = withContext(Dispatchers.IO) {
                getPingTime("208.67.222.123")
            }
            isCheckingPing = false
            delay(5000) // Actualizar cada 5 segundos
        }
    }

    DNSChangerScreen(
        isVpnActive = vpnRunningState,
        onToggleVpn = {
            if (!vpnRunningState) {
                onToggleVpn()
            } else {
                enteredPassword = ""
                showPasswordPrompt = true
            }
        },
        showSetPasswordDialog = showSetPassword,
        onSetPassword = { password ->
            PasswordManager.savePassword(context, password)
            showSetPassword = false
            MyDeviceAdminReceiver.requestAdminActivation(context)
        },
        showPasswordPrompt = showPasswordPrompt,
        onValidatePassword = { password ->
            if (PasswordManager.validatePassword(context, password)) {
                val stopIntent = Intent(context, DnsVpnService::class.java).apply {
                    action = DnsVpnService.ACTION_STOP_VPN
                }
                context.startService(stopIntent)
                showPasswordPrompt = false
                onVpnStateChanged(false)
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
        dnsName = "OpenDNS Family Shield",
        dnsAddress = dnsAddress,
        dnsPingMs = dnsPingMs,
        isCheckingPing = isCheckingPing
    )
}

fun getPingTime(host: String): Int {
    return try {
        val start = System.currentTimeMillis()
        val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 2 $host")
        val result = process.waitFor()
        val end = System.currentTimeMillis()
        if (result == 0) (end - start).toInt() else -1
    } catch (_: Exception) {
        -1
    }
}

@Composable
fun DNSChangerScreen(
    isVpnActive: Boolean,
    dnsName: String,
    dnsAddress: String,
    dnsPingMs: Int,
    isCheckingPing: Boolean,
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
    val backgroundColor = Color(0xFF1E1E2F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Botón circular grande
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
                contentDescription = if (isVpnActive) "Desconectar" else "Conectar",
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

        // Tarjeta de información
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Título y ping
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
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
                            tint = if (isVpnActive && dnsPingMs > 0) accentGreen else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isVpnActive && dnsPingMs > 0) "$dnsPingMs ms" else "-- ms",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))

                // Dirección
                InfoRow(
                    label = "DNS Address:",
                    value = dnsAddress,
                    icon = Icons.Default.NetworkCheck
                )

                Spacer(Modifier.height(12.dp))

                // Estado
                InfoRow(
                    label = "Status:",
                    value = if (isVpnActive) "CONNECTED" else "DISCONNECTED",
                    valueColor = if (isVpnActive) accentGreen else accentRed,
                    isBold = true
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Diálogo para establecer contraseña inicial
    if (showSetPasswordDialog) {
        SetPasswordDialog(
            tempPassword = tempPassword,
            confirmPassword = confirmPassword,
            errorMessage = errorMessage,
            onTempPasswordChange = { tempPassword = it },
            onConfirmPasswordChange = { confirmPassword = it },
            onConfirm = {
                when {
                    tempPassword.isBlank() -> {
                        errorMessage = "La contraseña no puede estar vacía"
                    }

                    tempPassword.length < 4 -> {
                        errorMessage = "La contraseña debe tener al menos 4 caracteres"
                    }

                    tempPassword != confirmPassword -> {
                        errorMessage = "Las contraseñas no coinciden"
                    }

                    else -> {
                        onSetPassword(tempPassword)
                        errorMessage = null
                    }
                }
            }
        )
    }

    // Diálogo para validar contraseña al desconectar
    if (showPasswordPrompt) {
        ValidatePasswordDialog(
            enteredPassword = enteredPassword,
            wrongPassword = wrongPassword,
            onPasswordChange = {
                onEnteredPasswordChange(it)
                wrongPassword = false
            },
            onConfirm = {
                val isValid = onValidatePassword(enteredPassword)
                wrongPassword = !isValid
            },
            onDismiss = {
                onDismissPasswordPrompt()
                wrongPassword = false
            }
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    valueColor: Color = Color.Gray,
    isBold: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(label, color = Color.Gray, fontSize = 14.sp)
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SetPasswordDialog(
    tempPassword: String,
    confirmPassword: String,
    errorMessage: String?,
    onTempPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Configura una contraseña de seguridad") },
        text = {
            Column {
                Text(
                    "Esta contraseña será necesaria para desactivar la protección.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = tempPassword,
                    onValueChange = onTempPasswordChange,
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirmar contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Guardar")
            }
        }
    )
}

@Composable
fun ValidatePasswordDialog(
    enteredPassword: String,
    wrongPassword: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Introduce la contraseña") },
        text = {
            Column {
                Text(
                    "Para desconectar la protección necesitas ingresar tu contraseña.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = enteredPassword,
                    onValueChange = onPasswordChange,
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = wrongPassword,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (wrongPassword) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Contraseña incorrecta",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}