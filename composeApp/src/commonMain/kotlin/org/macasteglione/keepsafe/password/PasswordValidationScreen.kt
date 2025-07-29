package org.macasteglione.keepsafe.password

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
import androidx.compose.ui.unit.dp
import org.macasteglione.keepsafe.deviceAdmin.DeviceAdminHandler

@Composable
fun PasswordValidationScreen(
    passwordManager: PasswordManager,
    deviceAdminHandler: DeviceAdminHandler,
    onPasswordCorrect: () -> Unit
) {
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
        Text("Enter password to uninstall", color = Color.White)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = entered,
            onValueChange = {
                entered = it
                wrongPassword = false
            },
            label = { Text("Password") }
        )

        if (wrongPassword) {
            Spacer(Modifier.height(8.dp))
            Text("Incorrect password", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            val isValid = passwordManager.validate(entered)
            if (isValid) {
                onPasswordCorrect()
            } else {
                wrongPassword = true
                deviceAdminHandler.onPasswordFailed()
            }
        }) {
            Text("Confirm")
        }
    }
}