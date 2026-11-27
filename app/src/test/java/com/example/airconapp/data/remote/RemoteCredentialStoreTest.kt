package com.example.airconapp.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCredentialStoreTest {

    /** In-memory [KeyValueStore] so storage round-trips without Android/Robolectric. */
    private class FakeStore : KeyValueStore {
        private val map = mutableMapOf<String, String>()
        override fun getString(key: String): String? = map[key]
        override fun putString(key: String, value: String) { map[key] = value }
        override fun remove(key: String) { map.remove(key) }
    }

    @Test
    fun `set then get round-trips the credential`() {
        val store = RemoteCredentialStore(FakeStore())
        assertFalse(store.has())
        assertNull(store.get())

        store.set(RemoteCredential(id = "unit-id", spw = "server-pw"))

        assertTrue(store.has())
        assertEquals(RemoteCredential("unit-id", "server-pw"), store.get())
    }

    @Test
    fun `clear removes the credential`() {
        val store = RemoteCredentialStore(FakeStore())
        store.set(RemoteCredential("unit-id", "server-pw"))

        store.clear()

        assertFalse(store.has())
        assertNull(store.get())
    }

    @Test
    fun `a half-written credential is treated as absent`() {
        val backing = FakeStore()
        backing.putString(RemoteCredentialStore.KEY_ID, "unit-id") // spw missing
        val store = RemoteCredentialStore(backing)

        assertFalse(store.has())
        assertNull(store.get())
    }
}
