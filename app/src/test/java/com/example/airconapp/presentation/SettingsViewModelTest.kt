package com.example.airconapp.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.domain.TemperatureUnit
import com.example.airconapp.domain.ThemeMode
import com.example.airconapp.ui.theme.ThemeSetting
import com.example.airconapp.wallpanel.WallPanel
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {
    private val values = mutableMapOf<String, Any>("use_mock_api" to true, "wall_panel_enabled" to false)
    private val savedUnit get() = NetworkModule.getTemperatureUnit()
    private val savedTheme get() = NetworkModule.getThemeMode()
    private val savedSimple get() = NetworkModule.getSimpleModeEnabled()
    private val savedStart get() = NetworkModule.getSimpleModeStartTemperature()
    private val settingsScope = CoroutineScope(Dispatchers.Unconfined)
    private lateinit var vm: SettingsViewModel

    @Before fun setUp() {
        val application = mockk<Application>()
        val prefs = mockk<android.content.SharedPreferences>()
        val editor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        every { application.applicationContext } returns application
        every { application.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.getString(any(), any()) } answers { values[firstArg()] as String? ?: secondArg() }
        every { prefs.getBoolean(any(), any()) } answers { values[firstArg()] as Boolean? ?: secondArg() }
        every { prefs.getInt(any(), any()) } answers { values[firstArg()] as Int? ?: secondArg() }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            val value = secondArg<String?>()
            if (value == null) values.remove(firstArg<String>()) else values[firstArg()] = value
            editor
        }
        every { editor.putBoolean(any(), any()) } answers { values[firstArg()] = secondArg<Boolean>(); editor }
        every { editor.putInt(any(), any()) } answers { values[firstArg()] = secondArg<Int>(); editor }
        NetworkModule.initialize(application)
        mockkObject(ThemeSetting, WallPanel)
        every { ThemeSetting.set(any(), any()) } answers { NetworkModule.setThemeMode(secondArg()) }
        every { WallPanel.setHomeAppEnabled(any(), any()) } just Runs
        mockkStatic("androidx.lifecycle.ViewModelKt")
        every { any<ViewModel>().viewModelScope } returns settingsScope
        vm = SettingsViewModel(application)
    }

    @After fun tearDown() {
        settingsScope.cancel()
        unmockkAll()
    }

    @Test fun `temperature draft can be reverted or discarded without persisting`() {
        assertFalse(vm.hasUnsavedChanges)
        vm.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        assertTrue(vm.hasUnsavedChanges)
        assertEquals(TemperatureUnit.CELSIUS, savedUnit)
        vm.setTemperatureUnit(TemperatureUnit.CELSIUS)
        assertFalse(vm.hasUnsavedChanges)
        vm.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        vm.discardChanges()
        assertEquals(TemperatureUnit.CELSIUS, vm.temperatureUnit.value)
        assertFalse(vm.hasUnsavedChanges)
    }

    @Test fun `every saved field participates in change tracking and discard`() {
        val edits = listOf<() -> Unit>(
            { vm.setThemeMode(ThemeMode.DARK) }, { vm.setApiIp("http://192.168.1.7") },
            { vm.setUseMockMode(false) }, { vm.setSimpleModeEnabled(true) },
            { vm.setWallPanelEnabled(true) }, { vm.setSimpleModeZone("Bedroom") },
            { vm.setSimpleModeStartTemperature(22) }
        )
        edits.forEach { edit ->
            edit()
            assertTrue(vm.hasUnsavedChanges)
            vm.discardChanges()
            assertFalse(vm.hasUnsavedChanges)
        }
    }

    @Test fun `save commits draft and clears pending changes`() {
        vm.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        vm.setThemeMode(ThemeMode.DARK)
        vm.setSimpleModeEnabled(true)
        vm.setSimpleModeZone("Bedroom")
        vm.setSimpleModeStartTemperature(22)
        vm.saveSettings()
        assertEquals(TemperatureUnit.FAHRENHEIT, savedUnit)
        assertEquals(ThemeMode.DARK, savedTheme)
        assertEquals(22, savedStart)
        assertFalse(vm.hasUnsavedChanges)
    }

    @Test fun `simple mode without a zone cannot be saved`() {
        vm.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        vm.setSimpleModeEnabled(true)
        assertFalse(vm.canSaveSettings)
        vm.saveSettings()
        assertEquals(TemperatureUnit.CELSIUS, savedUnit)
        assertFalse(savedSimple)
        assertTrue(vm.hasUnsavedChanges)
    }
}
