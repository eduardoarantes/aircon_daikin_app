package com.example.airconapp.domain

import com.example.airconapp.data.ControlInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FanSpeedTest {

    // What the real AirBase adapter reports: auto on, last manual speed high
    private val unit = ControlInfo(pow = "0", mode = "0", stemp = "21", f_rate = "5", f_dir = "0", f_airside = "0", f_auto = "1")

    @Test
    fun `reads auto, airside and the three rates`() {
        assertEquals(FanSpeed.AUTO, FanSpeed.current(unit))
        assertEquals(FanSpeed.AIRSIDE, FanSpeed.current(unit.copy(f_airside = "1")))
        assertEquals(FanSpeed.LOW, FanSpeed.current(unit.copy(f_auto = "0", f_rate = "1")))
        assertEquals(FanSpeed.MEDIUM, FanSpeed.current(unit.copy(f_auto = "0", f_rate = "3")))
        assertEquals(FanSpeed.HIGH, FanSpeed.current(unit.copy(f_auto = "0", f_rate = "5")))
        assertNull(FanSpeed.current(unit.copy(f_auto = "0", f_rate = "2")))
    }

    @Test
    fun `manual speed clears auto and airside`() {
        val sent = FanSpeed.apply(unit.copy(f_airside = "1"), FanSpeed.MEDIUM)
        assertEquals("3", sent.f_rate)
        assertEquals("0", sent.f_auto)
        assertEquals("0", sent.f_airside)
    }

    @Test
    fun `auto keeps the rate and clears airside`() {
        val sent = FanSpeed.apply(unit.copy(f_auto = "0", f_airside = "1"), FanSpeed.AUTO)
        assertEquals("5", sent.f_rate)
        assertEquals("1", sent.f_auto)
        assertEquals("0", sent.f_airside)
    }

    @Test
    fun `airside clears auto`() {
        val sent = FanSpeed.apply(unit, FanSpeed.AIRSIDE)
        assertEquals("1", sent.f_airside)
        assertEquals("0", sent.f_auto)
    }

    @Test
    fun `flags the unit does not report are neither offered nor sent`() {
        val bare = unit.copy(f_auto = null, f_airside = null)
        assertEquals(listOf(FanSpeed.LOW, FanSpeed.MEDIUM, FanSpeed.HIGH), FanSpeed.available(bare))
        val sent = FanSpeed.apply(bare, FanSpeed.LOW)
        assertNull(sent.f_auto)
        assertNull(sent.f_airside)
    }
}
