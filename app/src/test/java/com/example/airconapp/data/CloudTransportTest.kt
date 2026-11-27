package com.example.airconapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudTransportTest {

    private val local = Endpoint("http://192.168.1.6")
    private val cloud = CloudRelay.endpoint(id = "unit-id", spw = "server-pw")

    // --- 1.1 built URL per op, local vs cloud ---

    @Test
    fun `local GET urls carry no query`() {
        assertEquals(
            "http://192.168.1.6/skyfi/aircon/get_control_info",
            buildAirconUrl(local, "get_control_info")
        )
        assertEquals(
            "http://192.168.1.6/skyfi/aircon/get_zone_setting",
            buildAirconUrl(local, "get_zone_setting")
        )
        assertEquals(
            "http://192.168.1.6/skyfi/aircon/get_sensor_info",
            buildAirconUrl(local, "get_sensor_info")
        )
    }

    @Test
    fun `cloud GET urls append id spw device port with a single question mark`() {
        assertEquals(
            "https://dascontroller.com/skyfi/aircon/get_control_info?id=unit-id&spw=server-pw&device=A&port=30050",
            buildAirconUrl(cloud, "get_control_info")
        )
        assertEquals(
            "https://dascontroller.com/skyfi/aircon/get_sensor_info?id=unit-id&spw=server-pw&device=A&port=30050",
            buildAirconUrl(cloud, "get_sensor_info")
        )
    }

    // --- 1.2 query helper: already-queried writes, no malformed ?...?..., encoding preserved ---

    @Test
    fun `local write keeps its own query`() {
        assertEquals(
            "http://192.168.1.6/skyfi/aircon/set_control_info?pow=1&mode=3&stemp=24",
            buildAirconUrl(local, "set_control_info", "pow=1&mode=3&stemp=24")
        )
    }

    @Test
    fun `cloud write appends auth after the existing query, not a second question mark`() {
        val url = buildAirconUrl(cloud, "set_control_info", "pow=1&mode=3&stemp=24")
        assertEquals(
            "https://dascontroller.com/skyfi/aircon/set_control_info?pow=1&mode=3&stemp=24&id=unit-id&spw=server-pw&device=A&port=30050",
            url
        )
        // Exactly one '?' separates path from query.
        assertEquals(1, url.count { it == '?' })
    }

    @Test
    fun `existing url-encoded zone params are preserved and auth appended`() {
        // The caller encodes zone params (spaces as %20); the builder must not touch them.
        val encodedZones = "zone_onoff=1%3B0&zone_name=Living%20Room%3BBed%201"
        val url = buildAirconUrl(cloud, "set_zone_setting", encodedZones)
        assertEquals(
            "https://dascontroller.com/skyfi/aircon/set_zone_setting?" +
                "zone_onoff=1%3B0&zone_name=Living%20Room%3BBed%201&id=unit-id&spw=server-pw&device=A&port=30050",
            url
        )
    }

    // --- cloud response classification (drives the write path and stale-credential handling) ---

    @Test
    fun `read with ret OK or no ret classifies as OK`() {
        assertEquals(CloudOutcome.OK, classifyCloudResponse("ret=OK,pow=1,mode=3", isWrite = false))
        assertEquals(CloudOutcome.OK, classifyCloudResponse("pow=1,mode=3", isWrite = false))
    }

    @Test
    fun `account NG is a stale credential on both reads and writes`() {
        assertEquals(CloudOutcome.STALE_CREDENTIAL, classifyCloudResponse("ret=ACCOUNT NG", isWrite = false))
        assertEquals(CloudOutcome.STALE_CREDENTIAL, classifyCloudResponse("ret=ACCOUNT NG", isWrite = true))
    }

    @Test
    fun `writes require an explicit ret OK`() {
        assertEquals(CloudOutcome.OK, classifyCloudResponse("ret=OK", isWrite = true))
        assertEquals(CloudOutcome.REJECTED, classifyCloudResponse("ret=PARAM NG", isWrite = true))
        assertEquals(CloudOutcome.REJECTED, classifyCloudResponse("pow=1,mode=3", isWrite = true))
    }
}
