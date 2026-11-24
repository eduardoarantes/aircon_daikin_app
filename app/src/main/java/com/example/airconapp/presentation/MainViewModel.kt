package com.example.airconapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.ConnectionException
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.SensorInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.domain.FanSpeed
import com.example.airconapp.domain.SimpleModeLogic
import com.example.airconapp.domain.ZoneUsage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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

    /** null while the first status request is running, then whether the last one reached the unit. */
    private val _isConnected = MutableStateFlow<Boolean?>(null)
    val isConnected: StateFlow<Boolean?> = _isConnected

    /** One-off messages for a snackbar, e.g. a command the unit rejected. */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private val _zoneUsage = MutableStateFlow(ZoneUsage.decode(NetworkModule.getZoneUsage()))

    /** Indices into [zoneStatus] of the (up to) two zones toggled most, weighted towards recent use. */
    val mostUsedZones: StateFlow<List<Int>> = combine(_zoneStatus, _zoneUsage) { zones, usage ->
        zones?.let { ZoneUsage.mostUsed(it, usage, System.currentTimeMillis()) } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Commands update the UI straight away. A status poll that started before the latest command,
     * or while one is still being sent, would show the old state, so its result is dropped.
     */
    private var commandsInFlight = 0
    private var commandGeneration = 0

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
            val generation = commandGeneration
            _isLoading.value = true
            _error.value = null
            _simpleModeEnabled.value = NetworkModule.getSimpleModeEnabled()
            _wallPanelEnabled.value = NetworkModule.getWallPanelEnabled()
            _simpleModeZone.value = NetworkModule.getSimpleModeZone()
            _simpleModeStartTemperature.value = NetworkModule.getSimpleModeStartTemperature()
            try {
                val info = airconApiService.getControlInfo()
                val zones = airconApiService.getZoneSetting().zones
                // Ambient temperature is optional: a failure here must not hide the controls
                _sensorInfo.value = runCatching { airconApiService.getSensorInfo() }.getOrNull()
                if (commandsInFlight == 0 && generation == commandGeneration) {
                    _controlInfo.value = info
                    _zoneStatus.value = zones
                }
                _isConnected.value = true
                // Connection is back (e.g. Wi-Fi came up after boot): drop any stale error
                _showConnectionErrorDialog.value = false
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                // The aircon may have moved to a new IP: rediscover it and retry before
                // bothering the user
                if (NetworkModule.healConnection()) {
                    fetchAirconStatus()
                    return@launch
                }
                _isConnected.value = false
                if (e is ConnectionException) {
                    _showConnectionErrorDialog.value = true
                } else {
                    _error.value = "Failed to fetch aircon status: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Shows [update] at once, sends it, and on failure puts back what was there (unless
     * something newer replaced it) and posts [failure] as a message.
     */
    private fun <T> sendCommand(
        state: MutableStateFlow<T?>,
        update: (T) -> T,
        failure: String,
        send: suspend (T) -> Unit
    ) {
        val previous = state.value ?: return
        val next = update(previous)
        state.value = next
        commandsInFlight++
        commandGeneration++
        viewModelScope.launch {
            try {
                send(next)
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                if (state.value == next) state.value = previous
                _error.value = "$failure: ${e.message}"
                _messages.trySend(failure)
            } finally {
                commandsInFlight--
            }
        }
    }

    private fun sendControl(failure: String, update: (ControlInfo) -> ControlInfo) =
        sendCommand(_controlInfo, update, failure) { airconApiService.setControlInfo(it) }

    private fun sendZones(failure: String, update: (List<Zone>) -> List<Zone>) =
        sendCommand(_zoneStatus, update, failure) { airconApiService.setZoneSetting(it) }

    fun setPower(isOn: Boolean) =
        sendControl("Couldn't turn the aircon ${if (isOn) "on" else "off"}") { it.copy(pow = if (isOn) "1" else "0") }

    fun setTemperature(temperature: Int) =
        sendControl("Couldn't change the temperature") { it.copy(stemp = temperature.toString()) }

    fun setMode(mode: String) = sendControl("Couldn't change the mode") { it.copy(mode = mode) }

    fun setFanSpeed(speed: FanSpeed) = sendControl("Couldn't change the fan speed") { FanSpeed.apply(it, speed) }

    /** A zone toggled by the user: also counts towards "Most used zones". */
    fun toggleZone(zoneIndex: Int) {
        val zone = _zoneStatus.value?.getOrNull(zoneIndex) ?: return
        _zoneUsage.value = ZoneUsage.record(_zoneUsage.value, zone.name, System.currentTimeMillis())
        NetworkModule.setZoneUsage(ZoneUsage.encode(_zoneUsage.value))
        val isOn = !zone.isOn
        sendZones("Couldn't turn ${zone.name} ${if (isOn) "on" else "off"}") { zones ->
            zones.toMutableList().apply { this[zoneIndex] = this[zoneIndex].copy(isOn = isOn) }
        }
    }

    /** Opens or closes every zone in one request. */
    fun setAllZones(isOn: Boolean) =
        sendZones("Couldn't turn all zones ${if (isOn) "on" else "off"}") { zones -> zones.map { it.copy(isOn = isOn) } }

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

        // Keep a status poll that is already running from undoing this press
        commandsInFlight++
        commandGeneration++
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
            } finally {
                commandsInFlight--
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
