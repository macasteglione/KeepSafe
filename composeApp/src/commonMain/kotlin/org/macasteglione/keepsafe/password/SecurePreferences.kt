package org.macasteglione.keepsafe.password

interface SecurePreferences {
    fun isPasswordSet(): Boolean
    fun savePassword(password: String)
    fun validatePassword(password: String): Boolean
}