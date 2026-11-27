package com.example.airconapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import java.io.IOException
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
    val f_dir: String,
    // Daikin SkyFi "airside" fan flag ("1" = on). Not in swagger.yaml; null when unknown.
    val f_airside: String? = null,
    // AirBase automatic fan flag ("1" = auto, f_rate then keeps the last manual speed); null when unknown.
    val f_auto: String? = null
)

@Serializable
data class Zone(
    val name: String,
    val isOn: Boolean
)

// Sensor readings from /skyfi/aircon/get_sensor_info. "-" means not available.
data class SensorInfo(
    val indoorTemp: String?,
    val outdoorTemp: String?
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
    suspend fun getSensorInfo(): SensorInfo
}

// API Service Implementation
//
// One implementation serves both transports: [endpoint] is either the local adapter
// (`http://<ip>`, no auth) or the cloud relay (`CloudRelay.endpoint(id, spw)`). The cloud path
// adds the auth/target params to each request, enforces the relay's `ret` conventions, and never
// lets an exception carry the request URL (which holds the credential) to the UI or logs.
class AirconApiServiceImpl(private val client: HttpClient, private val endpoint: Endpoint) : AirconApiService {

    /** Backwards-compatible local constructor (no auth params). */
    constructor(client: HttpClient, baseUrl: String) : this(client, Endpoint(baseUrl))

    private val isCloud: Boolean get() = endpoint.isCloud

    override suspend fun getControlInfo(): ControlInfo =
        parseControlInfoString(request("get_control_info", query = null, isWrite = false))

    override suspend fun setControlInfo(controlInfo: ControlInfo): String {
        val queryString = "pow=${controlInfo.pow}&mode=${controlInfo.mode}&stemp=${controlInfo.stemp}&f_rate=${controlInfo.f_rate}&f_dir=${controlInfo.f_dir}" +
            (controlInfo.f_airside?.let { "&f_airside=$it" } ?: "") +
            (controlInfo.f_auto?.let { "&f_auto=$it" } ?: "")
        return request("set_control_info", query = queryString, isWrite = true)
    }

    override suspend fun getZoneSetting(): ZoneStatusResponse =
        parseZoneStatusString(request("get_zone_setting", query = null, isWrite = false))

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
        return request("set_zone_setting", query = queryString, isWrite = true)
    }

    override suspend fun getSensorInfo(): SensorInfo {
        val parts = parseKeyValues(request("get_sensor_info", query = null, isWrite = false))
        return SensorInfo(
            indoorTemp = parts["htemp"]?.takeIf { it != "-" },
            outdoorTemp = parts["otemp"]?.takeIf { it != "-" }
        )
    }

    /**
     * Issues one aircon operation and returns its raw key=value body.
     *
     * Local path: a connectivity failure (any [IOException], which covers no-route and timeouts)
     * becomes [ConnectionException] so the fallback boundary can react; other errors propagate.
     *
     * Cloud path: writes go as GET while [CloudRelay.WRITES_AS_GET] holds; `ret` is inspected
     * ([classifyCloudResponse]) to surface a stale credential or a rejected write; and ANY transport
     * error is replaced with a fixed-message [ConnectionException] carrying no cause, so the request
     * URL (and the `spw`/`id` in it) can never reach a log or the UI.
     */
    private suspend fun request(operation: String, query: String?, isWrite: Boolean): String {
        val url = buildAirconUrl(endpoint, operation, query)
        val useGet = !isWrite || (isCloud && CloudRelay.WRITES_AS_GET)
        val raw: String = try {
            if (useGet) {
                client.get { url(url) }.body()
            } else {
                client.post {
                    url(url)
                    contentType(ContentType.Text.Plain)
                }.body()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (isCloud) throw ConnectionException("Could not reach the aircon remotely")
            if (e is IOException) {
                // A response-phase timeout means we connected and sent, but the reply never came —
                // a local write may already have landed, so it must not be re-sent over the cloud.
                val responsePhase = e is HttpRequestTimeoutException || e is java.net.SocketTimeoutException
                throw ConnectionException("Could not connect to aircon", connectPhase = !responsePhase)
            }
            throw e
        }
        if (isCloud) {
            when (classifyCloudResponse(raw, isWrite)) {
                CloudOutcome.OK -> {}
                CloudOutcome.STALE_CREDENTIAL ->
                    throw RemoteCredentialException("Remote access needs to be re-enrolled from your home network")
                CloudOutcome.REJECTED -> throw RemoteException("The aircon rejected the remote command")
            }
        }
        return raw
    }

    private fun parseKeyValues(input: String): Map<String, String> =
        input.split(",").mapNotNull { part ->
            val kv = part.split("=", limit = 2)
            if (kv.size == 2) kv[0] to kv[1] else null
        }.toMap()

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
            f_dir = parts["f_dir"] ?: "",
            f_airside = parts["f_airside"],
            f_auto = parts["f_auto"]
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