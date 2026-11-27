package com.example.airconapp.di

import android.content.Context
import android.content.SharedPreferences
import com.example.airconapp.domain.TemperatureUnit
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureUnitPreferenceTest {
    @Test fun `preference defaults persists and reloads into shared state`() {
        val values = mutableMapOf<String, String>()
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.getString(any(), any()) } answers { values[firstArg()] ?: secondArg() }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            values[firstArg()] = secondArg()
            editor
        }
        NetworkModule.initialize(context)
        assertEquals(TemperatureUnit.CELSIUS, NetworkModule.getTemperatureUnit())
        TemperatureUnit.entries.reversed().forEach { unit ->
            NetworkModule.setTemperatureUnit(unit)
            assertEquals(unit, NetworkModule.getTemperatureUnit())
            assertEquals(unit, NetworkModule.temperatureUnit.value)
            NetworkModule.initialize(context)
            assertEquals(unit, NetworkModule.temperatureUnit.value)
        }
        values["temperature_unit"] = "invalid"
        NetworkModule.initialize(context)
        assertEquals(TemperatureUnit.CELSIUS, NetworkModule.temperatureUnit.value)
    }
}
