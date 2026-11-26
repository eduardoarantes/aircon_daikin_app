# Proposal

## Why

Schedules created in the app are executed by the phone (Android WorkManager), so they silently do nothing if the phone is off, out of battery, or cannot reach the unit at the scheduled time. Users are not told this. The AirBase adapter has its own built-in timer that runs without the phone, but the app does not expose it, so there is no way to set a schedule that survives the phone being off.

## What Changes

- Add a clear disclosure on the Schedules page explaining that app schedules run on this phone, and only fire if the phone is on and can reach the unit at that time.
- Add a "unit timer" (remote schedule) option on the Schedules page that reads and writes the adapter's quick timer (`/skyfi/aircon/get_quick_timer`, `/skyfi/aircon/set_quick_timer`): up to two slots, each a time of day that turns the unit on or off.
- Make the unit timer's limits explicit in the UI: fires once (each slot disables itself after firing, so it does not repeat), power on/off only (no mode, temperature, fan or zones), no day-of-week, runs on the adapter, and may not run if the adapter has no internet.
- Only offer the unit timer when the adapter reports support (`en_quick_timer=1` in `get_model_info`).

## Capabilities

### New Capabilities
- `schedules`: Phone-run schedules on the Schedules page, starting with the disclosure of where they run and what they depend on.
- `adapter-timer`: Viewing and editing the timer built into the Daikin adapter, which runs without the phone and is limited to switching power at a time of day.

### Modified Capabilities

## Impact

- **Code**: `ui/screens/SchedulesScreen.kt` (disclosure, unit timer section), a new edit UI for the timer, `presentation/SchedulerViewModel.kt` (timer state), `data/AirconApi.kt` (`getQuickTimer`, `setQuickTimer`, `getModelInfo` support flag), `data/mock/MockAirconApiService.kt` (mock timer).
- **Device behaviour**: writing the quick timer changes state stored on the adapter, shared with the official Daikin app (both apps edit the same two slots).
- **Related change**: `add-remote-cloud-control` would let the unit timer also be edited away from home; this change does not depend on it.
