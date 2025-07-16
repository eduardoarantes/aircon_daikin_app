package com.example.airconapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.ZoneStatus
import com.example.airconapp.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val airconApiService: AirconApiService = NetworkModule.airconApiService) : ViewModel() {

    private val _controlInfo = MutableStateFlow<ControlInfo?>(null)
    val controlInfo: StateFlow<ControlInfo?> = _controlInfo

    private val _zoneStatus = MutableStateFlow<ZoneStatus?>(null)
    val zoneStatus: StateFlow<ZoneStatus?> = _zoneStatus

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchAirconStatus()
    }

    fun fetchAirconStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _controlInfo.value = airconApiService.getControlInfo()
                _zoneStatus.value = airconApiService.getZoneSetting()
            } catch (e: Exception) {
                _error.value = "Failed to fetch aircon status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setPower(power: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                controlInfo.value?.let { currentInfo ->
                    val newControlInfo = currentInfo.copy(pow = power)
                    airconApiService.setControlInfo(newControlInfo)
                    _controlInfo.value = newControlInfo // Optimistically update UI
                }
            } catch (e: Exception) {
                _error.value = "Failed to set power: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setTemperature(temperature: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                controlInfo.value?.let { currentInfo ->
                    val newControlInfo = currentInfo.copy(stemp = temperature)
                    airconApiService.setControlInfo(newControlInfo)
                    _controlInfo.value = newControlInfo
                }
            } catch (e: Exception) {
                _error.value = "Failed to set temperature: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setMode(mode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                controlInfo.value?.let { currentInfo ->
                    val newControlInfo = currentInfo.copy(mode = mode)
                    airconApiService.setControlInfo(newControlInfo)
                    _controlInfo.value = newControlInfo
                }
            } catch (e: Exception) {
                _error.value = "Failed to set mode: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFanRate(fanRate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                controlInfo.value?.let { currentInfo ->
                    val newControlInfo = currentInfo.copy(f_rate = fanRate)
                    airconApiService.setControlInfo(newControlInfo)
                    _controlInfo.value = newControlInfo
                }
            } catch (e: Exception) {
                _error.value = "Failed to set fan rate: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // TODO: Add functions for setting fan direction, and zone status
}