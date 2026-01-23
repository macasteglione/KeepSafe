package com.macasteglione.keepsafe.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macasteglione.keepsafe.admin.MyDeviceAdminReceiver
import com.macasteglione.keepsafe.data.PasswordManager
import com.macasteglione.keepsafe.ui.theme.KeepSafeTheme
import com.macasteglione.keepsafe.ui.UiConstants

class PasswordValidationActivity : ComponentActivity() {

    companion object {
        // Constants are now centralized in UiConstants
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KeepSafeTheme {
                PasswordValidationScreen(
                    onPasswordCorrect = {
                        MyDeviceAdminReceiver.deactivateAdmin(this)
                        Toast.makeText(
                            this,
                            "Protección desactivada. Ahora puedes desinstalar KeepSafe.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    },
                    onMaxAttemptsReached = {
                        Toast.makeText(
                            this,
                            "Demasiados intentos fallidos. Protección mantenida.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Prevenir que se cierre con el botón atrás
        Toast.makeText(
            this,
            "Debes ingresar la contraseña correcta para continuar",
            Toast.LENGTH_SHORT
        ).show()
        // NO llamar a super.onBackPressed() para bloquear el botón atrás
    }
}

@Composable
fun PasswordValidationScreen(
    onPasswordCorrect: () -> Unit,
    onMaxAttemptsReached: () -> Unit,
    maxAttempts: Int = UiConstants.MAX_PASSWORD_ATTEMPTS
) {
    val context = LocalContext.current

    // Estados del Composable - SE MANTIENEN DURANTE RECOMPOSICIONES
    var enteredPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) }

    val attemptsRemaining = maxAttempts - attemptCount

    // Prevenir que se salga de la pantalla
    BackHandler {
        Toast.makeText(
            context,
            "Debes ingresar la contraseña correcta",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Función para validar contraseña
    val validatePassword: () -> Unit = {
        if (enteredPassword.isNotBlank() && !isLoading) {
            isLoading = true
            val isValid = PasswordManager.validatePassword(context, enteredPassword)
            isLoading = false

            if (isValid) {
                onPasswordCorrect()
            } else {
                isError = true
                attemptCount++  // Incrementar contador
                enteredPassword = ""

                if (attemptCount >= maxAttempts) {
                    // Máximo de intentos alcanzado
                    onMaxAttemptsReached()
                } else {
                    // Mostrar intentos restantes
                    Toast.makeText(
                        context,
                        "Contraseña incorrecta. ${maxAttempts - attemptCount} intentos restantes.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UiConstants.BACKGROUND_DARK)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono de bloqueo
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = UiConstants.ACCENT_RED,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Protección Activa",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Introduce la contraseña para desactivar KeepSafe",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Campo de contraseña
        OutlinedTextField(
            value = enteredPassword,
            onValueChange = {
                enteredPassword = it
                isError = false
            },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = isError,
            visualTransformation = if (passwordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { validatePassword() }
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible)
                            "Ocultar contraseña"
                        else
                            "Mostrar contraseña"
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = UiConstants.ACCENT_GREEN,
                unfocusedBorderColor = Color.Gray,
                errorBorderColor = UiConstants.ACCENT_RED,
                focusedLabelColor = UiConstants.ACCENT_GREEN,
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (isError) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Contraseña incorrecta",
                color = UiConstants.ACCENT_RED,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        // Contador de intentos con color dinámico
        Text(
            text = "Intentos restantes: $attemptsRemaining",
            color = when {
                attemptsRemaining <= 1 -> UiConstants.ACCENT_RED       // Rojo para último intento
                attemptsRemaining == 2 -> UiConstants.ACCENT_ORANGE    // Naranja para penúltimo
                else -> Color.Gray                          // Gris para intentos normales
            },
            fontSize = 14.sp,
            fontWeight = if (attemptsRemaining <= 1) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(Modifier.height(24.dp))

        // Botón de confirmación
        Button(
            onClick = validatePassword,
            enabled = enteredPassword.isNotBlank() && !isLoading && attemptCount < maxAttempts,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = UiConstants.ACCENT_GREEN,
                disabledContainerColor = Color.Gray
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Confirmar", fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        // Card de advertencia
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = UiConstants.CARD_BACKGROUND
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Advertencia",
                    color = UiConstants.ACCENT_ORANGE,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Al desactivar KeepSafe, se eliminará toda la protección contra contenido peligroso e inapropiado.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}