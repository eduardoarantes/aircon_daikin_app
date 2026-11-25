package com.example.airconapp.domain

import com.example.airconapp.data.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleModeLogicTest {

    private fun zones(vararg on: Boolean) = on.mapIndexed { i, isOn -> Zone("Z$i", isOn) }

    @Test
    fun `aircon off - opens only selected zone and powers on`() {
        val action = SimpleModeLogic.toggle(false, zones(true, false, true), "Z1")!!
        assertEquals(zones(false, true, false), action.zones)
        assertEquals("1", action.power)
        assertEquals("21", action.temperature)
        assertEquals("1", action.airside)
    }

    @Test
    fun `aircon off - uses configured start temperature`() {
        val action = SimpleModeLogic.toggle(false, zones(false, false), "Z0", startTemperature = 24)!!
        assertEquals("24", action.temperature)
    }

    @Test
    fun `aircon on and selected zone closed - opens it and keeps others`() {
        val action = SimpleModeLogic.toggle(true, zones(true, false, true), "Z1")!!
        assertEquals(zones(true, true, true), action.zones)
        assertNull(action.power)
        assertNull(action.temperature)
        assertNull(action.airside)
    }

    @Test
    fun `aircon on and other zones open - closes selected zone only`() {
        val action = SimpleModeLogic.toggle(true, zones(true, true, false), "Z1")!!
        assertEquals(zones(true, false, false), action.zones)
        assertNull(action.power)
    }

    @Test
    fun `aircon on and selected is the only open zone - powers off`() {
        val action = SimpleModeLogic.toggle(true, zones(false, true, false), "Z1")!!
        assertNull(action.zones)
        assertEquals("0", action.power)
        assertNull(action.temperature)
        assertNull(action.airside)
    }

    @Test
    fun `unknown zone - no action`() {
        assertNull(SimpleModeLogic.toggle(true, zones(true), "missing"))
    }

    @Test
    fun `isOn requires power and selected zone open`() {
        assertTrue(SimpleModeLogic.isOn(true, zones(false, true), "Z1"))
        assertFalse(SimpleModeLogic.isOn(false, zones(false, true), "Z1"))
        assertFalse(SimpleModeLogic.isOn(true, zones(true, false), "Z1"))
    }
}
