package com.example.airconapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AdapterInfoParserTest {
    // Captured from a real AirBase adapter (fw 1_3_0), password removed
    private val basic = "ret=OK,type=aircon,reg=au,dst=1,ver=1_3_0,rev=4A,pow=1,err=0,location=0," +
        "name=%41%69%72%63%6f%6e,icon=0,method=polling,port=30050,id=someone,pw=REDACTED,lpw_flag=0," +
        "adp_kind=3,led=1,en_setzone=1,mac=90E868BD544C,adp_mode=run,ssid=DaikinAP11530,err_type=0," +
        "err_code=0,en_ch=1,holiday=0,en_hol=0,sync_time=0"
    private val datetime = "ret=OK,sta=2,cur=2026/11/28 6:51:11,reg=au,dst=1,zone=345"
    private val remote = "ret=OK,method=polling,notice_ip_int=3600,notice_sync_int=60"

    @Test fun `parses the real adapter replies`() {
        val info = AdapterInfoParser.parse(basic, datetime, remote)
        assertEquals("1.3.0", info.firmware)
        assertEquals("90:E8:68:BD:54:4C", info.macAddress)
        assertEquals(true, info.outOfHome)
        assertEquals(LocalDateTime.of(2026, 11, 28, 6, 51, 11), info.dateTime)
        assertEquals(345, info.timeZoneCode)
        assertEquals("Sydney", info.timeZone)
        assertEquals(true, info.automaticDst)
        assertEquals(false, info.timeSyncDisabled)
    }

    @Test fun `home only means out of home is off`() {
        // The official app writes "home only"; it may come back plain or URL-encoded
        assertEquals(false, AdapterInfoParser.parse(null, null, "ret=OK,method=home only").outOfHome)
        assertEquals(false, AdapterInfoParser.parse(null, null, "ret=OK,method=home%20only").outOfHome)
        assertEquals(false, AdapterInfoParser.parse(null, null, "ret=OK,method=home_only").outOfHome)
    }

    @Test fun `zone codes map to the official app's names, unknown ones show their code`() {
        val info = AdapterInfoParser.parse(null, "ret=OK,cur=2026/1/2 13:04:05,dst=0,zone=283", null)
        assertEquals("Perth", info.timeZone)
        assertEquals("Zone code 999", AdapterInfoParser.parse(null, "ret=OK,zone=999", null).timeZone)
        assertEquals(false, info.automaticDst)
        assertEquals(LocalDateTime.of(2026, 1, 2, 13, 4, 5), info.dateTime)
    }

    @Test fun `missing or failed replies leave fields empty`() {
        val info = AdapterInfoParser.parse(null, "ret=PARAM NG,msg=404 Not Found", "garbage")
        assertNull(info.firmware)
        assertNull(info.macAddress)
        assertNull(info.outOfHome)
        assertNull(info.dateTime)
        assertNull(info.timeZone)
        assertNull(info.automaticDst)
        assertNull(info.timeSyncDisabled)
    }

    @Test fun `password is never part of the parsed info`() {
        assertFalse(AdapterInfoParser.parse(basic, datetime, remote).toString().contains("REDACTED"))
    }

    @Test fun `ok check needs ret OK`() {
        assertTrue(AdapterInfoParser.isOk(basic))
        assertFalse(AdapterInfoParser.isOk("ret=PARAM NG"))
        assertFalse(AdapterInfoParser.isOk(null))
    }

    @Test fun `odd mac is passed through`() {
        assertEquals("ABC", AdapterInfoParser.formatMac("abc"))
        assertNull(AdapterInfoParser.formatMac(" "))
    }
}
