package com.example.airconapp.data.remote

import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.ConnectionException
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.RemoteCredentialException
import com.example.airconapp.data.SensorInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.data.ZoneStatusResponse
import kotlinx.coroutines.CancellationException

/** Which transport served the most recent call. */
enum class TransportMode { LOCAL, REMOTE }

/**
 * Wraps the aircon API so that, when the local adapter is unreachable and a cloud credential is
 * stored, the same operation is transparently retried over the cloud relay instead of surfacing a
 * connection error. When no credential is stored, the local [ConnectionException] is surfaced
 * unchanged.
 *
 * Collaborators are injected (so the whole fallback policy is unit-testable):
 * - [local] / [cloud] build a bare [AirconApiService] for each transport ([cloud] is null when no
 *   credential is stored). They are factories, not cached instances, so a healed adapter IP or a
 *   refreshed credential is picked up on the next call.
 * - [heal] rediscovers a moved adapter on the LAN (returns true if its address changed); it is
 *   tried before falling back so a moved adapter at home does not strand the app on the cloud.
 * - [reEnrol] re-derives `spw` from the home network; returns true only when it succeeded (which
 *   implies local reachability), so a stale credential while away does not loop.
 * - [reportMode] publishes the transport that served the call for the UI.
 */
class RemoteFallbackApiService(
    private val local: () -> AirconApiService,
    private val cloud: () -> AirconApiService?,
    private val heal: suspend () -> Boolean,
    private val reEnrol: suspend () -> Boolean,
    private val reportMode: (TransportMode) -> Unit,
) : AirconApiService {

    override suspend fun getControlInfo(): ControlInfo = withFallback(isWrite = false) { it.getControlInfo() }
    override suspend fun setControlInfo(controlInfo: ControlInfo): String = withFallback(isWrite = true) { it.setControlInfo(controlInfo) }
    override suspend fun getZoneSetting(): ZoneStatusResponse = withFallback(isWrite = false) { it.getZoneSetting() }
    override suspend fun setZoneSetting(zones: List<Zone>): String = withFallback(isWrite = true) { it.setZoneSetting(zones) }
    override suspend fun getSensorInfo(): SensorInfo = withFallback(isWrite = false) { it.getSensorInfo() }

    private suspend fun <T> withFallback(isWrite: Boolean, op: suspend (AirconApiService) -> T): T {
        // 1. Prefer the local adapter.
        tryLocal(isWrite, op)?.let { return it.value }

        // 2. Local unreachable: the adapter may have moved. Rediscover and retry local once.
        if (heal()) {
            tryLocal(isWrite, op)?.let { return it.value }
        }

        // 3. Fall back to the cloud relay if remote access is enrolled.
        try {
            val cloudApi = cloud()
                ?: throw ConnectionException("Could not connect to aircon") // no credential: surface local error unchanged

            return try {
                op(cloudApi).also { reportMode(TransportMode.REMOTE) }
            } catch (e: RemoteCredentialException) {
                // Stale `spw`. If we can reach home, refresh it and retry once; otherwise do not loop.
                if (reEnrol()) {
                    val refreshed = cloud() ?: throw e
                    op(refreshed).also { reportMode(TransportMode.REMOTE) }
                } else {
                    throw e
                }
            }
        } catch (e: ConnectionException) {
            // Neither the local adapter nor the cloud relay could be reached: we are not operating
            // remotely, so clear the remote indicator instead of leaving it stuck on from an earlier
            // successful cloud call. (A reachable relay that merely rejects a request keeps REMOTE.)
            reportMode(TransportMode.LOCAL)
            throw e
        }
    }

    /**
     * Runs [op] on the local transport. Returns the boxed result on success (reporting LOCAL), null
     * if the adapter was unreachable ([ConnectionException]) so the caller can fall back. Any other
     * failure (e.g. a parse error, or cancellation) propagates.
     *
     * A **write** that failed *after* reaching the adapter ([ConnectionException.connectPhase] false)
     * is rethrown rather than retried: the command may already have landed locally, so re-sending it
     * over the cloud (which queues to the ~60 s poll) could re-apply a stale command after a newer one.
     */
    private suspend fun <T> tryLocal(isWrite: Boolean, op: suspend (AirconApiService) -> T): Boxed<T>? =
        try {
            Boxed(op(local())).also { reportMode(TransportMode.LOCAL) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ConnectionException) {
            if (isWrite && !e.connectPhase) throw e
            null
        }

    private class Boxed<T>(val value: T)
}
