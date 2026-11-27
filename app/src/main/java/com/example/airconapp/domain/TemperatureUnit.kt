package com.example.airconapp.domain

import kotlin.math.roundToInt

/** Display preference only: persisted temperatures and adapter commands remain Celsius. */
enum class TemperatureUnit(val label: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    companion object {
        fun fromName(name: String?): TemperatureUnit = entries.firstOrNull { it.name == name } ?: CELSIUS
    }
}

fun displayTemp(celsius: Int, unit: TemperatureUnit): Int =
    if (unit == TemperatureUnit.FAHRENHEIT) (celsius * 9.0 / 5.0 + 32).roundToInt() else celsius

/** Missing, invalid and non-finite sensor readings have no display value. */
fun displayTemp(celsiusText: String?, unit: TemperatureUnit): String? {
    val celsius = celsiusText?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    val converted = if (unit == TemperatureUnit.FAHRENHEIT) celsius * 9.0 / 5.0 + 32 else celsius
    return converted.takeIf { it.isFinite() && it >= Int.MIN_VALUE && it <= Int.MAX_VALUE }
        ?.roundToInt()?.toString()
}

fun label(unit: TemperatureUnit): String = unit.label
