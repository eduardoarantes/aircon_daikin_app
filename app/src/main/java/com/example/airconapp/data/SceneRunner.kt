package com.example.airconapp.data

import com.example.airconapp.di.NetworkModule
import com.example.airconapp.domain.Scene
import com.example.airconapp.domain.SceneLogic

/** Sends a scene to the unit, used by both the Scenes tab and the launcher shortcuts. */
object SceneRunner {

    /**
     * Zones first, so the aircon never starts blowing into the wrong rooms, then mode,
     * temperature and power. If the unit can't be reached it is searched for once (it may have a
     * new IP) and the scene tried again. Returns what was sent.
     */
    suspend fun apply(scene: Scene): Pair<ControlInfo, List<Zone>> = try {
        send(NetworkModule.airconApiService, scene)
    } catch (e: Exception) {
        if (e is kotlin.coroutines.cancellation.CancellationException) throw e
        if (!NetworkModule.healConnection()) throw e
        send(NetworkModule.airconApiService, scene)
    }

    private suspend fun send(api: AirconApiService, scene: Scene): Pair<ControlInfo, List<Zone>> {
        val currentZones = api.getZoneSetting().zones
        val zones = SceneLogic.zones(currentZones, scene)
        if (zones != null) api.setZoneSetting(zones)
        val control = SceneLogic.control(api.getControlInfo(), scene)
        api.setControlInfo(control)
        return control to (zones ?: currentZones)
    }
}
