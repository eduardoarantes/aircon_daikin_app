package com.example.airconapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.RemoteCredentialException
import com.example.airconapp.data.SensorInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.domain.FanSpeed
import com.example.airconapp.domain.Scene
import com.example.airconapp.data.SceneRunner
import com.example.airconapp.data.remote.TransportMode
import com.example.airconapp.domain.SimpleModeLogic
import com.example.airconapp.domain.ZoneUsage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

/** Failed status requests in a row before the app says it can't connect (a few seconds of searching). */
private const val FAILED_ATTEMPTS_BEFORE_GIVING_UP = 3

/** How long after the user's last change it is sent to the unit. */
private const val SEND_DELAY_MS = 2_000L

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

    /** Whether the aircon is currently reached locally or over the cloud relay (for the UI). */
    val transportMode: StateFlow<TransportMode> = NetworkModule.transportMode

    private val _simpleModeEnabled = MutableStateFlow(NetworkModule.getSimpleModeEnabled())
    val simpleModeEnabled: StateFlow<Boolean> = _simpleModeEnabled

    private val _wallPanelEnabled = MutableStateFlow(NetworkModule.getWallPanelEnabled())
    val wallPanelEnabled: StateFlow<Boolean> = _wallPanelEnabled

    private val _simpleModeZone = MutableStateFlow(NetworkModule.getSimpleModeZone())
    val simpleModeZone: StateFlow<String?> = _simpleModeZone

    private val _simpleModeStartTemperature = MutableStateFlow(NetworkModule.getSimpleModeStartTemperature())
    val simpleModeStartTemperature: StateFlow<Int> = _simpleModeStartTemperature

    /**
     * true when the last status request reached the unit; null while searching for it (first
     * load, or up to [FAILED_ATTEMPTS_BEFORE_GIVING_UP] failures in a row); false after that.
     */
    private val _isConnected = MutableStateFlow<Boolean?>(null)
    private var failedAttempts = 0
    val isConnected: StateFlow<Boolean?> = _isConnected

    /** So the "re-enrol from home" message is shown once per outage, not on every 5 s poll. */
    private var remoteReEnrolReported = false

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

    // Declared before init, which starts the first status poll that checks them
    private val sendScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controlSend = DelayedSend(_controlInfo) { airconApiService.setControlInfo(it) }
    private val zoneSend = DelayedSend(_zoneStatus) { airconApiService.setZoneSetting(it) }

    private var refreshJob: Job? = null

    // Get the current API service from NetworkModule
    private val airconApiService: AirconApiService
        get() = NetworkModule.airconApiService

    /** Fresh install: find the aircon on the network before the first fetch. */
    private val setup = viewModelScope.launch { NetworkModule.autoConfigureIfNeeded() }

    /** True while the app is on screen; polling (and counting failures) only happens then. */
    private var visible = false

    /**
     * The app came back on screen (launch, unlock, return from another app). Failures from
     * before don't count: the network is often down while the phone is locked, so start the
     * search afresh instead of greeting the user with a connection error.
     */
    fun onAppVisible() {
        visible = true
        failedAttempts = 0
        remoteReEnrolReported = false
        _showConnectionErrorDialog.value = false
        if (_isConnected.value == false) _isConnected.value = null
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            setup.join()
            while (isActive) {
                fetchAirconStatus()
                delay(5000) // Refresh every 5 seconds
            }
        }
    }

    /** The app left the screen (phone locked, another app opened): stop polling. */
    fun onAppHidden() {
        visible = false
        refreshJob?.cancel()
        _showConnectionErrorDialog.value = false
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        // The screen is going away: send what the user just changed instead of dropping it
        controlSend.flush()
        zoneSend.flush()
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
                if (commandsInFlight == 0 && generation == commandGeneration && !controlSend.busy && !zoneSend.busy) {
                    _controlInfo.value = info
                    _zoneStatus.value = zones
                }
                failedAttempts = 0
                remoteReEnrolReported = false
                _isConnected.value = true
                // Connection is back (e.g. Wi-Fi came up after boot): drop any stale error
                _showConnectionErrorDialog.value = false
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                // A request that failed because the phone was locked says nothing about the aircon
                if (!visible) return@launch
                // Away with a stale cloud credential: tell the user to re-enrol from home, once.
                if (e is RemoteCredentialException) {
                    if (!remoteReEnrolReported) {
                        remoteReEnrolReported = true
                        _messages.trySend(e.message ?: "Remote access needs re-enrolling from your home network")
                    }
                }
                // A moved adapter is already rediscovered inside the API service's fallback boundary
                // (RemoteFallbackApiService heals before surfacing an error), so don't heal again here.
                // Keep searching (the regular poll retries) before telling the user; then say so
                // once per outage, not on every poll
                failedAttempts++
                if (failedAttempts < FAILED_ATTEMPTS_BEFORE_GIVING_UP) {
                    _isConnected.value = null
                } else {
                    _isConnected.value = false
                    if (failedAttempts == FAILED_ATTEMPTS_BEFORE_GIVING_UP) _showConnectionErrorDialog.value = true
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sends user changes to the unit [SEND_DELAY_MS] after the last one, so a burst of taps
     * becomes a single request carrying the final state (and changes that cancel out send
     * nothing). The screen shows each change at once. If the send fails, the screen goes back
     * to what the unit last had (unless something newer replaced it) and posts a message.
     */
    private inner class DelayedSend<T : Any>(
        private val state: MutableStateFlow<T?>,
        private val send: suspend (T) -> Unit
    ) {
        /** What the unit is known to have: where a failed send rolls back to. */
        private var confirmed: T? = null
        private var timer: Job? = null
        private var sending = 0
        private var failure = ""

        /** A change is waiting or being sent; status polls must not overwrite it. */
        val busy: Boolean get() = timer != null || sending > 0

        fun change(failureMessage: String, update: (T) -> T) {
            val current = state.value ?: return
            if (!busy) confirmed = current
            state.value = update(current)
            failure = failureMessage
            commandGeneration++
            timer?.cancel()
            timer = viewModelScope.launch {
                delay(SEND_DELAY_MS)
                timer = null
                flush()
            }
        }

        /** Drops a change that hasn't been sent yet. */
        fun cancel() {
            timer?.cancel()
            timer = null
        }

        /** Sends the latest state now (also used when the screen closes). */
        fun flush() {
            timer?.cancel()
            timer = null
            val value = state.value ?: return
            if (value == confirmed && sending == 0) return
            val message = failure
            sending++
            // Not viewModelScope: a send must finish even if the screen closes meanwhile
            sendScope.launch {
                try {
                    send(value)
                    confirmed = value
                } catch (e: Exception) {
                    if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                // A request that failed because the phone was locked says nothing about the aircon
                if (!visible) return@launch
                    if (state.value == value) confirmed?.let { state.value = it }
                    _error.value = "$message: ${e.message}"
                    _messages.trySend(message)
                } finally {
                    sending--
                    commandGeneration++
                }
            }
        }
    }

    private fun sendControl(failure: String, update: (ControlInfo) -> ControlInfo) = controlSend.change(failure, update)

    private fun sendZones(failure: String, update: (List<Zone>) -> List<Zone>) = zoneSend.change(failure, update)

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

    /** Runs a scene from the Scenes tab; the launcher shortcuts use [SceneRunner] directly. */
    fun applyScene(scene: Scene) {
        // Pending taps would be sent after the scene and undo part of it
        controlSend.cancel()
        zoneSend.cancel()
        commandsInFlight++
        commandGeneration++
        viewModelScope.launch {
            _error.value = null
            try {
                val (control, zones) = SceneRunner.apply(scene)
                _controlInfo.value = control
                _zoneStatus.value = zones
                _messages.trySend("${scene.name} is on")
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                _error.value = "Couldn't start ${scene.name}: ${e.message}"
                _messages.trySend("Couldn't start ${scene.name}")
            } finally {
                commandsInFlight--
                commandGeneration++
            }
        }
    }

    fun hideConnectionErrorDialog() {
        _showConnectionErrorDialog.value = false
    }

}
