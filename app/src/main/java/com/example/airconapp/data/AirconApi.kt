package com.example.airconapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

// Data Models (DTOs) based on swagger.yaml

@Serializable
data class ControlInfo(
    val ret: String? = null,
    val pow: String,
    val mode: String,
    val stemp: String,
    val f_rate: String,
    val f_dir: String
)

@Serializable
data class ZoneStatus(
    val zone_onoff: String,
    val zone_name: String
)

// API Service Interface
interface AirconApiService {
    suspend fun getControlInfo(): ControlInfo
    suspend fun setControlInfo(controlInfo: ControlInfo): String
    suspend fun getZoneSetting(): ZoneStatus
    suspend fun setZoneSetting(zoneStatus: ZoneStatus): String
}

// API Service Implementation
class AirconApiServiceImpl(private val client: HttpClient, private val baseUrl: String) : AirconApiService {

    override suspend fun getControlInfo(): ControlInfo {
        val response = client.get {
            url("$baseUrl/skyfi/aircon/get_control_info")
        }
        val rawResponse: String = response.body()
        // Parse the raw string response into ControlInfo
        return parseControlInfoString(rawResponse)
    }

    override suspend fun setControlInfo(controlInfo: ControlInfo): String {
        val queryString = "pow=${controlInfo.pow}&mode=${controlInfo.mode}&stemp=${controlInfo.stemp}&f_rate=${controlInfo.f_rate}&f_dir=${controlInfo.f_dir}"
        val response = client.post {
            url("$baseUrl/skyfi/aircon/set_control_info?$queryString")
            contentType(ContentType.Text.Plain)
        }
        return response.body()
    }

    override suspend fun getZoneSetting(): ZoneStatus {
        val response = client.get {
            url("$baseUrl/skyfi/aircon/get_zone_setting")
        }
        val rawResponse: String = response.body()
        // Parse the raw string response into ZoneStatus
        return parseZoneStatusString(rawResponse)
    }

    override suspend fun setZoneSetting(zoneStatus: ZoneStatus): String {
        val queryString = "zone_onoff=${zoneStatus.zone_onoff}&zone_name=${zoneStatus.zone_name}"
        val response = client.post {
            url("$baseUrl/skyfi/aircon/set_zone_setting?$queryString")
            contentType(ContentType.Text.Plain)
        }
        return response.body()
    }

    // Helper function to parse control info string
    private fun parseControlInfoString(input: String): ControlInfo {
        val parts = input.split(",").associate { part ->
            val (key, value) = part.split("=")
            key to value
        }
        return ControlInfo(
            ret = parts["ret"],
            pow = parts["pow"] ?: "",
            mode = parts["mode"] ?: "",
            stemp = parts["stemp"] ?: "",
            f_rate = parts["f_rate"] ?: "",
            f_dir = parts["f_dir"] ?: ""
        )
    }

    // Helper function to parse zone status string
    private fun parseZoneStatusString(input: String): ZoneStatus {
        val parts = input.split(",").associate { part ->
            val (key, value) = part.split("=")
            key to value
        }
        return ZoneStatus(
            zone_onoff = parts["zone_onoff"] ?: "",
            zone_name = parts["zone_name"] ?: ""
        )
    }
}
