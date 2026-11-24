package com.example.airconapp.domain

import com.example.airconapp.data.ControlInfo

/**
 * Fan options shown in the app, mapped to the Daikin AirBase fields:
 * - Low / Medium / High: `f_rate` 1 / 3 / 5 (the unit reports `frate_steps=3`)
 * - Auto: the separate `f_auto=1` flag (`f_rate` keeps the last manual speed)
 * - Airside: the separate `f_airside=1` flag
 */
enum class FanSpeed(val shortLabel: String, val label: String) {
    AUTO("Auto", "Auto"),
    LOW("Low", "Low"),
    MEDIUM("Med", "Medium"),
    HIGH("High", "High"),
    AIRSIDE("Airside", "Airside");

    companion object {
        private val RATES = mapOf(LOW to "1", MEDIUM to "3", HIGH to "5")

        /** The option the unit is currently on, or null if it reports a value we don't know. */
        fun current(info: ControlInfo): FanSpeed? = when {
            info.f_airside == "1" -> AIRSIDE
            info.f_auto == "1" -> AUTO
            else -> RATES.entries.firstOrNull { it.value == info.f_rate }?.key
        }

        /** Options this unit supports: Auto and Airside only when it reports their flags. */
        fun available(info: ControlInfo): List<FanSpeed> = entries.filter {
            when (it) {
                AUTO -> info.f_auto != null
                AIRSIDE -> info.f_airside != null
                else -> true
            }
        }

        /**
         * The fan option a saved schedule uses. Schedules saved before the redesign store `f_rate`
         * "A" or 1–5 and no Auto/Airside flags: "A" is Auto, 1–2 Low, 3 Medium, 4–5 High.
         */
        fun ofSchedule(info: ControlInfo): FanSpeed = current(info) ?: when (info.f_rate) {
            "1", "2" -> LOW
            "3" -> MEDIUM
            "4", "5" -> HIGH
            else -> AUTO
        }

        /**
         * The control info a schedule stores for [speed]. It always carries both flags (the unit
         * supports them) and a rate the unit accepts, so old "A"/2/4 values are never sent again.
         */
        fun applyToSchedule(info: ControlInfo, speed: FanSpeed): ControlInfo = apply(
            info.copy(
                f_rate = info.f_rate.takeIf { it in RATES.values } ?: RATES.getValue(MEDIUM),
                f_auto = info.f_auto ?: "0",
                f_airside = info.f_airside ?: "0"
            ),
            speed
        )

        /** The control info to send to select [speed]. Flags the unit doesn't report stay unsent. */
        fun apply(info: ControlInfo, speed: FanSpeed): ControlInfo {
            val autoOff = info.f_auto?.let { "0" }
            val airsideOff = info.f_airside?.let { "0" }
            return when (speed) {
                AUTO -> info.copy(f_auto = "1", f_airside = airsideOff)
                AIRSIDE -> info.copy(f_airside = "1", f_auto = autoOff)
                else -> info.copy(f_rate = RATES.getValue(speed), f_auto = autoOff, f_airside = airsideOff)
            }
        }
    }
}
