# Tasks

## 1. Conversion helper and preference

- [x] 1.1 Add `domain/TemperatureUnit.kt`: enum (`CELSIUS`, `FAHRENHEIT`, labels), `fromName` default-Celsius, `displayTemp(Int, unit)`, `displayTemp(String?, unit)` (tolerating `"-"`/blank/non-numeric → null), and `label(unit)`; verify with a unit test covering 16/21/22/32 °C → °F, Celsius passthrough, and null/`"-"`/empty string handling.
- [x] 1.2 Add `KEY_TEMP_UNIT` with `getTemperatureUnit()`/`setTemperatureUnit()` in `NetworkModule` (mirroring the ThemeMode prefs); verify get/set/default round-trips (reuse the existing prefs test approach).

## 2. Settings control

- [x] 2.1 Expose the unit as reactive state in `SettingsViewModel` with a setter, and add a "Temperature unit" °C/°F control to `SettingsScreen` matching the theme control; verify by building and toggling it, confirming the choice persists across app restart.

## 3. Apply the unit at every display site

- [x] 3.1 Expose the current unit as reactive state on the view models that render temperatures (e.g. `MainViewModel`) so screens re-render on change; verify by toggling the setting and seeing Controls update live.
- [x] 3.2 `ControlsScreen`: convert the dial setpoint number, the indoor reading, and the unit label via the helper (stepper/dial still operate on Celsius `Int`); verify in-app that °F shows converted values and the dial still steps by 1 °C.
- [x] 3.3 `SchedulesScreen` and `ScenesScreen` list cards: convert the shown temperature and label; verify in-app in both units.
- [x] 3.4 `ScheduleEditScreen` and `SceneEditScreen` `TemperatureCard`: show the converted value and label while keeping the `Int` Celsius state and Celsius range/coercion; verify stepping and saving produce the same stored Celsius value in either unit.
- [x] 3.5 `SimpleModeScreenContent` Set/Room cards and `SettingsScreen` simple-mode start temperature: convert display and label (start-temp stays stored as Celsius `Int`); verify in-app in both units.

## 4. Verification

- [x] 4.1 Grep the UI for remaining hard-coded `°C`/`°F` literals outside `TemperatureUnit`; confirm every temperature renders through the helper.
- [x] 4.2 Run `./gradlew testDebugUnitTest` and confirm all tests pass.
- [x] 4.3 In-app end-to-end: switch to °F, confirm Controls, Schedules, Scenes, both editors, Simple mode and Settings all read °F consistently; set a schedule/scene in °F and confirm the saved Celsius value is unchanged when switched back to °C.

## Workflow follow-up

- Archive the change once implemented and verified.

## Verification evidence (2026-11-27)

- `./gradlew testDebugUnitTest assembleDebug`: successful; 62 tests, no failures or skips.
- `openspec validate add-temperature-unit-setting --strict`: passed.
- Source audit: unit literals exist only in `TemperatureUnit.kt`; the dial's accessibility state also uses the display helpers.
- Local Android emulator, mock API: verified live unit changes, Fahrenheit persistence after process restart, Controls setpoint/room readings, one-Celsius stepping, both lists and editors, Settings start temperature, and Simple mode Set/Room cards in both units.
- Saved a scene and schedule at 72 Fahrenheit; both reloaded as 22 Celsius after changing units. Inspected scene JSON and the Room schedule record to confirm stored values remain 22 Celsius. Settings start-temperature stepping from 70 to 72 Fahrenheit likewise persisted 22 Celsius.
- Emulator preferences and databases were backed up before verification and restored byte-for-byte afterward. No physical-device or real-adapter tests were performed.
