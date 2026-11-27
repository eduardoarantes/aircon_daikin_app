package com.example.airconapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemperatureUnitTest {
    @Test fun `setpoints convert for display only`() {
        listOf(16 to 61, 21 to 70, 22 to 72, 32 to 90, 23 to 73).forEach { (c, f) ->
            assertEquals(f, displayTemp(c, TemperatureUnit.FAHRENHEIT))
            assertEquals(c, displayTemp(c, TemperatureUnit.CELSIUS))
            assertEquals(f.toString(), displayTemp(c.toString(), TemperatureUnit.FAHRENHEIT))
        }
    }

    @Test fun `sensor readings round after conversion`() {
        assertEquals("72", displayTemp("22.0", TemperatureUnit.FAHRENHEIT))
        assertEquals("73", displayTemp("22.5", TemperatureUnit.FAHRENHEIT))
        assertEquals("23", displayTemp("22.5", TemperatureUnit.CELSIUS))
        assertEquals("22", displayTemp(" 22.0 ", TemperatureUnit.CELSIUS))
        listOf(null, "-", "", " ", "missing", "NaN", "Infinity", "1e100").forEach {
            TemperatureUnit.entries.forEach { unit -> assertNull(displayTemp(it, unit)) }
        }
    }

    @Test fun `names default safely and labels match units`() {
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromName(null))
        assertEquals(TemperatureUnit.CELSIUS, TemperatureUnit.fromName("unknown"))
        TemperatureUnit.entries.forEach { assertEquals(it, TemperatureUnit.fromName(it.name)) }
        assertEquals("°C", label(TemperatureUnit.CELSIUS))
        assertEquals("°F", label(TemperatureUnit.FAHRENHEIT))
    }
}
