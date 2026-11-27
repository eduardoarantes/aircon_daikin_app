package com.example.airconapp.data

/**
 * The aircon could not be reached (local adapter unreachable, or the cloud relay failed).
 *
 * [connectPhase] distinguishes *where* a local failure happened, which decides whether a **write**
 * may be safely retried over the cloud:
 * - `true` — the request never reached the adapter (connect refused/timed out, no route, unknown
 *   host). The command definitely did not land, so a write can fall back to the cloud.
 * - `false` — the adapter was reached but the response timed out (request/read timeout). The write
 *   may already have been applied locally, so it must NOT also be sent via the cloud (which queues
 *   it to the ~60 s poll and could re-apply a stale command after a newer one). Reads are idempotent
 *   and fall back regardless.
 */
class ConnectionException(message: String, val connectPhase: Boolean = true) : Exception(message)

/**
 * The cloud relay reported `ret=ACCOUNT NG`: the stored `spw` is invalid or stale and remote
 * access must be re-enrolled from the home network before cloud calls will work again.
 */
class RemoteCredentialException(message: String) : Exception(message)

/** The cloud relay accepted the request but rejected the operation (a non-OK write result). */
class RemoteException(message: String) : Exception(message)
