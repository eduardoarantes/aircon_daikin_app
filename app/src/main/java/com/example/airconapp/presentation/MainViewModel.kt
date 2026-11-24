package com.example.airconapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.ConnectionException
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.SensorInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.domain.SimpleModeLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

class MainViewModel : ViewModel() { // Removed airconApiService from constructor

    private val _controlInfo = MutableStateFlow<ControlInfo?>(null)
    val controlInfo: StateFlow<ControlInfo?> = _controlInfo

    private val _zoneStatus = MutableStateFlow<List<Zone>?>(null)
    val zoneStatus: StateFlow<List<Zone>?> = _zoneStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _showConnectionErrorDialog = MutableStateFlow(false)
    val showConnectionErrorDialog: StateFlow<Boolean> = _showConnectionErrorDialog

    private val _sensorInfo = MutableStateFlow<SensorInfo?>(null)
    val sensorInfo: StateFlow<SensorInfo?> = _sensorInfo

    private val _simpleModeEnabled = MutableStateFlow(NetworkModule.getSimpleModeEnabled())
    val simpleModeEnabled: StateFlow<Boolean> = _simpleModeEnabled

    private val _wallPanelEnabled = MutableStateFlow(NetworkModule.getWallPanelEnabled())
    val wallPanelEnabled: StateFlow<Boolean> = _wallPanelEnabled

    private val _simpleModeZone = MutableStateFlow(NetworkModule.getSimpleModeZone())
    val simpleModeZone: StateFlow<String?> = _simpleModeZone

    private val _simpleModeStartTemperature = MutableStateFlow(NetworkModule.getSimpleModeStartTemperature())
    val simpleModeStartTemperature: StateFlow<Int> = _simpleModeStartTemperature

    private var refreshJob: Job? = null

    // Get the current API service from NetworkModule
    private val airconApiService: AirconApiService
        get() = NetworkModule.airconApiService

    init {
        viewModelScope.launch {
            // Fresh install: find the aircon on the network before the first fetch
            NetworkModule.autoConfigureIfNeeded()
            fetchAirconStatus()
            startPeriodicRefresh()
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(5000) // Refresh every 5 seconds
                if (isActive) {
                    fetchAirconStatus()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    fun fetchAirconStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _simpleModeEnabled.value = NetworkModule.getSimpleModeEnabled()
            _wallPanelEnabled.value = NetworkModule.getWallPanelEnabled()
            _simpleModeZone.value = NetworkModule.getSimpleModeZone()
            _simpleModeStartTemperature.value = NetworkModule.getSimpleModeStartTemperature()
            try {
                _controlInfo.value = airconApiService.getControlInfo()
                _zoneStatus.value = airconApiService.getZoneSetting().zones
                // Ambient temperature is optional: a failure here must not hide the controls
                _sensorInfo.value = runCatching { airconApiService.getSensorInfo() }.getOrNull()
                // Connection is back (e.g. Wi-Fi came up after boot): drop any stale error
                _showConnectionErrorDialog.value = false
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                // The aircon may have moved to a new IP: rediscover it and retry before
                // bothering the user
                if (NetworkModule.healConnection()) {
                    fetchAirconStatus()
                } else if (e is ConnectionException) {
                    _showConnectionErrorDialog.value = true
                } else {
                    _error.value = "Failed to fetch aircon status: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setPower(power: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                controlInfo.value?.let { currentInfo ->
                    val newControlInfo = currentInfo.copy(pow = power)
                    _controlInfo.value = newControlInfo // Optimistic update
                    airconApiService.setControlInfo(newControlInfo)
                }
            } catch (e: Exception) {
                _error.value = "Failed to set power: ${e.message}"
                // Revert optimistic update and re-fetch if API call fails
                fetchAirconStatus()
            }
        }
    }

    fun setTemperature(temperature: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                controlInfo.value?.let { currentInfo ->
                    val newControlInfo = currentInfo.copy(stemp = temperature)
                    _controlInfo.value = newControlInfo // Optimistic update
                    airconApiService.setControlInfo(newControlInfo)
                }
            } catch (e: Exception) {
                _error.value = "Failed to set temperature: ${e.message}"
                fetchAirconStatus()
            }
        }
    }

    fun setMode(mode: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                controlInfo.value?.let { currentInfo ->
                    val newControlInfo = currentInfo.copy(mode = mode)
                    _controlInfo.value = newControlInfo // Optimistic update
                    airconApiService.setControlInfo(newControlInfo)
                }
            } catch (e: Exception) {
                _error.value = "Failed to set mode: ${e.message}"
                fetchAirconStatus()
            }
        }
    }

    fun setFanRate(fanRate: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                controlInfo.value?.let { currentInfo ->
                    val newControlInfo = currentInfo.copy(f_rate = fanRate)
                    _controlInfo.value = newControlInfo // Optimistic update
                    airconApiService.setControlInfo(newControlInfo)
                }
            } catch (e: Exception) {
                _error.value = "Failed to set fan rate: ${e.message}"
                fetchAirconStatus()
            }
        }
    }

    fun setZonePower(zoneIndex: Int, isOn: Boolean) {
        viewModelScope.launch {
            _error.value = null
            try {
                _zoneStatus.value?.let { currentZones ->
                    val updatedZones = currentZones.toMutableList().apply {
                        this[zoneIndex] = this[zoneIndex].copy(isOn = isOn)
                    }
                    _zoneStatus.value = updatedZones // Optimistic update
                    airconApiService.setZoneSetting(updatedZones)
                }
            } catch (e: Exception) {
                _error.value = "Failed to set zone power: ${e.message}"
                fetchAirconStatus()
            }
        }
    }

    /** Simple-mode button press; see [SimpleModeLogic] for the rules. */
    fun toggleSimpleMode() {
        val zoneName = _simpleModeZone.value ?: return
        val currentInfo = _controlInfo.value ?: return
        val currentZones = _zoneStatus.value ?: return
        val action = SimpleModeLogic.toggle(
            isPoweredOn = currentInfo.pow == "1",
            zones = currentZones,
            zoneName = zoneName,
            startTemperature = _simpleModeStartTemperature.value
        ) ?: return

        viewModelScope.launch {
            _error.value = null
            try {
                // Zones first, so the aircon never starts blowing into the wrong rooms
                action.zones?.let { zones ->
                    _zoneStatus.value = zones
                    airconApiService.setZoneSetting(zones)
                }
                action.power?.let { power ->
                    val newInfo = currentInfo.copy(
                        pow = power,
                        stemp = action.temperature ?: currentInfo.stemp,
                        f_airside = action.airside ?: currentInfo.f_airside
                    )
                    _controlInfo.value = newInfo
                    airconApiService.setControlInfo(newInfo)
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle aircon: ${e.message}"
                fetchAirconStatus()
            }
        }
    }

    fun hideConnectionErrorDialog() {
        _showConnectionErrorDialog.value = false
    }

    fun switchToMockMode() {
        NetworkModule.setUseMockApi(true)
        _showConnectionErrorDialog.value = false
        fetchAirconStatus()
    }
}
