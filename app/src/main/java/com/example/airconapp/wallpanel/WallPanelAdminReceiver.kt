package com.example.airconapp.wallpanel

import android.app.admin.DeviceAdminReceiver

/**
 * Device admin component. When the app is made device owner
 * (`adb shell dpm set-device-owner com.example.airconapp/.wallpanel.WallPanelAdminReceiver`)
 * simple mode can lock the phone into the app without the screen-pinning prompt.
 */
class WallPanelAdminReceiver : DeviceAdminReceiver()
