package com.macasteglione.keepsafe.ui

import androidx.compose.ui.graphics.Color

/**
 * UI and Application Constants for the KeepSafe application.
 * These constants are used across different components for consistent theming and behavior.
 */
object UiConstants {
    // Colors for UI theming
    val ACCENT_GREEN = Color(0xFF4CAF50)
    val ACCENT_RED = Color(0xFFE57373)
    val ACCENT_ORANGE = Color(0xFFFFB74D)
    val CARD_BACKGROUND = Color(0xFF2A2A2A)
    val BACKGROUND_DARK = Color(0xFF1E1E2F)

    // Request codes for system interactions
    const val REQUEST_VPN_PERMISSION = 100

    // UI timing constants
    const val VPN_UPDATE_DELAY_MS = 500L
    const val PING_UPDATE_INTERVAL_MS = 5000L
    const val VPN_START_DELAY_MS = 5000L
    const val VPN_RESTART_DELAY_MS = 3000L

    // Notification constants
    const val NOTIFICATION_ID = 1
    const val CHANNEL_ID = "vpn_channel"
    const val REACTIVATION_NOTIFICATION_ID = 9999

    // Validation constants
    const val MIN_PASSWORD_LENGTH = 4
    const val MAX_PASSWORD_ATTEMPTS = 3

    // VPN Service Actions
    const val ACTION_STOP_VPN = "STOP_VPN"
    const val ACTION_RECONNECT = "RECONNECT_VPN"
}