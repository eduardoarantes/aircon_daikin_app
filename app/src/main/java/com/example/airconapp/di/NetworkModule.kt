package com.example.airconapp.di

import android.net.ConnectivityManager
import com.example.airconapp.data.remote.isCellularOnly
import android.content.Context
import android.content.SharedPreferences
import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.AirconApiServiceImpl
import com.example.airconapp.data.CloudRelay
import com.example.airconapp.data.Endpoint
import com.example.airconapp.data.mock.MockAirconApiService
import com.example.airconapp.data.db.AppDatabase
import com.example.airconapp.data.remote.EnrolResult
import com.example.airconapp.data.remote.KeyValueStore
import com.example.airconapp.data.remote.RemoteCredential
import com.example.airconapp.data.remote.RemoteCredentialStore
import com.example.airconapp.data.remote.RemoteEnroller
import com.example.airconapp.data.remote.RemoteFallbackApiService
import com.example.airconapp.data.remote.TransportMode
import com.example.airconapp.domain.SimpleModeLogic
import com.example.airconapp.domain.TemperatureUnit
import kotlinx.coroutines.flow.asStateFlow
import com.example.airconapp.domain.ThemeMode
import com.example.airconapp.data.repo.SchedulerRepository
import com.example.airconapp.data.repo.SceneRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.url
import com.example.airconapp.data.discovery.DaikinDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.net.URLEncoder

object NetworkModule {

    private const val PREFS_NAME = "aircon_app_prefs"

    /**
     * The cloud credential lives in its own prefs file so it can be excluded from Android backup
     * (res/xml/backup_rules.xml, data_extraction_rules.xml) without excluding the other settings.
     */
    private const val CREDS_PREFS_NAME = "aircon_remote_creds"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_USE_MOCK_API = "use_mock_api"
    private const val KEY_SIMPLE_MODE_ENABLED = "simple_mode_enabled"
    private const val KEY_SIMPLE_MODE_ZONE = "simple_mode_zone"
    private const val KEY_SIMPLE_MODE_START_TEMP = "simple_mode_start_temp"
    private const val KEY_WALL_PANEL_ENABLED = "wall_panel_enabled"
    private const val KEY_ZONE_USAGE = "zone_usage"
    private const val KEY_TEMP_UNIT = "temperature_unit"
    private val _temperatureUnit = MutableStateFlow(TemperatureUnit.CELSIUS)
    val temperatureUnit: StateFlow<TemperatureUnit> = _temperatureUnit.asStateFlow()

    private const val KEY_THEME_MODE = "theme_mode"

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var credsPreferences: SharedPreferences
    private lateinit var _applicationContext: Context

    // Lazy initialization for HttpClient and AirconApiService
    // These will be re-initialized if settings change and accessed again
    @Volatile private var _httpClient: HttpClient? = null
    @Volatile private var _airconApiService: AirconApiService? = null

    val httpClient: HttpClient
        get() = _httpClient ?: synchronized(this) {
            _httpClient ?: buildHttpClient().also { _httpClient = it }
        }

    val airconApiService: AirconApiService
        get() = _airconApiService ?: synchronized(this) {
            _airconApiService ?: buildAirconApiService().also { _airconApiService = it }
        }

    private fun buildHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            // Fail fast on a dead IP so self-healing (rediscovery) can kick in
            install(HttpTimeout) {
                connectTimeoutMillis = 3_000
                requestTimeoutMillis = 5_000
            }
        }
    }

    private fun buildAirconApiService(): AirconApiService {
        val useMockApi = sharedPreferences.getBoolean(KEY_USE_MOCK_API, true) // Default to mock

        if (useMockApi) return MockAirconApiService()

        // Real mode: a local-first service that transparently falls back to the cloud relay when
        // the adapter is unreachable and remote access is enrolled. Providers read live settings so
        // a healed IP or a refreshed/cleared credential is picked up without rebuilding the service.
        return RemoteFallbackApiService(
            local = { AirconApiServiceImpl(httpClient, Endpoint(getBaseUrl())) },
            cloud = {
                remoteCredentials.get()?.let { AirconApiServiceImpl(httpClient, CloudRelay.endpoint(it.id, it.spw)) }
            },
            heal = { healConnection() },
            reEnrol = { enrolRemoteAccess() == EnrolResult.Success },
            reportMode = { _transportMode.value = it },
            preferCloud = { isOnCellularOnlyNetwork() },
        )
    }

    private fun isOnCellularOnlyNetwork(): Boolean = runCatching {
        val connectivity = _applicationContext.getSystemService(ConnectivityManager::class.java)
        isCellularOnly(connectivity?.getNetworkCapabilities(connectivity.activeNetwork))
    }.getOrDefault(false)

    // --- Remote cloud access ---

    private val _transportMode = MutableStateFlow(TransportMode.LOCAL)

    /** Whether the aircon is currently being reached locally or over the cloud relay. */
    val transportMode: StateFlow<TransportMode> = _transportMode

    private val remoteCredentials: RemoteCredentialStore by lazy {
        RemoteCredentialStore(object : KeyValueStore {
            override fun getString(key: String): String? = credsPreferences.getString(key, null)
            override fun putString(key: String, value: String) {
                credsPreferences.edit().putString(key, value).apply()
            }
            override fun remove(key: String) {
                credsPreferences.edit().remove(key).apply()
            }
        })
    }

    /** True when a cloud credential is stored, i.e. remote access is enrolled. */
    fun hasRemoteCredential(): Boolean = remoteCredentials.has()

    fun getRemoteId(): String? = remoteCredentials.get()?.id

    fun getRemoteSpw(): String? = remoteCredentials.get()?.spw

    /** Stores the cloud credential directly (enrolment uses this via [RemoteEnroller]). */
    fun setRemoteCredential(id: String, spw: String) {
        remoteCredentials.set(RemoteCredential(id, spw))
        invalidateService()
    }

    /** Removes the stored credential, disabling cloud fallback. */
    fun clearRemoteCredential() {
        remoteCredentials.clear()
        _transportMode.value = TransportMode.LOCAL
        invalidateService()
    }

    /**
     * Enrols (or refreshes) remote access: reads the adapter's `id`/`pw` over the LAN, derives the
     * server password `spw` from the relay, and stores the credential. Requires local reachability.
     * Credentials never touch logs — the network reads swallow their exceptions (which can embed the
     * URL and `pw`/`spw`) without logging.
     */
    suspend fun enrolRemoteAccess(): EnrolResult {
        val enroller = RemoteEnroller(
            readBasicInfo = {
                runCatching {
                    httpClient.get { url("${getBaseUrl()}${RemoteEnroller.BASIC_INFO_PATH}") }.body<String>()
                }.getOrNull()
            },
            fetchSpw = { id, apw ->
                runCatching {
                    val query = "id=${URLEncoder.encode(id, "UTF-8")}&apw=${URLEncoder.encode(apw, "UTF-8")}"
                    httpClient.get { url("${CloudRelay.HOST}${RemoteEnroller.GET_SPW_PATH}?$query") }.body<String>()
                }.getOrNull()
            },
            store = { remoteCredentials.set(it) },
        )
        val result = enroller.enrol()
        if (result == EnrolResult.Success) invalidateService()
        return result
    }

    /**
     * Enrols from a hand-entered username (`id`) and password (`pw`), deriving the server password
     * (`spw`) via the relay's `get_spw`. This is a pure cloud call, so it works away from home — no
     * local network access to the adapter is needed. The entered password is never stored (only the
     * derived `spw` is) and never logged.
     */
    suspend fun enrolRemoteWithCredentials(id: String, pw: String): EnrolResult {
        val body = runCatching {
            val query = "id=${URLEncoder.encode(id, "UTF-8")}&apw=${URLEncoder.encode(pw, "UTF-8")}"
            httpClient.get { url("${CloudRelay.HOST}${RemoteEnroller.GET_SPW_PATH}?$query") }.body<String>()
        }.getOrNull() ?: return EnrolResult.Rejected
        val fields = RemoteEnroller.parseKeyValues(body)
        if (fields["ret"] != "OK") return EnrolResult.Rejected
        val spw = fields["spw"]?.takeIf { it.isNotBlank() } ?: return EnrolResult.Rejected
        remoteCredentials.set(RemoteCredential(id, spw))
        invalidateService()
        return EnrolResult.Success
    }

    /** Drops the cached HTTP client / API service so the next access rebuilds them. */
    private fun invalidateService() {
        // Close the old CIO client so its engine threads/sockets are released rather than leaked
        // when a new one is built (enrol, clear, heal, or a settings change).
        runCatching { _httpClient?.close() }
        _httpClient = null
        _airconApiService = null
    }

    fun initialize(context: Context) {
        _applicationContext = context.applicationContext
        sharedPreferences = _applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        credsPreferences = _applicationContext.getSharedPreferences(CREDS_PREFS_NAME, Context.MODE_PRIVATE)
        _temperatureUnit.value = getTemperatureUnit()
    }

    fun getBaseUrl(): String {
        return sharedPreferences.getString(KEY_BASE_URL, "http://192.168.1.6") ?: "http://192.168.1.6"
    }

    fun setBaseUrl(url: String) {
        sharedPreferences.edit().putString(KEY_BASE_URL, url).apply()
        // Invalidate cached service instances so they are rebuilt with new settings
        invalidateService()
    }

    fun getUseMockApi(): Boolean {
        return sharedPreferences.getBoolean(KEY_USE_MOCK_API, true)
    }

    fun setUseMockApi(useMock: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_USE_MOCK_API, useMock).apply()
        // Invalidate cached service instances
        invalidateService()
    }

    fun getSimpleModeEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_SIMPLE_MODE_ENABLED, false)

    fun setSimpleModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SIMPLE_MODE_ENABLED, enabled).apply()
    }

    /**
     * Master switch for the wall-panel features of simple mode: screen lock (pinning or full kiosk),
     * home app, showing over the lock screen, keeping the screen on and silencing. On by default.
     */
    fun getWallPanelEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_WALL_PANEL_ENABLED, true)

    fun setWallPanelEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_WALL_PANEL_ENABLED, enabled).apply()
    }

    /** Zone toggle history for "Most used zones", encoded by [com.example.airconapp.domain.ZoneUsage.encode]. */
    fun getZoneUsage(): String? = sharedPreferences.getString(KEY_ZONE_USAGE, null)

    fun setZoneUsage(encoded: String) {
        sharedPreferences.edit().putString(KEY_ZONE_USAGE, encoded).apply()
    }

    /** Light, dark or follow the device; see [com.example.airconapp.ui.theme.ThemeSetting]. */
    fun getThemeMode(): ThemeMode = ThemeMode.fromName(sharedPreferences.getString(KEY_THEME_MODE, null))

    fun setThemeMode(mode: ThemeMode) {
        sharedPreferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getTemperatureUnit(): TemperatureUnit =
        TemperatureUnit.fromName(sharedPreferences.getString(KEY_TEMP_UNIT, null))

    fun setTemperatureUnit(unit: TemperatureUnit) {
        sharedPreferences.edit().putString(KEY_TEMP_UNIT, unit.name).apply()
        _temperatureUnit.value = unit
    }

    /** True when the app should behave as a wall panel rather than a normal app. */
    fun isWallPanelActive(): Boolean = getSimpleModeEnabled() && getWallPanelEnabled()

    /** Zone is stored by name: the API identifies zones only by position and name. */
    fun getSimpleModeZone(): String? =
        sharedPreferences.getString(KEY_SIMPLE_MODE_ZONE, null)

    fun setSimpleModeZone(zoneName: String?) {
        sharedPreferences.edit().putString(KEY_SIMPLE_MODE_ZONE, zoneName).apply()
    }

    /** Temperature set when simple mode turns the aircon on from off. */
    fun getSimpleModeStartTemperature(): Int =
        sharedPreferences.getInt(KEY_SIMPLE_MODE_START_TEMP, SimpleModeLogic.DEFAULT_START_TEMPERATURE)

    fun setSimpleModeStartTemperature(temperature: Int) {
        sharedPreferences.edit().putInt(KEY_SIMPLE_MODE_START_TEMP, temperature).apply()
    }

    // --- Self-configuration and self-healing via UDP discovery ---

    private val discoveryMutex = Mutex()
    private var lastDiscoveryAt = 0L
    private const val MIN_DISCOVERY_INTERVAL_MS = 30_000L

    /**
     * First run (no IP ever saved): find the unit on the network, save its IP and switch to
     * real mode. Returns true if a unit was found and applied.
     */
    suspend fun autoConfigureIfNeeded(): Boolean {
        if (sharedPreferences.contains(KEY_BASE_URL) || isOnCellularOnlyNetwork()) return false
        return discoverAndApply(force = true)
    }

    /**
     * Called when the aircon can't be reached: rediscover it (at most every 30 s) and switch
     * to its new IP if it moved. Returns true if the IP changed, so the caller can retry.
     */
    suspend fun healConnection(): Boolean {
        if (getUseMockApi()) return false
        return discoverAndApply(force = false)
    }

    private suspend fun discoverAndApply(force: Boolean): Boolean = discoveryMutex.withLock {
        val now = System.currentTimeMillis()
        if (!force && now - lastDiscoveryAt < MIN_DISCOVERY_INTERVAL_MS) return false
        lastDiscoveryAt = now
        // Only one unit is expected on the network; take the first that answers
        val unit = DaikinDiscovery.discover().firstOrNull() ?: return false
        val url = "http://${unit.ip}"
        val changed = url != getBaseUrl() || getUseMockApi()
        if (changed) {
            sharedPreferences.edit()
                .putString(KEY_BASE_URL, url)
                .putBoolean(KEY_USE_MOCK_API, false)
                .apply()
            invalidateService()
        }
        changed
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(_applicationContext)
    }

    val schedulerRepository: SchedulerRepository by lazy {
        SchedulerRepository(database.schedulerProfileDao())
    }

    val sceneRepository: SceneRepository by lazy {
        SceneRepository(_applicationContext)
    }
}
