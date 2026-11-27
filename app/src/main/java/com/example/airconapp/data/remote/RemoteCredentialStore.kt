package com.example.airconapp.data.remote

/** The cloud credential: the adapter `id` and the derived server password `spw`. */
data class RemoteCredential(val id: String, val spw: String)

/**
 * Minimal persistence surface so the credential logic can be tested without Android.
 * [NetworkModule] backs this with `SharedPreferences`; tests back it with an in-memory map.
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

/**
 * Stores the cloud credential (`id` + `spw`) on the device and nowhere else. The values are
 * confidential (they authenticate to the relay) and must never be logged — see the
 * `add-remote-cloud-control` change, Credential confidentiality.
 */
class RemoteCredentialStore(private val store: KeyValueStore) {

    fun get(): RemoteCredential? {
        val id = store.getString(KEY_ID)?.takeIf { it.isNotBlank() } ?: return null
        val spw = store.getString(KEY_SPW)?.takeIf { it.isNotBlank() } ?: return null
        return RemoteCredential(id, spw)
    }

    fun set(credential: RemoteCredential) {
        store.putString(KEY_ID, credential.id)
        store.putString(KEY_SPW, credential.spw)
    }

    fun clear() {
        store.remove(KEY_ID)
        store.remove(KEY_SPW)
    }

    fun has(): Boolean = get() != null

    companion object {
        const val KEY_ID = "remote_id"
        const val KEY_SPW = "remote_spw"
    }
}
