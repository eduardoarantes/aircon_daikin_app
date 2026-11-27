package com.example.airconapp.data.remote

import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkRoutingTest {
    private fun capabilities(vararg transports: Int): NetworkCapabilities {
        val capabilities = mockk<NetworkCapabilities>()
        every { capabilities.hasTransport(any()) } answers { firstArg<Int>() in transports }
        return capabilities
    }

    @Test fun `only cellular-only networks bypass LAN`() {
        assertTrue(isCellularOnly(capabilities(NetworkCapabilities.TRANSPORT_CELLULAR)))
        assertFalse(isCellularOnly(capabilities(NetworkCapabilities.TRANSPORT_WIFI)))
        assertFalse(isCellularOnly(capabilities(NetworkCapabilities.TRANSPORT_ETHERNET)))
        assertFalse(isCellularOnly(capabilities(NetworkCapabilities.TRANSPORT_CELLULAR, NetworkCapabilities.TRANSPORT_VPN)))
        assertFalse(isCellularOnly(capabilities(NetworkCapabilities.TRANSPORT_CELLULAR, NetworkCapabilities.TRANSPORT_WIFI)))
        assertFalse(isCellularOnly(capabilities()))
        assertFalse(isCellularOnly(null))
    }
}
