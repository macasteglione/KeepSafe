package org.macasteglione.keepsafe.ui.theme

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.macasteglione.keepsafe.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DNSChangerScreen(
    isVpnActive: Boolean,
    dnsAddress: String?,
    dnsPingMs: Int,
    onToggleVpn: () -> Unit,
    showSetPasswordDialog: Boolean,
    onSetPassword: (String) -> Unit,
    showPasswordPrompt: Boolean,
    onValidatePassword: () -> Boolean,
    enteredPassword: String,
    onEnteredPasswordChange: (String) -> Unit,
    onDismissPasswordPrompt: () -> Unit
) {
    var tempPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var wrongPassword by remember { mutableStateOf(false) }

    val accentGreen = Color(0xFF4CAF50)
    val accentRed = Color(0xFFE57373)
    val cardColor = Color(0xFF2A2A2A)
    val backgroundColor = Color(0xFF1E1E2F)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), color = Color.White) },
                modifier = Modifier.background(backgroundColor)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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

                Spacer(Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(
                            label = stringResource(R.string.vpn_ping),
                            value = "$dnsPingMs ms",
                            icon = Icons.Default.Speed
                        )
                        InfoRow(
                            label = stringResource(R.string.vpn_address),
                            value = dnsAddress ?: stringResource(R.string.vpn_address_not_available)
                        )
                        InfoRow(
                            label = stringResource(R.string.vpn_status),
                            value = if (isVpnActive) stringResource(R.string.vpn_status_connected)
                            else stringResource(R.string.vpn_status_disconnected),
                            valueColor = if (isVpnActive) accentGreen else accentRed,
                            bold = true
                        )
                    }
                }
            }

            // Diálogo para establecer contraseña
            if (showSetPasswordDialog) {
                val passwordMismatchText = stringResource(R.string.password_not_matching)

                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.set_password)) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = tempPassword,
                                onValueChange = { tempPassword = it },
                                label = { Text(stringResource(R.string.password)) }
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text(stringResource(R.string.confirm)) }
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
                                errorMessage = passwordMismatchText
                            }
                        }) {
                            Text(stringResource(R.string.save))
                        }
                    }
                )
            }

            // Diálogo para ingresar contraseña
            if (showPasswordPrompt) {
                AlertDialog(
                    onDismissRequest = {
                        onDismissPasswordPrompt()
                        wrongPassword = false
                    },
                    title = { Text(stringResource(R.string.enter_disconnect_password)) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = enteredPassword,
                                onValueChange = {
                                    onEnteredPasswordChange(it)
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
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val ok = onValidatePassword()
                            wrongPassword = !ok
                        }) {
                            Text(stringResource(R.string.accept))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            onDismissPasswordPrompt()
                            wrongPassword = false
                        }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: Color = Color.Gray,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(it, contentDescription = null, tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                value,
                color = valueColor,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
