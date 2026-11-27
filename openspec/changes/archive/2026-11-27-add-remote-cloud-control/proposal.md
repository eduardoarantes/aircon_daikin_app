# Proposal

## Why

The app can only control the aircon while the phone is on the same Wi-Fi as the Daikin adapter: every call is a LAN request to the adapter's IP, and away from home it simply fails with a connection error. The official Daikin app works remotely, and we have confirmed it does so through Daikin's own cloud relay (`dascontroller.com`) using the same `/skyfi/aircon/*` key=value API we already speak on the LAN. Adding this lets users control the AC from anywhere with no VPN and no new backend.

## What Changes

- Add a **cloud transport** for the existing aircon API: the same requests (`get_control_info`, `set_control_info`, `get_zone_setting`, `set_zone_setting`, `get_sensor_info`) sent to `https://dascontroller.com/skyfi/...` with authentication query parameters instead of to the adapter's LAN IP.
- Add a **cloud login step**: one-time capture of the adapter's `id` and `pw` from `basic_info` while on the LAN, then `GET /skyfi/common/get_spw?id=&apw=<pw>` to obtain the server password `spw`. Store the credential and refresh `spw` when the server reports `ACCOUNT NG`.
- Add **transparent fallback**: when the unit is not reachable on the LAN (discovery/heal fails), route calls through the cloud instead of surfacing a connection error, so control keeps working away from home.
- Surface **connection mode** (local vs remote) to the UI and expose a settings toggle to enrol/disenrol remote access and clear the stored credential.
- Account for cloud behaviour: reads are served from the server's cached state (near-instant); writes are queued to the adapter's ~60 s poll, so remote writes confirm with a delay.

## Capabilities

### New Capabilities
- `remote-access`: Controlling and reading the aircon when the phone is not on the adapter's local network, including the cloud credential/login flow, the cloud transport for the aircon API, and the local↔remote fallback policy.

### Modified Capabilities
<!-- None. The LAN behaviour is unspecified today (no existing specs); this change only adds the remote path and the fallback that selects it. -->

## Impact

- **Code**: `data/AirconApi.kt` (base-URL/path building and auth params), `di/NetworkModule.kt` (transport/base-URL selection, credential storage in SharedPreferences, fallback wiring alongside `healConnection`), `data/discovery/DaikinDiscovery.kt` (one-time credential capture from `basic_info`), `data/ConnectionException.kt` and callers in `presentation/MainViewModel.kt` (fallback instead of error), plus a settings surface in `ui/screens/SettingsScreen.kt` / `presentation/SettingsViewModel.kt`.
- **External dependency**: Daikin's cloud relay `dascontroller.com` (region-specific; ours is `reg=au`). Undocumented and third-party — availability and protocol are outside our control.
- **Privacy/security**: the stored `spw` (or adapter `pw`) and `id` are sent to Daikin's server in the clear within HTTPS query strings; the credential must be stored on-device and never logged.
- **No new backend, no new libraries**: reuses the existing Ktor client and key=value parsing.
