package com.example.airconapp.data.remote

import android.net.NetworkCapabilities

/** Unknown networks and VPNs remain local-first because they may provide a home LAN route. */
internal fun isCellularOnly(capabilities: NetworkCapabilities?): Boolean =
    capabilities != null &&
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
        !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
        !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) &&
        !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
