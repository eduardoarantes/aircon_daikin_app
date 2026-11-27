# Proposal

## Why

The app shows every temperature in Celsius with a hard-coded `°C` label. Users who think in Fahrenheit have no way to switch. The adapter is Celsius-native (whole-degree setpoints, `cool_l`/`heat_l` limits in °C), so the fix is a display-only unit setting, not a change to how temperatures are stored or sent.

## What Changes

- Add a **temperature unit preference** (Celsius or Fahrenheit, default Celsius) stored in SharedPreferences, following the existing `ThemeMode` pattern in `NetworkModule`.
- Add a **Temperature unit** control to the Settings screen to change it. Draft settings (including theme and temperature unit) are committed with Save; leaving with edits prompts to Save or Discard.
- Add a **pure conversion helper** (`°C Int` → displayed number + unit label, and sensor-string → displayed) that is unit-tested.
- Make **every temperature display honour the setting**: Controls (dial setpoint, indoor reading, unit label), Schedules list, Scenes list, the Scene and Schedule editors' `TemperatureCard`, Simple mode's Set/Room cards, and the Settings simple-mode start temperature.
- Keep **stepping in whole °C**: the +/- buttons and dial move by 1 °C as today; in Fahrenheit the shown value therefore jumps by 1–2 °F per tap (the exact jump varies with rounding). No sub-degree storage, no rounding back to the adapter.

## Capabilities

### New Capabilities
- `temperature-units`: How temperatures are displayed to the user, including the Celsius/Fahrenheit preference and the rule that it affects display only while storage, control payloads and limits stay in Celsius.

### Modified Capabilities

## Impact

- **Code**: new `domain/TemperatureUnit.kt` (enum + conversion helpers) with a unit test; `di/NetworkModule.kt` (preference get/set); `presentation/SettingsViewModel.kt` + `ui/screens/SettingsScreen.kt` (the control); and the display sites `ControlsScreen.kt`, `SchedulesScreen.kt`, `ScenesScreen.kt`, `ScheduleEditScreen.kt`, `SceneEditScreen.kt`, `ui/components/SimpleModeScreenContent.kt`.
- **Unchanged**: adapter API payloads, Room data (`SchedulerProfile`, scenes), simple-mode start temperature storage, and model-info limits all remain Celsius. Setpoint ranges (`TEMP_RANGE`, `cool_l`/`heat_l`, etc.) stay Celsius integers.
- **No new dependencies.**
