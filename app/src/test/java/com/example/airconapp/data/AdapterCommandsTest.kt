package com.example.airconapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AdapterCommandsTest {
    @Test fun `set timezone always sends zone and dst together`() {
        assertEquals("/skyfi/common/set_timezone?zone=345&dst=1", AdapterCommands.setTimeZone(345, true))
        assertEquals("/skyfi/common/set_timezone?zone=283&dst=0", AdapterCommands.setTimeZone(283, false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown zone codes are never sent`() {
        AdapterCommands.setTimeZone(0, true)
    }

    @Test fun `sync time switch`() {
        assertEquals("/skyfi/common/set_sync_time?sync_time=1", AdapterCommands.setTimeSyncDisabled(true))
        assertEquals("/skyfi/common/set_sync_time?sync_time=0", AdapterCommands.setTimeSyncDisabled(false))
    }

    private val sydneyMorning = ZonedDateTime.of(2026, 11, 28, 9, 5, 7, 0, ZoneId.of("Australia/Sydney"))

    @Test fun `disabling sync pushes the phone's local time`() {
        assertEquals(
            "/skyfi/common/notify_date_time?date=2026/11/28&time=09:05:07&zone=GMT",
            AdapterCommands.notifyDateTime(timeSyncDisabled = true, now = sydneyMorning)
        )
    }

    @Test fun `re-enabling sync pushes UTC, as the official app does`() {
        // 09:05 AEDT (UTC+11) is 22:05 the previous day in UTC
        assertEquals(
            "/skyfi/common/notify_date_time?date=2026/11/27&time=22:05:07&zone=GMT",
            AdapterCommands.notifyDateTime(timeSyncDisabled = false, now = sydneyMorning)
        )
    }

    @Test fun `zone table matches the official app`() {
        assertEquals(397, DaikinTimeZones.count)
        assertEquals("Abidjan", DaikinTimeZones.nameOf(1))
        assertEquals("Sydney", DaikinTimeZones.nameOf(345))
        assertEquals("Wellington", DaikinTimeZones.nameOf(397))
        assertNull(DaikinTimeZones.nameOf(398))
        assertEquals(397, DaikinTimeZones.all.map { it.first }.toSet().size)
    }
}
