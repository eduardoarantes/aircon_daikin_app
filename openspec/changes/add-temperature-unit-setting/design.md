# Design

## Context

See proposal.md — Why. Temperature is Celsius everywhere: setpoints are `Int` °C (`ControlsScreen`, `ScheduleEditScreen`, `SceneEditScreen` with `TEMP_RANGE`/`SceneLogic.TEMPERATURE_RANGE`), sensor readings are strings from `get_sensor_info` (`indoorTemp`/`outdoorTemp`, `"-"` when absent), and the `°C` label is hard-coded at each site. The adapter accepts whole-Celsius setpoints and reports Celsius limits (`cool_l`/`heat_l`). A `ThemeMode` enum + `NetworkModule` get/set + a Settings control is the established pattern for a display preference; the unit setting mirrors it exactly.

## Goals / Non-Goals

**Goals:**
- One pure, tested conversion used by every display site.
- Preference plumbed like `ThemeMode` (prefs + Settings control), reactive so screens update when it changes.
- Zero change to stored data, payloads, ranges, or stepping logic.

**Non-Goals:**
- Fahrenheit-granularity setpoints or any sub-degree Celsius storage.
- Converting ranges/limits or the adapter timer times.
- Per-screen unit overrides; one app-wide setting.

## Decisions

- **`TemperatureUnit` enum with pure helpers.** `domain/TemperatureUnit.kt`: `CELSIUS`, `FAHRENHEIT`, plus `fromName` (null-safe, defaults Celsius) like `ThemeMode`. Helpers:
  - `displayTemp(celsius: Int, unit): Int` → `unit == FAHRENHEIT ? round(c*9/5+32) : c`.
  - `displayTemp(celsiusText: String?, unit): String?` → parse (tolerating `"-"`/non-numeric → null), convert, format; for sensor strings.
  - `label(unit): String` → `"°C"`/`"°F"`.
  These are the only place conversion lives, and they are unit-tested (22→72, 21→70, 16→61, 32→90, null/`"-"` passthrough). Alternative considered: a Compose `CompositionLocal` carrying the unit. Rejected as heavier than needed; screens already read other prefs via the view models.
- **Preference plumbing mirrors ThemeMode.** `KEY_TEMP_UNIT` in `NetworkModule` with `getTemperatureUnit()/setTemperatureUnit()`; expose as reactive state on the view models that render temperatures (`MainViewModel` for Controls/Simple mode, plus the editors) the same way theme is exposed, so a change re-renders.
- **Display-only boundary.** Setpoint state stays `Int` °C everywhere; only the rendered `Text` converts. Steppers/dial keep operating on the Celsius `Int` and Celsius ranges, so stepping, coercing and payloads are untouched. This is what makes Fahrenheit safe to add without risking the control path.
- **Settings control shape.** A segmented/toggle "Temperature unit: °C / °F" row in `SettingsScreen`, consistent with the existing theme control.

## Risks / Trade-offs

- [Fahrenheit steps look uneven] → Stepping by 1 °C shows as 1–2 °F jumps and some odd °F values never appear. Accepted per product decision (display-only, no rounding back); the alternative drifts or sticks. Not surfaced in UI beyond the values themselves.
- [Missed display site] → A hard-coded `°C` left somewhere shows the wrong unit. Mitigation: the grep of temperature sites in tasks enumerates every site; a task verifies no `°C`/`°F` string literal remains outside the helper.
- [Sensor value formatting] → Indoor/outdoor are strings and may be non-integer; the string helper must not crash on `"-"` or empty. Covered by the helper's tests.
