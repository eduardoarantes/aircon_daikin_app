package com.example.airconapp.data.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DaikinDiscoveryTest {

    @Test
    fun `parses aircon reply and decodes the name`() {
        val reply = "ret=OK,type=aircon,reg=au,ver=1_3_0,name=%48%6f%6d%65%20%41%43,port=30050"
        assertEquals(DiscoveredUnit("192.168.1.10", "Home AC"), DaikinDiscovery.parseReply("192.168.1.10", reply))
    }

    @Test
    fun `missing name is null`() {
        assertEquals(DiscoveredUnit("10.0.0.5", null), DaikinDiscovery.parseReply("10.0.0.5", "ret=OK,type=aircon"))
    }

    @Test
    fun `rejects non-aircon and error replies`() {
        assertNull(DaikinDiscovery.parseReply("10.0.0.5", "ret=OK,type=other"))
        assertNull(DaikinDiscovery.parseReply("10.0.0.5", "ret=PARAM NG,type=aircon"))
        assertNull(DaikinDiscovery.parseReply("10.0.0.5", "garbage"))
    }
}
