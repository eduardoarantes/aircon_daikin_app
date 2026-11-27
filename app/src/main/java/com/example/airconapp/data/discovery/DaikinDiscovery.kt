package com.example.airconapp.data.discovery

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URLDecoder

/** A Daikin unit that answered the discovery broadcast. */
data class DiscoveredUnit(val ip: String, val name: String?)

/**
 * Finds Daikin Wi-Fi adapters on the local network, the same way pydaikin / Home Assistant do:
 * a UDP broadcast of `DAIKIN_UDP/common/basic_info` to port 30050. Each adapter replies to the
 * sender's port with its basic_info (`ret=OK,type=aircon,...,name=%4d%61...`), from its own IP.
 */
object DaikinDiscovery {

    private const val DISCOVERY_PORT = 30050
    private val PROBE = "DAIKIN_UDP/common/basic_info".toByteArray()

    suspend fun discover(timeoutMs: Int = 3000): List<DiscoveredUnit> = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, DiscoveredUnit>()
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = 300
                socket.send(DatagramPacket(PROBE, PROBE.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT))

                val buffer = ByteArray(2048)
                val deadline = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < deadline) {
                    currentCoroutineContext().ensureActive()
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (e: SocketTimeoutException) {
                        continue
                    }
                    val ip = packet.address.hostAddress ?: continue
                    val reply = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    parseReply(ip, reply)?.let { found[ip] = it }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Discovery is best-effort; callers can still use their configured transport.
        }
        found.values.toList()
    }

    /** Returns the unit for a valid aircon reply, or null for anything else. */
    fun parseReply(ip: String, reply: String): DiscoveredUnit? {
        val fields = reply.split(",").mapNotNull { part ->
            val kv = part.split("=", limit = 2)
            if (kv.size == 2) kv[0].trim() to kv[1] else null
        }.toMap()
        if (fields["ret"] != "OK" || fields["type"] != "aircon") return null
        val name = fields["name"]
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
        return DiscoveredUnit(ip = ip, name = name)
    }
}
