package org.macasteglione.keepsafe.password

class PasswordManager(private val securePrefs: SecurePreferences) {
    fun isSet() = securePrefs.isPasswordSet()
    fun save(pass: String) = securePrefs.savePassword(pass)
    fun validate(pass: String) = securePrefs.validatePassword(pass)
}