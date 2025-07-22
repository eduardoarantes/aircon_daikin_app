package com.example.airconapp.di

import android.content.Context
import android.content.SharedPreferences
import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.AirconApiServiceImpl
import com.example.airconapp.data.mock.MockAirconApiService
import com.example.airconapp.data.db.AppDatabase
import com.example.airconapp.data.repo.SchedulerRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkModule {

    private const val PREFS_NAME = "aircon_app_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_USE_MOCK_API = "use_mock_api"

    private lateinit var sharedPreferences: SharedPreferences
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
        }
    }

    private fun buildAirconApiService(): AirconApiService {
        val useMockApi = sharedPreferences.getBoolean(KEY_USE_MOCK_API, true) // Default to mock
        val baseUrl = sharedPreferences.getString(KEY_BASE_URL, "http://192.168.1.6") ?: "http://192.168.1.6"

        return if (useMockApi) {
            MockAirconApiService()
        } else {
            AirconApiServiceImpl(httpClient, baseUrl)
        }
    }

    fun initialize(context: Context) {
        _applicationContext = context.applicationContext
        sharedPreferences = _applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBaseUrl(): String {
        return sharedPreferences.getString(KEY_BASE_URL, "http://192.168.1.6") ?: "http://192.168.1.6"
    }

    fun setBaseUrl(url: String) {
        sharedPreferences.edit().putString(KEY_BASE_URL, url).apply()
        // Invalidate cached service instances so they are rebuilt with new settings
        _httpClient = null
        _airconApiService = null
    }

    fun getUseMockApi(): Boolean {
        return sharedPreferences.getBoolean(KEY_USE_MOCK_API, true)
    }

    fun setUseMockApi(useMock: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_USE_MOCK_API, useMock).apply()
        // Invalidate cached service instances
        _httpClient = null
        _airconApiService = null
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(_applicationContext)
    }

    val schedulerRepository: SchedulerRepository by lazy {
        SchedulerRepository(database.schedulerProfileDao())
    }
}
