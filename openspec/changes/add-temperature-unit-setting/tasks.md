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

## 5. Save/discard follow-up

- [x] 5.1 Stage temperature unit and theme with the existing settings fields; track edits, support discard and preserve zone validation on save.
- [x] 5.2 Prompt with Save or Discard from both toolbar and Android Back; dismissing continues editing and unchanged/reverted settings exit directly.
- [x] 5.3 Verify save/discard/revert behavior, restart persistence, and both back paths; run unit tests, build, and strict OpenSpec validation.

### Follow-up verification

- `./gradlew testDebugUnitTest assembleDebug`: passed, 66 tests. Regression coverage includes temperature/theme drafts, all saved settings fields, reverting/discarding, save persistence, and invalid Simple mode zone selection.
- Emulator with mock API: confirmed unsaved temperature choices do not change stored preferences; toolbar Back and Android Back both prompt; dismiss keeps editing; Discard leaves saved Celsius unchanged; Save commits Fahrenheit and updates Controls; unchanged/reverted settings exit directly; theme edits prompt and can be discarded; saved Fahrenheit survives process restart.
- Restored emulator preferences and databases byte-for-byte after verification. This follow-up supersedes the original immediate-persistence behavior: unit and theme changes now take effect when saved.

## 6. Performance and security review fixes

- [x] 6.1 Authorize published shortcut commands with an installation-private token, reject missing/forged commands, and exclude the token from backup/transfer.
- [x] 6.2 Flush only pending edits on ViewModel clear; unchanged, reverted, and already-sent settings must not be replayed.
- [x] 6.3 Initialize services at application startup and lazily access them in workers after worker initialization, so scheduled work survives a cold process start.
- [x] 6.4 Coalesce status refreshes, await them in the polling loop, and cancel in-flight requests when hidden or cleared.
- [x] 6.5 Publish control/zone state before optional sensor requests and preserve cancellation.
- [x] 6.6 Remember failed LAN probes for 30 seconds per active network/address when remote access is available; retry local on a network change or deadline and preserve write safety.
- [x] 6.7 Fix the three lint errors, verify debug/release builds and regression/integration tests, and exclude preview tooling from release.

### Performance/security verification (2026-11-27)

- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`: passed. 84 JVM tests, zero failures/skips; lint has zero errors (43 dependency/style warnings remain).
- Regression tests cover no writes on unchanged close, flushing only actual pending edits once, reverting/already-sent edits, single refresh ownership and cancellation on hide, controls available while sensors wait, polling restart, missing/forged shortcut capabilities, persistent authorization, failed-LAN reuse, network changes, local recovery after 30 seconds, cancellation, and existing ambiguous-write safety.
- `StartupSecurityTest` on the local emulator: four tests passed with no skips. Verified process initialization without an activity, construction/startup of both scheduled workers using missing IDs (no adapter commands), forged shortcut intents enqueueing no commands, and a published authorized shortcut successfully running in one tap against mock mode.
- Emulator launch check confirmed Controls loads with the default ViewModel factory. Original emulator preferences and databases are restored after verification.
- Release manifest uses AirconApplication and contains no preview tooling activity. No real-adapter commands were used in tests.
