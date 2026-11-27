package com.example.airconapp.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteEnrollerTest {

    // Synthetic values only — never real id/pw/spw.
    private val basicInfoOk = "ret=OK,type=aircon,reg=au,id=unit-id,pw=adapter-pw,name=Home"
    private val spwOk = "ret=OK,spw=server-pw"

    @Test
    fun `successful enrolment derives spw from the adapter pw and stores id plus spw`() = runBlocking {
        var capturedId: String? = null
        var capturedApw: String? = null
        var stored: RemoteCredential? = null

        val enroller = RemoteEnroller(
            readBasicInfo = { basicInfoOk },
            fetchSpw = { id, apw -> capturedId = id; capturedApw = apw; spwOk },
            store = { stored = it },
        )

        val result = enroller.enrol()

        assertEquals(EnrolResult.Success, result)
        // get_spw is called with the adapter's id and its pw as apw (the adapter pw is NOT the spw).
        assertEquals("unit-id", capturedId)
        assertEquals("adapter-pw", capturedApw)
        assertEquals(RemoteCredential(id = "unit-id", spw = "server-pw"), stored)
    }

    @Test
    fun `a non-OK get_spw response is rejected and nothing is stored`() = runBlocking {
        var stored: RemoteCredential? = null
        val enroller = RemoteEnroller(
            readBasicInfo = { basicInfoOk },
            fetchSpw = { _, _ -> "ret=ACCOUNT NG" },
            store = { stored = it },
        )

        assertEquals(EnrolResult.Rejected, enroller.enrol())
        assertNull(stored)
    }

    @Test
    fun `enrolment without local access returns a connect-to-home outcome and never calls the relay`() = runBlocking {
        var spwCalled = false
        val enroller = RemoteEnroller(
            readBasicInfo = { null }, // adapter not reachable on the LAN
            fetchSpw = { _, _ -> spwCalled = true; spwOk },
            store = { },
        )

        assertEquals(EnrolResult.NotOnLocalNetwork, enroller.enrol())
        assertEquals(false, spwCalled)
    }
}
