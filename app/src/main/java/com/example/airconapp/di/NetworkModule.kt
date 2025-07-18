package com.example.airconapp.di

import android.content.Context
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

    private const val BASE_URL = "http://192.168.1.6" // Your aircon IP
    private const val USE_MOCK_API = true // Set to true to use mock API, false for real API

    val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    val airconApiService: AirconApiService by lazy {
        if (USE_MOCK_API) {
            MockAirconApiService()
        } else {
            AirconApiServiceImpl(httpClient, BASE_URL)
        }
    }

    private lateinit var applicationContext: Context

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    val schedulerRepository: SchedulerRepository by lazy {
        SchedulerRepository(database.schedulerProfileDao())
    }
}
