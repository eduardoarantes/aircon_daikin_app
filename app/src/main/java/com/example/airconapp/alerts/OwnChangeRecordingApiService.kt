package com.example.airconapp.alerts

import com.example.airconapp.data.AirconApiService
import com.example.airconapp.data.ControlInfo

/**
 * Passes every call through to [delegate] and, after a successful [setControlInfo], tells the
 * alert monitor what this app just sent. That way the app's own changes (controls, scenes,
 * schedules, shortcuts) never raise "someone turned the aircon on/off" alerts.
 */
class OwnChangeRecordingApiService(
    private val delegate: AirconApiService,
    private val recordOwnChange: (ControlInfo) -> Unit
) : AirconApiService by delegate {

    override suspend fun setControlInfo(controlInfo: ControlInfo): String =
        delegate.setControlInfo(controlInfo).also { recordOwnChange(controlInfo) }
}
