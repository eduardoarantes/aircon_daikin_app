package com.example.airconapp.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.airconapp.di.NetworkModule
import com.example.airconapp.data.discovery.DaikinDiscovery
import com.example.airconapp.data.remote.EnrolResult
import com.example.airconapp.domain.ThemeMode
import com.example.airconapp.ui.theme.ThemeSetting
import com.example.airconapp.domain.TemperatureUnit
import com.example.airconapp.domain.SimpleModeLogic
import com.example.airconapp.wallpanel.WallPanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _temperatureUnit = MutableStateFlow(NetworkModule.getTemperatureUnit())
    val temperatureUnit = _temperatureUnit.asStateFlow()

    private val _themeMode = MutableStateFlow(NetworkModule.getThemeMode())
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        _temperatureUnit.value = unit
    }

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

    private val _wallPanelEnabled = MutableStateFlow(NetworkModule.getWallPanelEnabled())
    val wallPanelEnabled: StateFlow<Boolean> = _wallPanelEnabled.asStateFlow()

    fun setWallPanelEnabled(enabled: Boolean) {
        _wallPanelEnabled.value = enabled
    }

    fun setSimpleModeZone(zoneName: String) {
        _simpleModeZone.value = zoneName
    }

    /** Null when idle; otherwise the progress/result of the last "Find" (shown under the IP field). */
    private val _discoveryStatus = MutableStateFlow<String?>(null)
    val discoveryStatus: StateFlow<String?> = _discoveryStatus.asStateFlow()

    /** Looks for the aircon on the network and fills in its IP (saved with Save Settings). */
    fun findAircon() {
        viewModelScope.launch {
            _discoveryStatus.value = "Searching…"
            val unit = DaikinDiscovery.discover().firstOrNull()
            if (unit == null) {
                _discoveryStatus.value = "No aircon found on this network"
            } else {
                _apiIp.value = "http://${unit.ip}"
                _useMockMode.value = false
                _discoveryStatus.value = "Found ${unit.name ?: "aircon"} at ${unit.ip}"
            }
        }
    }

    // --- Remote (cloud) access ---

    private val _remoteEnrolled = MutableStateFlow(NetworkModule.hasRemoteCredential())
    /** True when a cloud credential is stored, so control works away from the home network. */
    val remoteEnrolled: StateFlow<Boolean> = _remoteEnrolled.asStateFlow()

    private val _remoteStatus = MutableStateFlow<String?>(null)
    /** Progress/result of the last enrol attempt, shown under the remote-access controls. */
    val remoteStatus: StateFlow<String?> = _remoteStatus.asStateFlow()

    private val _remoteBusy = MutableStateFlow(false)
    val remoteBusy: StateFlow<Boolean> = _remoteBusy.asStateFlow()

    /**
     * Enrols remote access: derives the cloud credential from the adapter over the LAN. Must be run
     * while connected to the home network.
     */
    fun enableRemoteAccess() {
        if (_remoteBusy.value) return
        viewModelScope.launch {
            _remoteBusy.value = true
            _remoteStatus.value = "Setting up remote access…"
            val result = NetworkModule.enrolRemoteAccess()
            _remoteStatus.value = when (result) {
                EnrolResult.Success -> "Remote access is on. You can now control the aircon away from home."
                EnrolResult.NotOnLocalNetwork ->
                    "Couldn't reach the aircon. Connect to your home Wi-Fi and try again."
                EnrolResult.Rejected -> "The aircon didn't accept remote access. Please try again."
            }
            _remoteEnrolled.value = NetworkModule.hasRemoteCredential()
            _remoteBusy.value = false
        }
    }

    /** Clears the stored credential, disabling cloud fallback. */
    fun clearRemoteAccess() {
        NetworkModule.clearRemoteCredential()
        _remoteEnrolled.value = false
        _remoteStatus.value = "Remote access is off."
    }

    // Manual credential entry (advanced): set up remote access with a username + password,
    // without being on the home network. The server password (spw) is derived via the cloud.
    private val _manualUsername = MutableStateFlow("")
    val manualUsername: StateFlow<String> = _manualUsername.asStateFlow()

    private val _manualPassword = MutableStateFlow("")
    val manualPassword: StateFlow<String> = _manualPassword.asStateFlow()

    /** Whether the manual username/password fields are shown. */
    private val _manualEntryVisible = MutableStateFlow(false)
    val manualEntryVisible: StateFlow<Boolean> = _manualEntryVisible.asStateFlow()

    fun toggleManualEntry() { _manualEntryVisible.value = !_manualEntryVisible.value }

    fun setManualUsername(value: String) { _manualUsername.value = value }

    fun setManualPassword(value: String) { _manualPassword.value = value }

    /**
     * Enrols from a hand-entered username + password, deriving the server password (`spw`) via the
     * cloud (`get_spw`). Unlike automatic enrolment this needs no local network, so it works away
     * from home — only the login is required.
     */
    fun saveManualRemoteCredential() {
        if (_remoteBusy.value) return
        val username = _manualUsername.value.trim()
        val password = _manualPassword.value.trim()
        if (username.isBlank() || password.isBlank()) {
            _remoteStatus.value = "Enter both your username and password."
            return
        }
        viewModelScope.launch {
            _remoteBusy.value = true
            _remoteStatus.value = "Checking your details…"
            val result = NetworkModule.enrolRemoteWithCredentials(username, password)
            _remoteStatus.value = when (result) {
                EnrolResult.Success -> {
                    // Clear the fields and collapse the form; the status line is the confirmation.
                    _manualUsername.value = ""
                    _manualPassword.value = ""
                    _manualEntryVisible.value = false
                    "Remote access is on — you can now control the aircon from anywhere."
                }
                else -> "Couldn't verify those details. Check your username and password."
            }
            _remoteEnrolled.value = NetworkModule.hasRemoteCredential()
            _remoteBusy.value = false
        }
    }

    fun setApiIp(ip: String) {
        _apiIp.value = ip
    }

    fun setUseMockMode(useMock: Boolean) {
        _useMockMode.value = useMock
    }

    /** Compared on each settings-field update; reverting an edit clears the pending changes. */
    val hasUnsavedChanges: Boolean
        get() = _temperatureUnit.value != NetworkModule.getTemperatureUnit() ||
            _themeMode.value != NetworkModule.getThemeMode() ||
            _apiIp.value != NetworkModule.getBaseUrl() ||
            _useMockMode.value != NetworkModule.getUseMockApi() ||
            _simpleModeEnabled.value != NetworkModule.getSimpleModeEnabled() ||
            _wallPanelEnabled.value != NetworkModule.getWallPanelEnabled() ||
            _simpleModeZone.value != NetworkModule.getSimpleModeZone() ||
            _simpleModeStartTemperature.value != NetworkModule.getSimpleModeStartTemperature()

    val canSaveSettings: Boolean
        get() = !_simpleModeEnabled.value || _simpleModeZone.value != null

    fun discardChanges() {
        _temperatureUnit.value = NetworkModule.getTemperatureUnit()
        _themeMode.value = NetworkModule.getThemeMode()
        _apiIp.value = NetworkModule.getBaseUrl()
        _useMockMode.value = NetworkModule.getUseMockApi()
        _simpleModeEnabled.value = NetworkModule.getSimpleModeEnabled()
        _wallPanelEnabled.value = NetworkModule.getWallPanelEnabled()
        _simpleModeZone.value = NetworkModule.getSimpleModeZone()
        _simpleModeStartTemperature.value = NetworkModule.getSimpleModeStartTemperature()
    }

    fun saveSettings() {
        if (!canSaveSettings) return
        NetworkModule.setTemperatureUnit(_temperatureUnit.value)
        ThemeSetting.set(getApplication(), _themeMode.value)
        NetworkModule.setBaseUrl(_apiIp.value)
        NetworkModule.setUseMockApi(_useMockMode.value)
        NetworkModule.setSimpleModeEnabled(_simpleModeEnabled.value)
        NetworkModule.setWallPanelEnabled(_wallPanelEnabled.value)
        NetworkModule.setSimpleModeZone(_simpleModeZone.value)
        NetworkModule.setSimpleModeStartTemperature(_simpleModeStartTemperature.value)
        WallPanel.setHomeAppEnabled(getApplication(), NetworkModule.isWallPanelActive())
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