package com.example.airconapp.shortcuts

import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

/** An unguessable capability carried only in shortcuts published by this installation. */
class ShortcutAuthorization(private val prefs: SharedPreferences) {
    fun token(): String = synchronized(tokenLock) {
        prefs.getString(KEY_TOKEN, null)?.let { return@synchronized it }
        val token = ByteArray(32).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_TOKEN, token).apply()
        token
    }

    fun accepts(provided: String?): Boolean {
        val expected = prefs.getString(KEY_TOKEN, null) ?: return false
        if (provided == null || provided.length != expected.length) return false
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), provided.toByteArray(Charsets.UTF_8))
    }

    companion object {
        const val PREFS_NAME = "aircon_shortcut_auth"
        const val EXTRA_TOKEN = "shortcut_token"
        private val tokenLock = Any()
        private const val KEY_TOKEN = "token"
    }
}
