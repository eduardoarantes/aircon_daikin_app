package com.example.airconapp.domain

import com.example.airconapp.data.Zone
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoneUsageTest {

    private val zones = listOf(Zone("Living", false), Zone("Bedroom", false), Zone("Kitchen", false), Zone("Office", false))
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `no history shows the first zones in device order`() {
        assertEquals(listOf(0, 1), ZoneUsage.mostUsed(zones, emptyMap(), 0))
    }

    @Test
    fun `most toggled zones come first`() {
        var usage = emptyMap<String, ZoneUsage>()
        repeat(3) { usage = ZoneUsage.record(usage, "Kitchen", 0) }
        usage = ZoneUsage.record(usage, "Office", 0)
        assertEquals(listOf(2, 3), ZoneUsage.mostUsed(zones, usage, 0))
    }

    @Test
    fun `recent use beats old use`() {
        var usage = emptyMap<String, ZoneUsage>()
        // Kitchen toggled 4 times two months ago, Office twice this week
        repeat(4) { usage = ZoneUsage.record(usage, "Kitchen", 0) }
        repeat(2) { usage = ZoneUsage.record(usage, "Office", 58 * day) }
        assertEquals(listOf(3, 2), ZoneUsage.mostUsed(zones, usage, 60 * day))
    }

    @Test
    fun `score halves every half life`() {
        val usage = ZoneUsage.record(emptyMap(), "Living", 0)
        assertEquals(0.5, usage.getValue("Living").scoreAt(ZoneUsage.HALF_LIFE_MILLIS), 1e-9)
    }

    @Test
    fun `fewer zones than tiles shows what exists`() {
        assertEquals(listOf(0), ZoneUsage.mostUsed(zones.take(1), emptyMap(), 0))
        assertEquals(emptyList<Int>(), ZoneUsage.mostUsed(emptyList(), emptyMap(), 0))
    }

    @Test
    fun `encodes and decodes, skipping bad lines`() {
        val usage = mapOf("Guest Main" to ZoneUsage(2.5, 123L), "Edu" to ZoneUsage(1.0, 456L))
        assertEquals(usage, ZoneUsage.decode(ZoneUsage.encode(usage)))
        assertEquals(emptyMap<String, ZoneUsage>(), ZoneUsage.decode("garbage\nx\ty\tz"))
        assertEquals(emptyMap<String, ZoneUsage>(), ZoneUsage.decode(null))
    }
}
