package com.example.airconapp.alerts

import android.content.SharedPreferences
import com.example.airconapp.domain.MonitorState

/**
 * Keeps the alert monitor's [MonitorState] between background runs. Updates are serialised so
 * the background check and this app's own changes can't overwrite each other.
 */
class AlertStateStore(private val prefs: SharedPreferences) {

    @Synchronized
    fun <T> update(block: (MonitorState) -> Pair<MonitorState, T>): T {
        val (next, result) = block(read())
        write(next)
        return result
    }

    /** Forgets what was last seen, so the next reading becomes a fresh baseline. */
    @Synchronized
    fun reset() {
        prefs.edit().clear().apply()
    }

    private fun read(): MonitorState = MonitorState(
        power = if (prefs.contains(KEY_POWER)) prefs.getBoolean(KEY_POWER, false) else null,
        mode = prefs.getString(KEY_MODE, null),
        onSinceMillis = if (prefs.contains(KEY_ON_SINCE)) prefs.getLong(KEY_ON_SINCE, 0L) else null,
        overrunNotified = prefs.getBoolean(KEY_OVERRUN_NOTIFIED, false)
    )

    private fun write(state: MonitorState) {
        prefs.edit().apply {
            if (state.power == null) remove(KEY_POWER) else putBoolean(KEY_POWER, state.power)
            if (state.mode == null) remove(KEY_MODE) else putString(KEY_MODE, state.mode)
            if (state.onSinceMillis == null) remove(KEY_ON_SINCE) else putLong(KEY_ON_SINCE, state.onSinceMillis)
            putBoolean(KEY_OVERRUN_NOTIFIED, state.overrunNotified)
        }.apply()
    }

    private companion object {
        const val KEY_POWER = "power"
        const val KEY_MODE = "mode"
        const val KEY_ON_SINCE = "on_since"
        const val KEY_OVERRUN_NOTIFIED = "overrun_notified"
    }
}
