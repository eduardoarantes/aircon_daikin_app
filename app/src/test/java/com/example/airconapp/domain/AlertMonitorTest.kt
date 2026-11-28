package com.example.airconapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertMonitorTest {
    private val all = AlertSettings(overrun = true, powerChange = true, modeChange = true)
    private val hour = 60 * 60 * 1000L

    private fun observe(state: MonitorState, pow: String, mode: String, now: Long, settings: AlertSettings = all) =
        AlertMonitor.observe(state, pow, mode, now, settings)

    @Test fun `first reading only sets the baseline`() {
        val (state, alerts) = observe(MonitorState(), "1", "2", 0)
        assertTrue(alerts.isEmpty())
        assertEquals(MonitorState(power = true, mode = "2", onSinceMillis = 0, overrunNotified = false), state)
    }

    @Test fun `power change from elsewhere alerts`() {
        val (on, _) = observe(MonitorState(), "0", "2", 0)
        val (off, alerts) = observe(on, "1", "2", hour)
        assertEquals(listOf(Alert.PowerChanged(true)), alerts)
        assertEquals(listOf(Alert.PowerChanged(false)), observe(off, "0", "2", 2 * hour).second)
    }

    @Test fun `mode change from elsewhere alerts`() {
        val (state, _) = observe(MonitorState(), "1", "2", 0)
        assertEquals(listOf(Alert.ModeChanged("2", "1")), observe(state, "1", "1", hour).second)
    }

    @Test fun `disabled alerts stay quiet`() {
        val (state, _) = observe(MonitorState(), "0", "2", 0)
        val settings = AlertSettings(overrun = true)
        assertTrue(observe(state, "1", "1", hour, settings).second.isEmpty())
    }

    @Test fun `own change moves the baseline without alerting`() {
        val (state, _) = observe(MonitorState(), "0", "2", 0)
        val own = AlertMonitor.recordOwnChange(state, "1", "1", hour)
        assertTrue(observe(own, "1", "1", 2 * hour).second.isEmpty())
    }

    @Test fun `overrun alerts once per run after 12 hours`() {
        var state = observe(MonitorState(), "0", "2", 0).first
        state = observe(state, "1", "2", hour, AlertSettings(overrun = true)).first
        val (justBefore, none) = observe(state, "1", "2", 12 * hour, AlertSettings(overrun = true))
        assertTrue(none.isEmpty())
        val (after, alerts) = observe(justBefore, "1", "2", 13 * hour, AlertSettings(overrun = true))
        assertEquals(listOf(Alert.Overrun), alerts)
        assertTrue(observe(after, "1", "2", 20 * hour, AlertSettings(overrun = true)).second.isEmpty())

        // Turning off ends the run; the next long run warns again
        val off = observe(after, "0", "2", 21 * hour).first
        val on = observe(off, "1", "2", 22 * hour).first
        assertEquals(listOf(Alert.Overrun), observe(on, "1", "2", 34 * hour, AlertSettings(overrun = true)).second)
    }

    @Test fun `own changes keep the run going for overrun`() {
        val start = observe(MonitorState(), "1", "2", 0).first
        val tempChange = AlertMonitor.recordOwnChange(start, "1", "1", 5 * hour)
        assertEquals(listOf(Alert.Overrun), observe(tempChange, "1", "1", 12 * hour, AlertSettings(overrun = true)).second)
    }
}
