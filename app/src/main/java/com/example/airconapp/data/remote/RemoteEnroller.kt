package com.example.airconapp.data.remote

/** Outcome of trying to enrol (or re-enrol) remote access. */
sealed interface EnrolResult {
    /** `spw` derived and stored; cloud calls can now authenticate. */
    object Success : EnrolResult

    /** The adapter could not be read over the LAN: enrolment must be done from the home network. */
    object NotOnLocalNetwork : EnrolResult

    /** The adapter was reached but the relay did not hand back a valid `spw`. */
    object Rejected : EnrolResult
}

/**
 * Derives the cloud server password (`spw`) and stores the credential. The two network reads are
 * injected so the decision logic is unit-testable without any HTTP:
 *
 * 1. [readBasicInfo] — `GET http://<adapter>/skyfi/common/basic_info` over the LAN; returns the
 *    raw body, or null when the adapter is not reachable (enrolment requires being home).
 * 2. [fetchSpw] — `GET <relay>/skyfi/common/get_spw?id=<id>&apw=<adapter pw>`; returns the raw
 *    body, or null when the relay could not be reached.
 *
 * On `ret=OK` with an `spw`, [store] is called with (`id`, `spw`).
 */
class RemoteEnroller(
    private val readBasicInfo: suspend () -> String?,
    private val fetchSpw: suspend (id: String, apw: String) -> String?,
    private val store: (RemoteCredential) -> Unit,
) {
    suspend fun enrol(): EnrolResult {
        val basic = readBasicInfo() ?: return EnrolResult.NotOnLocalNetwork
        val basicFields = parseKeyValues(basic)
        val id = basicFields["id"]?.takeIf { it.isNotBlank() }
        val pw = basicFields["pw"]?.takeIf { it.isNotBlank() }
        if (id == null || pw == null) return EnrolResult.NotOnLocalNetwork

        val spwBody = fetchSpw(id, pw) ?: return EnrolResult.Rejected
        val spwFields = parseKeyValues(spwBody)
        if (spwFields["ret"] != "OK") return EnrolResult.Rejected
        val spw = spwFields["spw"]?.takeIf { it.isNotBlank() } ?: return EnrolResult.Rejected

        store(RemoteCredential(id, spw))
        return EnrolResult.Success
    }

    companion object {
        /** Paths used only by enrolment (not the aircon ops): under `/skyfi/common/`. */
        const val BASIC_INFO_PATH = "/skyfi/common/basic_info"
        const val GET_SPW_PATH = "/skyfi/common/get_spw"

        internal fun parseKeyValues(input: String): Map<String, String> =
            input.split(",").mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) kv[0].trim() to kv[1] else null
            }.toMap()
    }
}
