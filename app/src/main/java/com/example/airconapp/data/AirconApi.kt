package com.example.airconapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import java.net.URLDecoder
import java.net.URLEncoder

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
data class Zone(
    val name: String,
    val isOn: Boolean
)

// ZoneStatus will now hold a list of Zone objects
data class ZoneStatusResponse(
    val zones: List<Zone>
)

// API Service Interface
interface AirconApiService {
    suspend fun getControlInfo(): ControlInfo
    suspend fun setControlInfo(controlInfo: ControlInfo): String
    suspend fun getZoneSetting(): ZoneStatusResponse
    suspend fun setZoneSetting(zones: List<Zone>): String
}

// API Service Implementation
class AirconApiServiceImpl(private val client: HttpClient, private val baseUrl: String) : AirconApiService {

    override suspend fun getControlInfo(): ControlInfo {
        val response = client.get {
            url("$baseUrl/skyfi/aircon/get_control_info")
        }
        val rawResponse: String = response.body()
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

    override suspend fun getZoneSetting(): ZoneStatusResponse {
        val response = client.get {
            url("$baseUrl/skyfi/aircon/get_zone_setting")
        }
        val rawResponse: String = response.body()
        return parseZoneStatusString(rawResponse)
    }

    override suspend fun setZoneSetting(zones: List<Zone>): String {
        val zoneOnOffString = zones.joinToString(";") { if (it.isOn) "1" else "0" }
        val zoneNameString = zones.joinToString(";") { it.name }

        // URL-encode the entire strings before putting them in the query
        var encodedZoneOnOffString = URLEncoder.encode(zoneOnOffString, "UTF-8")
        var encodedZoneNameString = URLEncoder.encode(zoneNameString, "UTF-8")

        // Explicitly replace '+' with '%20' for spaces, if the API expects it
        encodedZoneOnOffString = encodedZoneOnOffString.replace("+", "%20")
        encodedZoneNameString = encodedZoneNameString.replace("+", "%20")

        val queryString = "zone_onoff=$encodedZoneOnOffString&zone_name=$encodedZoneNameString"
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

    // Helper function to parse zone status string into a list of Zone objects
    private fun parseZoneStatusString(input: String): ZoneStatusResponse {
        val parts = input.split(",").associate { part ->
            val (key, value) = part.split("=")
            key to value
        }

        val encodedZoneOnOffString = parts["zone_onoff"] ?: ""
        val encodedZoneNameString = parts["zone_name"] ?: ""

        // URL-decode the strings
        val zoneOnOffString = URLDecoder.decode(encodedZoneOnOffString, "UTF-8")
        var zoneNameString = URLDecoder.decode(encodedZoneNameString, "UTF-8")

        // Replace '+' with spaces, as URLDecoder might not always handle it for all contexts
        zoneNameString = zoneNameString.replace("+", " ")

        val zoneOnOffList = zoneOnOffString.split(";")
        val zoneNameList = zoneNameString.split(";")

        val zones = zoneNameList.indices.map { index ->
            val name = zoneNameList.getOrElse(index) { "Zone ${index + 1}" }
            val isOn = zoneOnOffList.getOrElse(index) { "0" } == "1"
            Zone(name = name, isOn = isOn)
        }
        return ZoneStatusResponse(zones)
    }
}