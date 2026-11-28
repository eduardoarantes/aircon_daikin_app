package com.example.airconapp.domain

/** Which background alerts the user has switched on in Settings. */
data class AlertSettings(
    /** Alert when the aircon has been running for [AlertMonitor.OVERRUN_MILLIS] without a break. */
    val overrun: Boolean = false,
    /** Alert when the aircon is turned on or off from somewhere other than this app. */
    val powerChange: Boolean = false,
    /** Alert when the operation mode is changed from somewhere other than this app. */
    val modeChange: Boolean = false
) {
    val any: Boolean get() = overrun || powerChange || modeChange

    companion object {
        val NONE = AlertSettings()
    }
}

/**
 * What the monitor last knew about the unit. Empty (all null) until the first reading, which only
 * sets the baseline: there is nothing to compare it with yet.
 */
data class MonitorState(
    val power: Boolean? = null,
    val mode: String? = null,
    /** When the current run started (or was first seen running); null while off. */
    val onSinceMillis: Long? = null,
    /** The overrun alert for the current run has been sent, so it isn't repeated. */
    val overrunNotified: Boolean = false
)

sealed interface Alert {
    data object Overrun : Alert
    data class PowerChanged(val isOn: Boolean) : Alert
    data class ModeChanged(val fromMode: String, val toMode: String) : Alert
}

/**
 * Turns periodic readings of the unit into alerts. Pure: the caller stores the returned state.
 *
 * Changes this app makes itself are passed to [recordOwnChange] instead, which moves the baseline
 * without alerting, so only changes from elsewhere (the wall controller, the Daikin app, another
 * phone) raise power and mode alerts.
 */
object AlertMonitor {

    const val OVERRUN_HOURS = 12
    const val OVERRUN_MILLIS = OVERRUN_HOURS * 60 * 60 * 1000L

    fun observe(
        state: MonitorState,
        pow: String,
        mode: String,
        nowMillis: Long,
        settings: AlertSettings
    ): Pair<MonitorState, List<Alert>> {
        val isOn = pow == "1"
        val alerts = mutableListOf<Alert>()
        val known = state.power != null

        if (known && settings.powerChange && state.power != isOn) alerts += Alert.PowerChanged(isOn)
        if (known && settings.modeChange && state.mode != null && state.mode != mode) {
            alerts += Alert.ModeChanged(state.mode, mode)
        }

        val onSince = when {
            !isOn -> null
            state.power == true -> state.onSinceMillis ?: nowMillis
            else -> nowMillis // just turned on, or first reading
        }
        var overrunNotified = isOn && state.power == true && state.overrunNotified
        if (settings.overrun && isOn && !overrunNotified && onSince != null &&
            nowMillis - onSince >= OVERRUN_MILLIS
        ) {
            alerts += Alert.Overrun
            overrunNotified = true
        }

        return MonitorState(isOn, mode, onSince, overrunNotified) to alerts
    }

    /** Moves the baseline to a state this app just sent, without raising any alert. */
    fun recordOwnChange(state: MonitorState, pow: String, mode: String, nowMillis: Long): MonitorState =
        observe(state, pow, mode, nowMillis, AlertSettings.NONE).first

    /** Daikin AirBase mode codes as shown in the app. */
    fun modeLabel(code: String): String = when (code) {
        "0" -> "Fan"
        "1" -> "Heat"
        "2" -> "Cool"
        "3" -> "Auto"
        "7" -> "Dry"
        else -> "mode $code"
    }
}
