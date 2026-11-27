package com.example.airconapp.shortcuts

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class ShortcutAuthorizationTest {
    private fun store(): SharedPreferences {
        val values = mutableMapOf<String, String>()
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.getString(any(), any()) } answers { values[firstArg()] ?: secondArg() }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers { values[firstArg()] = secondArg(); editor }
        return prefs
    }

    @Test fun `missing and forged credentials cannot authorize a command`() {
        val auth = ShortcutAuthorization(store())
        assertFalse(auth.accepts(null))
        assertFalse(auth.accepts(""))
        assertFalse(auth.accepts("forged"))
        val token = auth.token()
        assertEquals(64, token.length)
        assertFalse(auth.accepts("0".repeat(64)))
        assertFalse(auth.accepts(token.dropLast(1)))
        assertTrue(auth.accepts(token))
    }

    @Test fun `authorization survives publisher recreation and is isolated per installation`() {
        val prefs = store()
        val token = ShortcutAuthorization(prefs).token()
        val recreated = ShortcutAuthorization(prefs)
        assertEquals(token, recreated.token())
        assertTrue(recreated.accepts(token))
        assertFalse(ShortcutAuthorization(store()).accepts(token))
        assertNotEquals(token, ShortcutAuthorization(store()).token())
    }
}
