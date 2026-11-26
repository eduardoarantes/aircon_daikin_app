package com.example.airconapp.domain

import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.Zone

/**
 * A saved preset, e.g. "Bedroom cool": open these zones (closing the rest), turn the aircon on
 * in [mode] at [temperature] with [fan]. Shown in the Scenes tab and as shortcuts when the app
 * icon is long-pressed.
 *
 * @param mode Daikin mode code (see `AirconMode`).
 * @param zones names of the zones to open; every other zone is closed. Zones are matched by
 *        name because the API only identifies them by position and name.
 */
data class Scene(
    val id: String,
    val name: String,
    val mode: String,
    val temperature: Int,
    val fan: FanSpeed,
    val zones: List<String>
)

object SceneLogic {

    /** Daikin's fan-only mode has no set temperature. */
    const val FAN_MODE = "0"

    /** The unit's own temperature range, same as the Controls dial. */
    val TEMPERATURE_RANGE = 16..32

    /** The control info to send for [scene], starting from what the unit has now. */
    fun control(current: ControlInfo, scene: Scene): ControlInfo = FanSpeed.apply(
        current.copy(
            pow = "1",
            mode = scene.mode,
            // Fan mode keeps whatever the unit has: it ignores the temperature anyway
            stemp = if (scene.mode == FAN_MODE) current.stemp else scene.temperature.toString()
        ),
        scene.fan
    )

    /**
     * The zone list to send for [scene]: its zones open, all others closed. Null when none of
     * the scene's zones exist on the unit (e.g. renamed), so the zones are left untouched
     * rather than all being closed.
     */
    fun zones(current: List<Zone>, scene: Scene): List<Zone>? {
        if (current.none { it.name in scene.zones }) return null
        return current.map { it.copy(isOn = it.name in scene.zones) }
    }
}
