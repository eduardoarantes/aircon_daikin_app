package com.example.airconapp.di

import com.example.airconapp.data.AirconApiServiceImpl
import com.example.airconapp.data.AirconApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkModule {

    private const val BASE_URL = "http://192.168.1.6" // TODO: Replace with your actual Daikin API base URL

    val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    val airconApiService: AirconApiService by lazy {
        AirconApiServiceImpl(httpClient, BASE_URL)
    }
}
