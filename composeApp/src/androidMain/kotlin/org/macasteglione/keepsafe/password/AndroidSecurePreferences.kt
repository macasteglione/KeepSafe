package org.macasteglione.keepsafe.password

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.subtle.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore("secure_prefs")

class AndroidSecurePreferences(private val context: Context) : SecurePreferences {
    private val passwordKey = stringPreferencesKey("vpn_password")

    private fun getAead(): Aead {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "master_keyset", "master_key_preference")
            .withKeyTemplate(com.google.crypto.tink.aead.AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://vpn_master_key")
            .build()
            .keysetHandle

        return keysetHandle.getPrimitive(Aead::class.java)
    }

    override fun isPasswordSet(): Boolean = runBlocking {
        val prefs = context.dataStore.data.first()
        return@runBlocking prefs[passwordKey] != null
    }

    override fun savePassword(password: String): Unit = runBlocking {
        val encrypted = getAead().encrypt(password.toByteArray(), null)
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        context.dataStore.edit { it[passwordKey] = encoded }
    }

    override fun validatePassword(password: String): Boolean = runBlocking {
        val prefs = context.dataStore.data.first()
        val stored = prefs[passwordKey] ?: return@runBlocking false
        return@runBlocking try {
            val decrypted = getAead().decrypt(Base64.decode(stored, Base64.NO_WRAP), null)
            String(decrypted) == password
        } catch (e: Exception) {
            false
        }
    }
}