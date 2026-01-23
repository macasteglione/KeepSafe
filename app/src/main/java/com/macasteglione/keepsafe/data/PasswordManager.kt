package com.macasteglione.keepsafe.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.subtle.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Extension property to access DataStore from Context.
 * Provides encrypted preferences storage for sensitive data.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore("secure_prefs")

/**
 * Password manager for secure password storage and validation.
 *
 * Uses Google Tink cryptographic library with Android Keystore for
 * hardware-backed encryption of user passwords. Provides secure
 * storage and validation of passwords required for VPN deactivation.
 *
 * Security features:
 * - AES-GCM encryption with Android Keystore master key
 * - Hardware-backed key storage when available
 * - Base64 encoding for encrypted data storage
 */
object PasswordManager {

    // Tink key management constants
    private const val KEYSET_NAME = "master_keyset"
    private const val PREFERENCE_FILE = "master_key_preference"
    private const val MASTER_KEY_URI = "android-keystore://vpn_master_key"

    // DataStore key for encrypted password
    private val PASSWORD_KEY = stringPreferencesKey("vpn_password")

    /**
     * Gets or creates an AEAD (Authenticated Encryption with Associated Data) primitive.
     *
     * Initializes Google Tink with Android Keystore integration for secure
     * encryption operations. Uses AES-256-GCM encryption with hardware-backed
     * key storage when available.
     *
     * @param context Android context for key storage access
     * @return AEAD primitive for encryption/decryption operations
     * @throws SecurityException if cryptographic initialization fails
     */
    private fun getAead(context: Context): Aead {
        try {
            AeadConfig.register()
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
                .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle

            return keysetHandle.getPrimitive(Aead::class.java)
        } catch (e: Exception) {
            throw SecurityException(
                "Failed to initialize cryptographic primitives: ${e.message}",
                e
            )
        }
    }

    /**
     * Checks if a password has been configured for the app.
     *
     * @param context Android context for data access
     * @return true if a password is already set, false otherwise
     */
    fun isPasswordSet(context: Context): Boolean = runBlocking {
        val prefs = context.dataStore.data.first()
        return@runBlocking prefs[PASSWORD_KEY] != null
    }

    /**
     * Securely saves the user's password using encryption.
     *
     * Encrypts the password using AES-GCM and stores it in encrypted DataStore.
     * The encryption key is managed by Android Keystore for maximum security.
     *
     * @param context Android context for data access
     * @param password Plain text password to encrypt and store
     * @throws SecurityException if encryption fails
     */
    fun savePassword(context: Context, password: String) = runBlocking {
        try {
            val aead = getAead(context)
            val encrypted = aead.encrypt(password.toByteArray(), null)
            val encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            context.dataStore.edit { prefs ->
                prefs[PASSWORD_KEY] = encryptedBase64
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to save password securely: ${e.message}", e)
        }
    }

    /**
     * Validates a password against the stored encrypted password.
     *
     * Decrypts the stored password and compares it with the provided password.
     * Returns false if no password is set or if decryption/validation fails.
     *
     * @param context Android context for data access
     * @param password Plain text password to validate
     * @return true if password matches stored password, false otherwise
     */
    fun validatePassword(context: Context, password: String): Boolean = runBlocking {
        try {
            val prefs = context.dataStore.data.first()
            val encryptedBase64 = prefs[PASSWORD_KEY] ?: return@runBlocking false

            val aead = getAead(context)
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val decrypted = aead.decrypt(encryptedBytes, null).decodeToString()
            return@runBlocking decrypted == password
        } catch (e: SecurityException) {
            // Cryptographic error - log and return false
            Log.e("PasswordManager", "Cryptographic validation error: ${e.message}")
            return@runBlocking false
        } catch (e: Exception) {
            // Any other error - log and return false
            Log.e("PasswordManager", "Password validation error: ${e.message}")
            return@runBlocking false
        }
    }
}