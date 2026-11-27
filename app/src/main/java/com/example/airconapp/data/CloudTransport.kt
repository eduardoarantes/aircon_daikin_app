package com.example.airconapp.data

import java.net.URLEncoder

/**
 * An aircon API target: a base URL plus optional authentication/target query parameters.
 *
 * - Local adapter: `Endpoint("http://<ip>")` — no auth params.
 * - Cloud relay: `CloudRelay.endpoint(id, spw)` — carries `id`, `spw`, `device`, `port`.
 *
 * The same request-building and key=value parsing serves both transports; only the base URL and
 * these extra params differ. See the `add-remote-cloud-control` change.
 */
data class Endpoint(val baseUrl: String, val authParams: Map<String, String> = emptyMap()) {
    val isCloud: Boolean get() = authParams.isNotEmpty()
}

/** Central definition of Daikin's cloud relay host and the fixed target constants. */
object CloudRelay {
    /** Region `au` relay; undocumented third-party. Isolated here so it can be revisited. */
    const val HOST = "https://dascontroller.com"

    /** The app controls the primary unit; the relay always addresses it as `device=A`. */
    const val DEVICE = "A"

    /** The adapter's aircon port, sent literally on every cloud aircon call. */
    const val PORT = "30050"

    /**
     * The relay's write path is untested end-to-end; the reverse-engineered notes show plain GETs.
     * Keep writes-as-GET switchable from one place while the write payload is verified (tasks 6.2).
     */
    const val WRITES_AS_GET = true

    fun endpoint(id: String, spw: String): Endpoint =
        Endpoint(
            HOST,
            // Order is fixed so built URLs are deterministic (and testable).
            linkedMapOf("id" to id, "spw" to spw, "device" to DEVICE, "port" to PORT)
        )
}

/**
 * Builds the full request URL for an aircon [operation] on [endpoint], appending the endpoint's
 * auth params to any [existingQuery] the operation already carries (writes) without ever producing
 * a malformed `?a=b?c=d`. [existingQuery] is assumed already URL-encoded by the caller; auth param
 * values are URL-encoded here.
 */
internal fun buildAirconUrl(endpoint: Endpoint, operation: String, existingQuery: String? = null): String {
    val base = "${endpoint.baseUrl}/skyfi/aircon/$operation"
    val authQuery = endpoint.authParams.entries.joinToString("&") { (key, value) ->
        "$key=${URLEncoder.encode(value, "UTF-8")}"
    }
    val query = listOf(existingQuery, authQuery)
        .filter { !it.isNullOrBlank() }
        .joinToString("&")
    return if (query.isBlank()) base else "$base?$query"
}

/** What a cloud relay response means, derived purely from its `ret` field. */
enum class CloudOutcome {
    /** Proceed (read: parse the body; write: success). */
    OK,

    /** `ret=ACCOUNT NG`: the stored credential is stale — re-enrol from home. */
    STALE_CREDENTIAL,

    /** A write returned a non-OK, non-stale result: the relay rejected the operation. */
    REJECTED
}

/** Parses the `ret` value out of a key=value relay body, or null if absent. */
internal fun retOf(raw: String): String? =
    raw.split(",").firstNotNullOfOrNull { part ->
        val kv = part.split("=", limit = 2)
        if (kv.size == 2 && kv[0].trim() == "ret") kv[1].trim() else null
    }

/**
 * Classifies a cloud response. Reads only care about a stale credential (otherwise the body is
 * parsed as usual, even if `ret` is absent). Writes require an explicit `ret=OK`.
 */
internal fun classifyCloudResponse(raw: String, isWrite: Boolean): CloudOutcome {
    val ret = retOf(raw)
    return when {
        ret == "ACCOUNT NG" -> CloudOutcome.STALE_CREDENTIAL
        isWrite && ret != "OK" -> CloudOutcome.REJECTED
        else -> CloudOutcome.OK
    }
}
