package com.example.airconapp.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Read-only adapter details, as the official Daikin app shows under "Adapter settings".
 * Every field is null when the adapter did not report it.
 *
 * Sources (all LAN, under `/skyfi/common/`):
 * - `basic_info`: `ver`, `mac`, `sync_time` (it also carries the adapter password `pw`, which is
 *   never kept here)
 * - `get_datetime`: `cur` (the adapter's local time), `zone`, `dst`
 * - `get_remote_method`: `method` (`polling` = Out-of-Home on)
 */
data class AdapterInfo(
    val firmware: String?,
    val macAddress: String?,
    /** Daikin's "Out-of-Home (Not at home)": whether the adapter keeps polling Daikin's cloud. */
    val outOfHome: Boolean?,
    val dateTime: LocalDateTime?,
    /** Zone name when known, otherwise the adapter's numeric zone code. */
    val timeZone: String?,
    val automaticDst: Boolean?,
    /** Daikin's "Disable Time Sync with internet" switch. */
    val timeSyncDisabled: Boolean?,
)

/** Result of the connection test. [remote] is null when remote access isn't set up. */
data class ConnectionTestResult(val local: Boolean, val remote: Boolean?)

object AdapterInfoParser {
    const val BASIC_INFO_PATH = "/skyfi/common/basic_info"
    const val DATETIME_PATH = "/skyfi/common/get_datetime"
    const val REMOTE_METHOD_PATH = "/skyfi/common/get_remote_method"

    /**
     * Zone codes seen on real adapters. The full Daikin table isn't known, so anything else is
     * shown as its code rather than guessed.
     */
    private val ZONE_NAMES = mapOf("345" to "Sydney")

    private val ADAPTER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d H:m:s")

    /** Builds [AdapterInfo] from the three raw bodies; any of them may be null (not read). */
    fun parse(basicInfo: String?, dateTime: String?, remoteMethod: String?): AdapterInfo {
        val basic = basicInfo?.let(::okFields).orEmpty()
        val time = dateTime?.let(::okFields).orEmpty()
        val remote = remoteMethod?.let(::okFields).orEmpty()
        return AdapterInfo(
            firmware = basic["ver"]?.takeIf { it.isNotBlank() }?.replace('_', '.'),
            macAddress = basic["mac"]?.let(::formatMac),
            outOfHome = when (remote["method"] ?: basic["method"]) {
                "polling" -> true
                "home_only" -> false
                else -> null
            },
            dateTime = time["cur"]?.let { runCatching { LocalDateTime.parse(it.trim(), ADAPTER_TIME) }.getOrNull() },
            timeZone = time["zone"]?.takeIf { it.isNotBlank() }?.let { ZONE_NAMES[it] ?: "Zone code $it" },
            automaticDst = flag(time["dst"] ?: basic["dst"]),
            timeSyncDisabled = flag(basic["sync_time"]),
        )
    }

    /** `90E868BD544C` → `90:E8:68:BD:54:4C`; anything not 12 hex digits is returned as is. */
    fun formatMac(raw: String): String? {
        val mac = raw.trim().uppercase()
        if (mac.isEmpty()) return null
        if (!mac.matches(Regex("[0-9A-F]{12}"))) return mac
        return mac.chunked(2).joinToString(":")
    }

    /** True when a basic_info body says `ret=OK`: the adapter answered over the LAN. */
    fun isOk(body: String?): Boolean = body != null && fields(body)["ret"] == "OK"

    private fun flag(value: String?): Boolean? = when (value) {
        "1" -> true
        "0" -> false
        else -> null
    }

    /** Fields of a body, or none when the adapter answered with anything but `ret=OK`. */
    private fun okFields(body: String): Map<String, String> =
        fields(body).takeIf { it["ret"] == "OK" }.orEmpty()

    private fun fields(body: String): Map<String, String> =
        body.split(",").mapNotNull { part ->
            val kv = part.split("=", limit = 2)
            if (kv.size == 2) kv[0].trim() to kv[1] else null
        }.toMap()
}
