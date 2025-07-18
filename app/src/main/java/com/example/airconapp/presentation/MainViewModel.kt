package com.example.airconapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.data.ZoneStatusResponse
import com.example.airconapp.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

class MainViewModel(private val airconApiService: AirconApiService = NetworkModule.airconApiService) : ViewModel() {

    private val _controlInfo = MutableStateFlow<ControlInfo?>(null)
    val controlInfo: StateFlow<ControlInfo?> = _controlInfo

    private val _zoneStatus = MutableStateFlow<List<Zone>?>(null)
    val zoneStatus: StateFlow<List<Zone>?> = _zoneStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var refreshJob: Job? = null

    init {
        fetchAirconStatus()
        startPeriodicRefresh()
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
            try {
                _controlInfo.value = airconApiService.getControlInfo()
                _zoneStatus.value = airconApiService.getZoneSetting().zones
            } catch (e: Exception) {
                _error.value = "Failed to fetch aircon status: ${e.message}"
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
}
