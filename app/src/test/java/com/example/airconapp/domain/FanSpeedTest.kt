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

    // A schedule saved by the old editor: f_rate "A" or 1–5, no flags
    private val oldSchedule = ControlInfo(pow = "1", mode = "2", stemp = "23", f_rate = "A", f_dir = "0")

    @Test
    fun `old schedules map to the new options`() {
        assertEquals(FanSpeed.AUTO, FanSpeed.ofSchedule(oldSchedule))
        assertEquals(FanSpeed.LOW, FanSpeed.ofSchedule(oldSchedule.copy(f_rate = "1")))
        assertEquals(FanSpeed.LOW, FanSpeed.ofSchedule(oldSchedule.copy(f_rate = "2")))
        assertEquals(FanSpeed.MEDIUM, FanSpeed.ofSchedule(oldSchedule.copy(f_rate = "3")))
        assertEquals(FanSpeed.HIGH, FanSpeed.ofSchedule(oldSchedule.copy(f_rate = "4")))
        assertEquals(FanSpeed.HIGH, FanSpeed.ofSchedule(oldSchedule.copy(f_rate = "5")))
    }

    @Test
    fun `saving a schedule writes values the unit accepts`() {
        val auto = FanSpeed.applyToSchedule(oldSchedule, FanSpeed.AUTO)
        assertEquals("1", auto.f_auto)
        assertEquals("0", auto.f_airside)
        assertEquals("3", auto.f_rate)

        val high = FanSpeed.applyToSchedule(oldSchedule.copy(f_rate = "4"), FanSpeed.HIGH)
        assertEquals("5", high.f_rate)
        assertEquals("0", high.f_auto)
        assertEquals("0", high.f_airside)

        val airside = FanSpeed.applyToSchedule(oldSchedule, FanSpeed.AIRSIDE)
        assertEquals("1", airside.f_airside)
        assertEquals("0", airside.f_auto)
        assertEquals(FanSpeed.AIRSIDE, FanSpeed.ofSchedule(airside))
    }
}
