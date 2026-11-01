package com.kalaban.farshidb2011.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.pinStore by preferencesDataStore("kalaban_secure")

class SecurePrefs(private val context: Context) {
    private val pinHashKey = stringPreferencesKey("admin_pin_hash")
    private val pinSaltKey = stringPreferencesKey("admin_pin_salt")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    /** In-memory only — unlocking must not survive process death. PIN hash stays in DataStore. */
    private val _sessionUnlocked = MutableStateFlow(false)
    val sessionUnlocked: Flow<Boolean> = _sessionUnlocked

    val pinHash: Flow<String?> = context.pinStore.data.map { it[pinHashKey] }

    /** "system", "light", or "dark" — null if not explicitly set in DataStore yet. */
    val themeMode: Flow<String?> = context.pinStore.data.map { it[themeModeKey] }

    suspend fun setThemeMode(mode: String) {
        context.pinStore.edit {
            it[themeModeKey] = mode
        }
    }

    suspend fun setPin(pin: String) {
        val salt = randomSalt()
        context.pinStore.edit {
            it[pinSaltKey] = salt
            it[pinHashKey] = hash(pin, salt)
        }
    }

    suspend fun hasPin(): Boolean = !context.pinStore.data.first()[pinHashKey].isNullOrBlank()

    suspend fun checkPin(pin: String): Boolean {
        val prefs = context.pinStore.data.first()
        val stored = prefs[pinHashKey] ?: return false
        val salt = prefs[pinSaltKey]
        return if (salt.isNullOrBlank()) {
            // Legacy unsalted hash migration path
            stored == legacyHash(pin)
        } else {
            stored == hash(pin, salt)
        }
    }

    suspend fun setSessionUnlocked(unlocked: Boolean) {
        _sessionUnlocked.value = unlocked
    }

    companion object {
        private fun randomSalt(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun hash(pin: String, salt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            // Stretch a bit for stronger local PIN storage without external crypto libs.
            var value = (salt + pin).toByteArray()
            repeat(10_000) {
                value = digest.digest(value)
            }
            return value.joinToString("") { "%02x".format(it) }
        }

        private fun legacyHash(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
