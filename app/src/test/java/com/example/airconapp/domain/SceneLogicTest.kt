package com.example.airconapp.domain

import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SceneLogicTest {

    private val off = ControlInfo(pow = "0", mode = "1", stemp = "25", f_rate = "1", f_dir = "0", f_airside = "0", f_auto = "0")
    private val zones = listOf(Zone("Living", true), Zone("Bedroom", false), Zone("Office", true))

    private fun scene(mode: String = "2", temperature: Int = 21, fan: FanSpeed = FanSpeed.AUTO, zones: List<String> = listOf("Bedroom")) =
        Scene(id = "1", name = "Scene", mode = mode, temperature = temperature, fan = fan, zones = zones)

    @Test
    fun `turns the aircon on in the scene's mode and temperature`() {
        val result = SceneLogic.control(off, scene())
        assertEquals("1", result.pow)
        assertEquals("2", result.mode)
        assertEquals("21", result.stemp)
        assertEquals("1", result.f_auto)
    }

    @Test
    fun `fan mode keeps the unit's temperature`() {
        val result = SceneLogic.control(off, scene(mode = SceneLogic.FAN_MODE))
        assertEquals("25", result.stemp)
    }

    @Test
    fun `manual fan speed clears auto`() {
        val result = SceneLogic.control(off.copy(f_auto = "1"), scene(fan = FanSpeed.HIGH))
        assertEquals("5", result.f_rate)
        assertEquals("0", result.f_auto)
    }

    @Test
    fun `opens only the scene's zones`() {
        val result = SceneLogic.zones(zones, scene(zones = listOf("Bedroom", "Office")))
        assertEquals(listOf(Zone("Living", false), Zone("Bedroom", true), Zone("Office", true)), result)
    }

    @Test
    fun `leaves zones alone when none of the scene's zones exist`() {
        assertNull(SceneLogic.zones(zones, scene(zones = listOf("Garage"))))
    }
}
