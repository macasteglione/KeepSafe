package com.macasteglione.keepsafe

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.subtle.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("secure_prefs")

object PasswordManager {
    private const val KEYSET_NAME = "master_keyset"
    private const val PREFERENCE_FILE = "master_key_preference"
    private const val MASTER_KEY_URI = "android-keystore://vpn_master_key"

    private val PASSWORD_KEY = stringPreferencesKey("vpn_password")

    private fun getAead(context: Context): Aead {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
            .withKeyTemplate(com.google.crypto.tink.aead.AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        return keysetHandle.getPrimitive(Aead::class.java)
    }

    fun isPasswordSet(context: Context): Boolean = runBlocking {
        val prefs = context.dataStore.data.first()
        return@runBlocking prefs[PASSWORD_KEY] != null
    }

    fun savePassword(context: Context, password: String) = runBlocking {
        val aead = getAead(context)
        val encrypted = aead.encrypt(password.toByteArray(), null)
        val encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        context.dataStore.edit { prefs ->
            prefs[PASSWORD_KEY] = encryptedBase64
        }
    }

    fun validatePassword(context: Context, password: String): Boolean = runBlocking {
        val prefs = context.dataStore.data.first()
        val encryptedBase64 = prefs[PASSWORD_KEY] ?: return@runBlocking false

        val aead = getAead(context)
        return@runBlocking try {
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val decrypted = aead.decrypt(encryptedBytes, null).decodeToString()
            decrypted == password
        } catch (e: Exception) {
            false
        }
    }
}