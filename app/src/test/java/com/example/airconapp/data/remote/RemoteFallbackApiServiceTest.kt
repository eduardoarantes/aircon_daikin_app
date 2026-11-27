package com.example.airconapp.data.remote

import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.ConnectionException
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.RemoteCredentialException
import com.example.airconapp.data.SensorInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.data.ZoneStatusResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RemoteFallbackApiServiceTest {

    private val sample = ControlInfo(ret = "OK", pow = "1", mode = "3", stemp = "24", f_rate = "A", f_dir = "0")

    /** A fake transport; [onCall] runs before each call (to throw), the rest are canned returns. */
    private class FakeApi(
        private val onCall: () -> Unit = {},
        private val control: ControlInfo = ControlInfo(ret = "OK", pow = "1", mode = "3", stemp = "24", f_rate = "A", f_dir = "0"),
        private val setResult: String = "ret=OK",
    ) : AirconApiService {
        var calls = 0
        override suspend fun getControlInfo(): ControlInfo { calls++; onCall(); return control }
        override suspend fun setControlInfo(controlInfo: ControlInfo): String { calls++; onCall(); return setResult }
        override suspend fun getZoneSetting(): ZoneStatusResponse { calls++; onCall(); return ZoneStatusResponse(emptyList()) }
        override suspend fun setZoneSetting(zones: List<Zone>): String { calls++; onCall(); return setResult }
        override suspend fun getSensorInfo(): SensorInfo { calls++; onCall(); return SensorInfo(null, null) }
    }

    private val modes = mutableListOf<TransportMode>()

    private fun service(
        local: () -> AirconApiService,
        cloud: () -> AirconApiService?,
        heal: suspend () -> Boolean = { false },
        reEnrol: suspend () -> Boolean = { false },
    ) = RemoteFallbackApiService(local, cloud, heal, reEnrol, reportMode = { modes.add(it) })

    // --- 3.1 three branches ---

    @Test
    fun `local reachable uses local and never touches the cloud`() = runBlocking {
        val localApi = FakeApi(control = sample)
        var cloudBuilt = 0
        val svc = service(local = { localApi }, cloud = { cloudBuilt++; null })

        assertEquals(sample, svc.getControlInfo())
        assertEquals(0, cloudBuilt) // cloud factory not even consulted
        assertEquals(TransportMode.LOCAL, modes.last())
    }

    @Test
    fun `local unreachable with a credential falls back to the cloud`() = runBlocking {
        val cloudApi = FakeApi(control = sample.copy(stemp = "19"))
        val svc = service(
            local = { FakeApi(onCall = { throw ConnectionException("down") }) },
            cloud = { cloudApi },
        )

        assertEquals("19", svc.getControlInfo().stemp)
        assertEquals(TransportMode.REMOTE, modes.last())
    }

    @Test
    fun `local unreachable without a credential surfaces the connection error`() = runBlocking {
        val svc = service(
            local = { FakeApi(onCall = { throw ConnectionException("down") }) },
            cloud = { null },
        )

        try {
            svc.getControlInfo()
            fail("expected ConnectionException")
        } catch (e: ConnectionException) {
            // expected: local error surfaced unchanged
        }
    }

    @Test
    fun `a moved adapter is healed and local is retried before any cloud fallback`() = runBlocking {
        var attempt = 0
        var cloudBuilt = 0
        // First local build fails; after heal, the second build succeeds.
        val svc = service(
            local = {
                attempt++
                if (attempt == 1) FakeApi(onCall = { throw ConnectionException("moved") }) else FakeApi(control = sample)
            },
            cloud = { cloudBuilt++; FakeApi() },
            heal = { true },
        )

        assertEquals(sample, svc.getControlInfo())
        assertEquals(0, cloudBuilt) // healed locally, cloud untouched
        assertEquals(TransportMode.LOCAL, modes.last())
    }

    @Test
    fun `a write that failed AFTER reaching the adapter is not re-sent over the cloud`() = runBlocking {
        // Response-phase local failure: the command may already have landed locally.
        var cloudBuilt = 0
        val svc = service(
            local = { FakeApi(onCall = { throw ConnectionException("read timed out", connectPhase = false) }) },
            cloud = { cloudBuilt++; FakeApi() },
        )

        try {
            svc.setControlInfo(sample)
            fail("expected the local write error to surface, not a cloud fallback")
        } catch (e: ConnectionException) {
            assertEquals(0, cloudBuilt) // cloud never consulted for this write
        }
    }

    @Test
    fun `a write that never reached the adapter does fall back to the cloud`() = runBlocking {
        // Connect-phase local failure: the command definitely did not land.
        val svc = service(
            local = { FakeApi(onCall = { throw ConnectionException("no route", connectPhase = true) }) },
            cloud = { FakeApi(setResult = "ret=OK") },
        )

        assertEquals("ret=OK", svc.setControlInfo(sample))
        assertEquals(TransportMode.REMOTE, modes.last())
    }

    @Test
    fun `a read always falls back even on a response-phase failure`() = runBlocking {
        val svc = service(
            local = { FakeApi(onCall = { throw ConnectionException("read timed out", connectPhase = false) }) },
            cloud = { FakeApi(control = sample) },
        )

        assertEquals(sample, svc.getControlInfo())
        assertEquals(TransportMode.REMOTE, modes.last())
    }

    // --- 3.2 stale credential ---

    @Test
    fun `stale credential with local access re-enrols and retries once`() = runBlocking {
        var cloudCall = 0
        var reEnrolled = false
        val svc = service(
            local = { FakeApi(onCall = { throw ConnectionException("away") }) },
            cloud = {
                // First cloud attempt is stale; after re-enrol the next one succeeds.
                if (cloudCall == 0) FakeApi(onCall = { cloudCall++; throw RemoteCredentialException("stale") })
                else FakeApi(control = sample)
            },
            reEnrol = { reEnrolled = true; true },
        )

        assertEquals(sample, svc.getControlInfo())
        assertTrue(reEnrolled)
        assertEquals(TransportMode.REMOTE, modes.last())
    }

    @Test
    fun `stale credential while away does not loop and reports needing re-enrolment`() = runBlocking {
        val cloudApi = FakeApi(onCall = { throw RemoteCredentialException("stale") })
        val svc = service(
            local = { FakeApi(onCall = { throw ConnectionException("away") }) },
            cloud = { cloudApi },
            reEnrol = { false }, // cannot reach home to refresh
        )

        try {
            svc.getControlInfo()
            fail("expected RemoteCredentialException")
        } catch (e: RemoteCredentialException) {
            assertEquals(1, cloudApi.calls) // tried once, did not loop
        }
    }

    // --- 3.3 cached-state tolerance on remote writes ---

    @Test
    fun `a remote write succeeds and a following stale read is not treated as a failure`() = runBlocking {
        // Cloud accepts the write (ret=OK) but still serves the pre-write state until the adapter polls.
        val staleState = sample.copy(stemp = "24")
        val cloudApi = FakeApi(control = staleState, setResult = "ret=OK")
        val svc = service(
            local = { FakeApi(onCall = { throw ConnectionException("away") }) },
            cloud = { cloudApi },
        )

        // Write a new temperature over the cloud.
        assertEquals("ret=OK", svc.setControlInfo(sample.copy(stemp = "19")))
        // The next remote read still shows the old value — and that must not raise.
        assertEquals("24", svc.getControlInfo().stemp)
        assertEquals(TransportMode.REMOTE, modes.last())
    }

    // --- 4.1 mode flips local <-> remote ---

    @Test
    fun `transport mode flips to remote on fallback and back to local when local recovers`() = runBlocking {
        var localUp = false
        val cloudApi = FakeApi(control = sample)
        val svc = service(
            local = { if (localUp) FakeApi(control = sample) else FakeApi(onCall = { throw ConnectionException("down") }) },
            cloud = { cloudApi },
        )

        svc.getControlInfo()
        assertEquals(TransportMode.REMOTE, modes.last())

        localUp = true
        svc.getControlInfo()
        assertEquals(TransportMode.LOCAL, modes.last())
    }
}
