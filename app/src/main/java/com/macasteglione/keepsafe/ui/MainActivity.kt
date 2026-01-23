package com.macasteglione.keepsafe.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import com.macasteglione.keepsafe.admin.MyDeviceAdminReceiver
import com.macasteglione.keepsafe.data.PasswordManager
import com.macasteglione.keepsafe.data.VpnStateManager
import com.macasteglione.keepsafe.service.DnsVpnService
import com.macasteglione.keepsafe.ui.theme.KeepSafeTheme
import com.macasteglione.keepsafe.ui.UiConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Main activity for the KeepSafe application.
 *
 * This activity serves as the primary user interface for managing the DNS-based VPN protection.
 * It handles VPN state management, password validation for disabling protection, and displays
 * real-time connection information including DNS servers and ping times.
 *
 * Key features:
 * - VPN toggle with password protection for disabling
 * - Real-time DNS server information display
 * - Ping monitoring for connection quality
 * - Device admin integration for enhanced security
 * - Automatic reactivation prompts after app updates
 */
class MainActivity : ComponentActivity() {

    // VPN state management - tracks current VPN connection status
    private val vpnRunningState = mutableStateOf(false)

    /**
     * Initializes the main activity and sets up the VPN protection interface.
     *
     * This method performs the following initialization steps:
     * 1. Installs splash screen for smooth app startup
     * 2. Sets the app theme
     * 3. Enables edge-to-edge display
     * 4. Checks current VPN status and updates UI state
     * 5. Applies device admin restrictions for security
     * 6. Checks if VPN needs reactivation after app updates
     * 7. Sets up the Compose UI with the main screen
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        setTheme(com.macasteglione.keepsafe.R.style.Theme_KeepSafe)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize VPN state by checking if service is actually running
        vpnRunningState.value = VpnStateManager.isVpnReallyActive(this)

        // Apply maximum device restrictions for parental control
        MyDeviceAdminReceiver.applyMaximumRestrictions(this)

        // Check if VPN needs to be reactivated after app update
        checkVpnStatusAfterUpdate()

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

    /**
     * Updates VPN state when the activity resumes.
     *
     * This ensures the UI accurately reflects the current VPN status
     * whenever the user returns to the app.
     */
    override fun onResume() {
        super.onResume()
        // Update VPN state when returning to the app
        vpnRunningState.value = VpnStateManager.isVpnReallyActive(this)
    }

    /**
     * Handles VPN toggle button presses.
     *
     * Starting VPN requires admin permissions and may need user permission.
     * Stopping VPN requires password authentication through a dialog.
     */
    private fun handleVpnToggle() {
        if (!vpnRunningState.value) {
            startVpn()
        }
        // To stop VPN, user must enter password in the dialog
    }

    /**
     * Initiates the VPN connection process with proper validation.
     *
     * Performs pre-flight checks before starting VPN:
     * 1. Ensures password is configured
     * 2. Ensures device admin is active
     * 3. Requests VPN permission if needed
     * 4. Starts the VPN service
     */
    private fun startVpn() {
        when {
            // Safety check: ensure password is configured
            !PasswordManager.isPasswordSet(this) -> {
                Log.w("MainActivity", "VPN start attempted without password set")
                return
            }

            // Ensure device admin privileges are active
            !MyDeviceAdminReceiver.isAdminActive(this) -> {
                MyDeviceAdminReceiver.requestAdminActivation(this)
                return
            }

            else -> {
                // Request VPN permission from system if needed
                val intent = VpnService.prepare(this)
                if (intent != null) {
                        startActivityForResult(intent, UiConstants.REQUEST_VPN_PERMISSION)
                } else {
                    startVpnService()
                }
            }
        }
    }

    /**
     * Handles the result of VPN permission request.
     *
     * @param requestCode The request code that was passed to startActivityForResult()
     * @param resultCode The result code returned by the child activity
     * @param data Additional data returned by the child activity
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UiConstants.REQUEST_VPN_PERMISSION && resultCode == RESULT_OK) {
            startVpnService()
        }
    }

    /**
     * Starts the DNS VPN service and updates UI state.
     *
     * This method initiates the background VPN service that handles DNS filtering.
     */
    private fun startVpnService() {
        startService(Intent(this, DnsVpnService::class.java))
        vpnRunningState.value = true
    }

    /**
     * Checks VPN status after app updates and prompts for reactivation if needed.
     *
     * When the app is updated, the VPN service may be stopped by the system.
     * This method detects this situation and prompts the user to reactivate protection.
     */
    private fun checkVpnStatusAfterUpdate() {
        val shouldBeActive = VpnStateManager.getVpnState(this)
        val isActuallyActive = VpnStateManager.isVpnReallyActive(this)

        if (shouldBeActive && !isActuallyActive) {
            // Show urgent reactivation dialog
            android.app.AlertDialog.Builder(this)
                .setTitle("Protección Desactivada")
                .setMessage(
                    "KeepSafe se detuvo durante la actualización.\n\n" +
                            "¿Deseas reactivar la protección ahora?"
                )
                .setCancelable(false)
                .setPositiveButton("Reactivar") { _, _ ->
                    // Start VPN after getting permission
                    val intent = VpnService.prepare(this)
                    if (intent != null) {
                    startActivityForResult(intent, UiConstants.REQUEST_VPN_PERMISSION)
                    } else {
                        startService(Intent(this, DnsVpnService::class.java))
                        vpnRunningState.value = true
                    }
                }
                .setNegativeButton("Ahora No") { _, _ ->
                    // User chooses not to reactivate
                    Toast.makeText(
                        this,
                        "Navegando sin protección",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .show()
        }
    }
}

/**
 * Main screen composable that displays the VPN control interface.
 *
 * This composable manages the primary UI state and orchestrates the display
 * of password setup dialogs, VPN toggle controls, and connection information.
 *
 * @param vpnRunningState Current VPN connection state
 * @param onToggleVpn Callback to handle VPN start/stop actions
 * @param onVpnStateChanged Callback when VPN state changes
 */
@Composable
fun MainScreen(
    vpnRunningState: Boolean,
    onToggleVpn: () -> Unit,
    onVpnStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // Dialog visibility states
    var showSetPassword by remember { mutableStateOf(!PasswordManager.isPasswordSet(context)) }
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var enteredPassword by remember { mutableStateOf("") }

    // DNS information display states
    var dnsAddress by remember { mutableStateOf("No disponible") }
    var dnsPingMs by remember { mutableIntStateOf(0) }
    var isCheckingPing by remember { mutableStateOf(false) }

    // Update DNS address when VPN state changes
    LaunchedEffect(vpnRunningState) {
        if (vpnRunningState) {
            delay(UiConstants.VPN_UPDATE_DELAY_MS) // Wait for VPN to establish
            dnsAddress = VpnStateManager.getSavedVpnAddress(context)
        } else {
            dnsAddress = "No disponible"
        }
    }

    // Monitor ping time periodically when VPN is active
    LaunchedEffect(vpnRunningState) {
        while (vpnRunningState) {
            isCheckingPing = true
            dnsPingMs = withContext(Dispatchers.IO) {
                getPingTime("208.67.222.123") // OpenDNS primary server
            }
            isCheckingPing = false
            delay(UiConstants.PING_UPDATE_INTERVAL_MS) // Update every 5 seconds
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
                    action = UiConstants.ACTION_STOP_VPN
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

/**
 * Measures ping time to a specified host using system ping command.
 *
 * @param host The hostname or IP address to ping
 * @return Ping time in milliseconds, or -1 if ping failed or timed out
 */
fun getPingTime(host: String): Int {
    return try {
        val start = System.currentTimeMillis()
        // Execute ping command with 1 packet and 2 second timeout
        val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 2 $host")
        val result = process.waitFor()
        val end = System.currentTimeMillis()
        // Return ping time if successful (exit code 0), otherwise -1
        if (result == 0) (end - start).toInt() else -1
    } catch (_: Exception) {
        -1 // Return -1 on any error
    }
}

/**
 * Main DNS changer screen composable displaying VPN status and controls.
 *
 * This composable renders the primary user interface with:
 * - Large circular VPN toggle button
 * - Connection status display
 * - DNS server information card
 * - Password setup and validation dialogs
 *
 * @param isVpnActive Whether VPN protection is currently active
 * @param dnsName Display name of the DNS service
 * @param dnsAddress Current DNS server address
 * @param dnsPingMs Current ping time to DNS server in milliseconds
 * @param isCheckingPing Whether ping measurement is in progress
 * @param onToggleVpn Callback for VPN toggle button
 * @param showSetPasswordDialog Whether to show initial password setup dialog
 * @param onSetPassword Callback when password is successfully set
 * @param showPasswordPrompt Whether to show password validation dialog
 * @param onValidatePassword Callback to validate entered password
 * @param enteredPassword Current password input text
 * @param onEnteredPasswordChange Callback when password input changes
 * @param onDismissPasswordPrompt Callback to dismiss password dialog
 */
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
    // Password setup dialog state
    var tempPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var wrongPassword by remember { mutableStateOf(false) }

    // UI color scheme based on VPN state
    val statusText = if (isVpnActive) "Connected" else "Disconnected"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UiConstants.BACKGROUND_DARK)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Botón circular grande
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(if (isVpnActive) UiConstants.ACCENT_GREEN else Color.DarkGray)
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
            color = if (isVpnActive) UiConstants.ACCENT_GREEN else UiConstants.ACCENT_RED,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(Modifier.height(32.dp))

        // Tarjeta de información
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = UiConstants.CARD_BACKGROUND),
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
                            tint = if (isVpnActive && dnsPingMs > 0) UiConstants.ACCENT_GREEN else Color.Gray,
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
                    valueColor = if (isVpnActive) UiConstants.ACCENT_GREEN else UiConstants.ACCENT_RED,
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

                    tempPassword.length < UiConstants.MIN_PASSWORD_LENGTH -> {
                        errorMessage = "La contraseña debe tener al menos ${UiConstants.MIN_PASSWORD_LENGTH} caracteres"
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

/**
 * Reusable composable for displaying information rows in the status card.
 *
 * Displays a label with optional icon on the left and a value on the right,
 * commonly used for DNS information display.
 *
 * @param label The label text to display
 * @param value The value text to display
 * @param icon Optional icon to display next to the label
 * @param valueColor Color for the value text (defaults to gray)
 * @param isBold Whether the value text should be bold
 */
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

/**
 * Dialog composable for initial password setup.
 *
 * This dialog is shown when the app is first launched and no password
 * has been configured yet. It requires the user to set a password
 * that will be needed to disable VPN protection later.
 *
 * @param tempPassword The temporary password input
 * @param confirmPassword The password confirmation input
 * @param errorMessage Error message to display if validation fails
 * @param onTempPasswordChange Callback when password input changes
 * @param onConfirmPasswordChange Callback when confirmation input changes
 * @param onConfirm Callback when user confirms password setup
 */
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
        onDismissRequest = {}, // Cannot be dismissed - password must be set
        title = { Text("Configura una contraseña de seguridad") },
        text = {
            Column {
                Text(
                    "Esta contraseña será necesaria para desactivar la protección.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))

                // Password input field
                OutlinedTextField(
                    value = tempPassword,
                    onValueChange = onTempPasswordChange,
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // Password confirmation field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirmar contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Display error message if validation failed
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

/**
 * Dialog composable for password validation when disabling VPN.
 *
 * This dialog requires the user to enter their previously configured password
 * before allowing them to disable VPN protection. Includes attempt limiting
 * and visual feedback for incorrect passwords.
 *
 * @param enteredPassword Current password input
 * @param wrongPassword Whether the last password attempt was incorrect
 * @param onPasswordChange Callback when password input changes
 * @param onConfirm Callback when user attempts to validate password
 * @param onDismiss Callback when user cancels password validation
 */
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

                // Password input field with error state
                OutlinedTextField(
                    value = enteredPassword,
                    onValueChange = onPasswordChange,
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = wrongPassword,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Show error message if password was wrong
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