package com.macasteglione.keepsafe.ui

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.macasteglione.keepsafe.admin.MyDeviceAdminReceiver
import com.macasteglione.keepsafe.data.PasswordManager
import com.macasteglione.keepsafe.ui.theme.KeepSafeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Activity de validación de contraseña REFORZADA
 * Previene bypass por cierre de app desde recientes
 */
class PasswordValidationActivity : ComponentActivity() {

    private var attemptCount = 0
    private val maxAttempts = 3
    private var isValidationComplete = false

    // Flag para evitar re-activación
    private var hasReactivated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
            KeepSafeTheme {
                LifecycleMonitor(
                    onPaused = {
                        if (!isValidationComplete) {
                            handleBypassAttempt()
                        }
                    }
                )

                PasswordValidationScreen(
                    attemptsRemaining = maxAttempts - attemptCount,
                    onPasswordCorrect = {
                        isValidationComplete = true
                        MyDeviceAdminReceiver.deactivateAdmin(this)
                        Toast.makeText(
                            this,
                            "Protección desactivada correctamente.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    },
                    onMaxAttemptsReached = {
                        isValidationComplete = true
                        reactivateDeviceAdmin()
                        Toast.makeText(
                            this,
                            "Demasiados intentos. Protección mantenida.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    },
                    onPasswordWrong = {
                        attemptCount++
                    },
                    onCancelled = {
                        isValidationComplete = true
                        reactivateDeviceAdmin()
                        Toast.makeText(
                            this,
                            "Protección mantenida activa",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (!isValidationComplete && !isFinishing) {
            handleBypassAttempt()
        }
    }

    override fun onStop() {
        super.onStop()

        if (!isValidationComplete && !isFinishing) {
            handleBypassAttempt()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isValidationComplete) {
            handleBypassAttempt()
        }
    }

    /**
     * Maneja intentos de bypass (cerrar app desde recientes)
     */
    private fun handleBypassAttempt() {
        if (hasReactivated) return
        hasReactivated = true

        Toast.makeText(
            this,
            "Intento de bypass detectado. Reactivando protección...",
            Toast.LENGTH_LONG
        ).show()

        // Re-activar Device Admin
        reactivateDeviceAdmin()

        // Relanzar esta activity para forzar validación
        val intent = Intent(this, PasswordValidationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }

    /**
     * Re-activa el Device Admin si fue desactivado
     */
    private fun reactivateDeviceAdmin() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (!dpm.isAdminActive(component)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "ATENCIÓN: Se detectó un intento de bypass. " +
                            "Debes ingresar la contraseña correcta para desactivar KeepSafe."
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // NO permitir salir con botón atrás
        Toast.makeText(
            this,
            "Debes ingresar la contraseña correcta para continuar",
            Toast.LENGTH_SHORT
        ).show()
        // NO llamar a super.onBackPressed()
    }

    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)
        // Si remueven la tarea, re-activar protección
        if (!isValidationComplete) {
            reactivateDeviceAdmin()
        }
    }
}

/**
 * Composable que monitorea el ciclo de vida de la Activity
 */
@Composable
fun LifecycleMonitor(
    onPaused: () -> Unit = {},
    onResumed: () -> Unit = {},
    onStopped: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> onPaused()
                Lifecycle.Event.ON_RESUME -> onResumed()
                Lifecycle.Event.ON_STOP -> onStopped()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun PasswordValidationScreen(
    attemptsRemaining: Int,
    onPasswordCorrect: () -> Unit,
    onMaxAttemptsReached: () -> Unit,
    onPasswordWrong: () -> Unit,
    onCancelled: () -> Unit = {},
    maxAttempts: Int = 3
) {
    val context = LocalContext.current

    var enteredPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showWarning by remember { mutableStateOf(false) }

    // Auto-detectar si el usuario intenta salir
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)

            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val component = ComponentName(context, MyDeviceAdminReceiver::class.java)

            if (!dpm.isAdminActive(component)) {
                showWarning = true
            }
        }
    }

    val validatePassword: () -> Unit = {
        if (enteredPassword.isNotBlank() && !isLoading) {
            isLoading = true
            val isValid = PasswordManager.validatePassword(context, enteredPassword)
            isLoading = false

            if (isValid) {
                onPasswordCorrect()
            } else {
                isError = true
                enteredPassword = ""
                onPasswordWrong()

                if (attemptsRemaining - 1 <= 0) {
                    onMaxAttemptsReached()
                } else {
                    Toast.makeText(
                        context,
                        "Contraseña incorrecta. ${attemptsRemaining - 1} intentos restantes.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Prevenir salida con botón atrás
    BackHandler {
        Toast.makeText(
            context,
            "Debes ingresar la contraseña correcta",
            Toast.LENGTH_SHORT
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2F))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Advertencia si detecta bypass
        if (showWarning) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE57373))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Intento de bypass detectado!\nSe ha reactivado la protección.",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color(0xFFE57373),
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

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Cerrar esta pantalla reactivará la protección",
            fontSize = 12.sp,
            color = Color(0xFFFFB74D),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(32.dp))

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
                        contentDescription = null
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color.Gray,
                errorBorderColor = Color(0xFFE57373),
                focusedLabelColor = Color(0xFF4CAF50),
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
                color = Color(0xFFE57373),
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Intentos restantes: $attemptsRemaining",
            color = when {
                attemptsRemaining <= 1 -> Color(0xFFE57373)
                attemptsRemaining == 2 -> Color(0xFFFFB74D)
                else -> Color.Gray
            },
            fontSize = 14.sp,
            fontWeight = if (attemptsRemaining <= 1) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(Modifier.height(24.dp))

        // Botón de confirmación
        Button(
            onClick = validatePassword,
            enabled = enteredPassword.isNotBlank() && !isLoading && attemptsRemaining > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
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

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                onCancelled()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Gray
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Cancelar", fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Texto explicativo del botón cancelar
        Text(
            text = "Cancelar mantendrá la protección activa",
            fontSize = 12.sp,
            color = Color(0xFF4CAF50),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A2A)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Advertencia",
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "• Al desactivar KeepSafe, se eliminará toda la protección.\n" +
                            "• Cerrar esta pantalla reactivará automáticamente la protección.\n" +
                            "• El Device Admin se activará nuevamente si intentas hacer bypass.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}