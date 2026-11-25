package com.example.airconapp.domain

import com.example.airconapp.data.Zone

/**
 * Decides what a single press of the simple-mode on/off button does for one selected zone.
 *
 * - Aircon off: open only the selected zone (close all others), then power on
 *   at the configured start temperature with the airside fan.
 * - Aircon on, selected zone closed: open the selected zone, leave the others as they are.
 * - Aircon on, selected zone open and other zones open: close the selected zone only.
 * - Aircon on, selected zone is the only open zone: power the aircon off (zones untouched).
 */
object SimpleModeLogic {

    const val DEFAULT_START_TEMPERATURE = 21
    val START_TEMPERATURE_RANGE = 16..30
    const val AIRSIDE_ON = "1"

    /**
     * @param zones new zone list to send, or null if zones should not change.
     * @param power new power value ("0"/"1") to send, or null if power should not change.
     *        When both are set, zones must be sent before power.
     * @param temperature set temperature to send with [power], or null to keep the current one.
     * @param airside airside fan flag to send with [power], or null to keep the current one.
     */
    data class Action(
        val zones: List<Zone>?,
        val power: String?,
        val temperature: String? = null,
        val airside: String? = null
    )

    /** "On" from the selected zone's point of view: aircon running and that zone open. */
    fun isOn(isPoweredOn: Boolean, zones: List<Zone>, zoneName: String): Boolean =
        isPoweredOn && zones.firstOrNull { it.name == zoneName }?.isOn == true

    /** Returns null when the selected zone does not exist. */
    fun toggle(
        isPoweredOn: Boolean,
        zones: List<Zone>,
        zoneName: String,
        startTemperature: Int = DEFAULT_START_TEMPERATURE
    ): Action? {
        val index = zones.indexOfFirst { it.name == zoneName }
        if (index < 0) return null
        val selected = zones[index]

        if (!isPoweredOn) {
            val onlySelected = zones.mapIndexed { i, zone -> zone.copy(isOn = i == index) }
            return Action(
                zones = onlySelected,
                power = "1",
                temperature = startTemperature.toString(),
                airside = AIRSIDE_ON
            )
        }

        if (!selected.isOn) {
            val withSelected = zones.toMutableList().apply { this[index] = selected.copy(isOn = true) }
            return Action(zones = withSelected, power = null)
        }

        val othersOpen = zones.anyIndexed { i, zone -> i != index && zone.isOn }
        if (!othersOpen) return Action(zones = null, power = "0")

        val withoutSelected = zones.toMutableList().apply { this[index] = selected.copy(isOn = false) }
        return Action(zones = withoutSelected, power = null)
    }

    private inline fun <T> List<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
        forEachIndexed { i, item -> if (predicate(i, item)) return true }
        return false
    }
}
