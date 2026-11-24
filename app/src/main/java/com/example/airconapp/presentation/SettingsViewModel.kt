package com.example.airconapp.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.domain.SimpleModeLogic
import com.example.airconapp.wallpanel.WallPanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _apiIp = MutableStateFlow(NetworkModule.getBaseUrl())
    val apiIp: StateFlow<String> = _apiIp.asStateFlow()

    private val _useMockMode = MutableStateFlow(NetworkModule.getUseMockApi())
    val useMockMode: StateFlow<Boolean> = _useMockMode.asStateFlow()

    private val _simpleModeEnabled = MutableStateFlow(NetworkModule.getSimpleModeEnabled())
    val simpleModeEnabled: StateFlow<Boolean> = _simpleModeEnabled.asStateFlow()

    private val _simpleModeZone = MutableStateFlow(NetworkModule.getSimpleModeZone())
    val simpleModeZone: StateFlow<String?> = _simpleModeZone.asStateFlow()

    private val _simpleModeStartTemperature = MutableStateFlow(NetworkModule.getSimpleModeStartTemperature())
    val simpleModeStartTemperature: StateFlow<Int> = _simpleModeStartTemperature.asStateFlow()

    fun setSimpleModeStartTemperature(temperature: Int) {
        _simpleModeStartTemperature.value = temperature.coerceIn(SimpleModeLogic.START_TEMPERATURE_RANGE)
    }

    /** Zone names from the aircon; null while loading or if the aircon could not be reached. */
    private val _availableZones = MutableStateFlow<List<String>?>(null)
    val availableZones: StateFlow<List<String>?> = _availableZones.asStateFlow()

    private val _zonesLoading = MutableStateFlow(true)
    val zonesLoading: StateFlow<Boolean> = _zonesLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _availableZones.value = runCatching {
                NetworkModule.airconApiService.getZoneSetting().zones.map { it.name }
            }.getOrNull()
            _zonesLoading.value = false
        }
    }

    fun setSimpleModeEnabled(enabled: Boolean) {
        _simpleModeEnabled.value = enabled
    }

    fun setSimpleModeZone(zoneName: String) {
        _simpleModeZone.value = zoneName
    }

    fun setApiIp(ip: String) {
        _apiIp.value = ip
    }

    fun setUseMockMode(useMock: Boolean) {
        _useMockMode.value = useMock
    }

    fun saveSettings() {
        NetworkModule.setBaseUrl(_apiIp.value)
        NetworkModule.setUseMockApi(_useMockMode.value)
        NetworkModule.setSimpleModeEnabled(_simpleModeEnabled.value)
        NetworkModule.setSimpleModeZone(_simpleModeZone.value)
        NetworkModule.setSimpleModeStartTemperature(_simpleModeStartTemperature.value)
        WallPanel.setHomeAppEnabled(getApplication(), _simpleModeEnabled.value)
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(application) as T
            }
        }
    }
}