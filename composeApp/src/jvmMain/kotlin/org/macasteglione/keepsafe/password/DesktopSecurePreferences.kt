package org.macasteglione.keepsafe.password

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.cleartext.CleartextKeysetHandle
import com.google.crypto.tink.subtle.Base64
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import java.io.File

class DesktopSecurePreferences : SecurePreferences {

    private val settings = Settings()

    init {
        TinkConfig.register()
    }

    private fun getAead(): Aead {
        val keyFile = File("keyset.json")
        val keysetHandle: KeysetHandle = if (keyFile.exists()) {
            CleartextKeysetHandle.read(JsonKeysetReader.withFile(keyFile))
        } else {
            val handle = KeysetHandle.generateNew(AesGcmKeyManager.aes256GcmTemplate())
            CleartextKeysetHandle.write(handle, JsonKeysetWriter.withFile(keyFile))
            handle
        }

        return keysetHandle.getPrimitive(Aead::class.java)
    }

    override fun isPasswordSet(): Boolean {
        return settings.hasKey("vpn_password")
    }

    override fun savePassword(password: String) {
        val encrypted = getAead().encrypt(password.toByteArray(), null)
        settings["vpn_password"] = Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    override fun validatePassword(password: String): Boolean {
        val encryptedBase64 = settings.getStringOrNull("vpn_password") ?: return false
        return try {
            val decrypted = getAead().decrypt(Base64.decode(encryptedBase64, Base64.NO_WRAP), null)
            String(decrypted) == password
        } catch (e: Exception) {
            false
        }
    }
}