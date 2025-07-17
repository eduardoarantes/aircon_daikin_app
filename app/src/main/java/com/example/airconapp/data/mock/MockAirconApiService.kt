package com.example.airconapp.data.mock

import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.ControlInfo
import com.example.airconapp.data.Zone
import com.example.airconapp.data.ZoneStatusResponse
import kotlinx.coroutines.delay

class MockAirconApiService : AirconApiService {

    private var mockControlInfo = ControlInfo(
        ret = "OK",
        pow = "1", // Start as ON
        mode = "2", // Cool
        stemp = "23",
        f_rate = "3",
        f_dir = "0"
    )

    private var mockZones = mutableListOf(
        Zone("Living Room", true),
        Zone("Bedroom", false),
        Zone("Kitchen", true),
        Zone("Office", false),
        Zone("Hall", true),
        Zone("Dining", false),
        Zone("Guest Main", true),
        Zone("Edu", false)
    )

    override suspend fun getControlInfo(): ControlInfo {
        delay(500) // Simulate network delay
        return mockControlInfo
    }

    override suspend fun setControlInfo(controlInfo: ControlInfo): String {
        delay(500) // Simulate network delay
        mockControlInfo = controlInfo
        return "ret=OK"
    }

    override suspend fun getZoneSetting(): ZoneStatusResponse {
        delay(500) // Simulate network delay
        return ZoneStatusResponse(mockZones)
    }

    override suspend fun setZoneSetting(zones: List<Zone>): String {
        delay(500) // Simulate network delay
        mockZones = zones.toMutableList()
        return "ret=OK"
    }
}
