package com.example.airconapp.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Exercises [AirconApiServiceImpl.request] over the wire with a real CIO client against a loopback
 * socket server — so the cloud URL, the GET-for-writes choice, the `ret` handling and the sanitised
 * error all run here, not only in the user's manual end-to-end check (tasks 6.2).
 *
 * Uses a minimal raw-socket HTTP server (no `com.sun.net.httpserver`, which isn't on the Android
 * unit-test compile classpath, and no extra test dependencies).
 */
class AirconApiServiceImplWireTest {

    private lateinit var server: ServerSocket
    private lateinit var client: HttpClient
    private var port = 0

    /** The request line ("GET /path?query HTTP/1.1") of the last request the server received. */
    @Volatile private var lastRequestLine: String? = null
    @Volatile private var responseBody: String = "ret=OK"

    private fun cloudAt(host: String) = AirconApiServiceImpl(
        client,
        Endpoint(host, mapOf("id" to "unit-id", "spw" to "server-pw", "device" to "A", "port" to "30050"))
    )

    private val cloud get() = cloudAt("http://127.0.0.1:$port")

    @Before
    fun setUp() {
        server = ServerSocket(0)
        port = server.localPort
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (e: Exception) { break }
                handle(socket)
            }
        }
        client = HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = 1_000
                requestTimeoutMillis = 2_000
            }
        }
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            lastRequestLine = reader.readLine()
            // Drain headers up to the blank line (requests under test carry no body).
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }
            val bytes = responseBody.toByteArray()
            val response = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: text/plain\r\n")
                append("Content-Length: ${bytes.size}\r\n")
                append("Connection: close\r\n")
                append("\r\n")
            }
            s.getOutputStream().apply {
                write(response.toByteArray())
                write(bytes)
                flush()
            }
        }
    }

    /** The "/path?query" portion of the captured request line. */
    private fun target(): String = lastRequestLine!!.split(" ")[1]

    private fun method(): String = lastRequestLine!!.split(" ")[0]

    @After
    fun tearDown() {
        client.close()
        server.close()
    }

    @Test
    fun `cloud read sends id spw device port and parses the body`() = runBlocking {
        responseBody = "ret=OK,pow=1,mode=3,stemp=24,f_rate=A,f_dir=0"

        val info = cloud.getControlInfo()

        assertEquals("GET", method())
        assertEquals(
            "/skyfi/aircon/get_control_info?id=unit-id&spw=server-pw&device=A&port=30050",
            target()
        )
        assertEquals("24", info.stemp)
    }

    @Test
    fun `cloud write is sent as a GET with the payload and auth in the query`() = runBlocking {
        responseBody = "ret=OK"

        cloud.setControlInfo(
            ControlInfo(pow = "1", mode = "3", stemp = "19", f_rate = "A", f_dir = "0")
        )

        assertEquals("GET", method()) // WRITES_AS_GET for the relay
        assertEquals(
            "/skyfi/aircon/set_control_info?pow=1&mode=3&stemp=19&f_rate=A&f_dir=0" +
                "&id=unit-id&spw=server-pw&device=A&port=30050",
            target()
        )
    }

    @Test
    fun `account NG on a cloud call raises a stale-credential error`() = runBlocking {
        responseBody = "ret=ACCOUNT NG"

        try {
            cloud.getControlInfo()
            fail("expected RemoteCredentialException")
        } catch (e: RemoteCredentialException) {
            // expected
        }
    }

    @Test
    fun `a non-OK cloud write is rejected`() = runBlocking {
        responseBody = "ret=PARAM NG"

        try {
            cloud.setControlInfo(ControlInfo(pow = "1", mode = "3", stemp = "19", f_rate = "A", f_dir = "0"))
            fail("expected RemoteException")
        } catch (e: RemoteException) {
            // expected
        }
    }

    @Test
    fun `an unreachable cloud host fails without leaking the credential`() = runBlocking {
        // A port that is allocated then immediately closed: connections are refused.
        val closedPort = ServerSocket(0).use { it.localPort }

        try {
            cloudAt("http://127.0.0.1:$closedPort").getControlInfo()
            fail("expected ConnectionException")
        } catch (e: ConnectionException) {
            assertFalse("message must not contain the credential", e.message!!.contains("server-pw"))
            assertFalse("message must not contain the url", e.message!!.contains("127.0.0.1"))
            assertNull("no cause, which could carry the url", e.cause)
        }
    }
}
