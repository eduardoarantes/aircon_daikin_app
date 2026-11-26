# Tasks

## 1. Phone schedule disclosure

- [ ] 1.1 Add the disclosure to `SchedulesScreen` so it shows in both the empty state and the list ("Schedules run on this phone. They only work if the phone is on and can reach the unit at that time."); verify by building and opening the Schedules page with and without schedules.

## 2. Timer data layer

- [ ] 2.1 Add a `QuickTimer` model (two slots: enabled, power on/off, minutes after midnight) with pure parse and serialise functions; verify with unit tests using the captured string `ret=OK,t1_ena=0,t1_pow=1,t1_time=1020,t2_ena=0,t2_pow=0,t2_time=1050`, including time formatting (1020 → 17:00, 1380 → 23:00).
- [ ] 2.2 Add `getModelInfo` support flag parsing (`en_quick_timer`) with a unit test for `1`, `0` and missing.
- [ ] 2.3 Add `getQuickTimer` and `setQuickTimer` to `AirconApiService`, `AirconApiServiceImpl` (always sending all six fields) and `MockAirconApiService`; verify with a unit test that the built `set_quick_timer` query contains all six fields and that a non-`ret=OK` response is reported as failure.

## 3. Unit timer UI

- [ ] 3.1 Load timer support and current slots in `SchedulerViewModel` each time the Schedules page is shown; verify with a view-model test using the mock service (supported, unsupported, unreachable).
- [ ] 3.2 Add the "Unit timer" card on the Schedules page showing each slot ("Turn on at 17:00" / not set), hidden when unsupported, and add the unit-timer pointer to the disclosure only when supported; verify by building and checking both mock states.
- [ ] 3.3 Add the unit timer editor (per slot: on/off, time, enabled) with the limits text visible, saving through `setQuickTimer` and showing an error while keeping old values on failure; verify on the mock, including a forced failure.

## 4. Real unit verification

- [ ] 4.1 On the real unit (unit OFF; never enable a slot with power on): save an enabled OFF slot a few minutes ahead, confirm `get_quick_timer` shows it and the unit stays off, then after its time passes confirm the slot has cleared (one-off, disables itself). UI wording is "at HH:MM" with no daily-repeat claim.
- [ ] 4.2 Disable the test slot again and confirm `get_quick_timer` matches what the app shows.
- [ ] 4.3 Run `./gradlew testDebugUnitTest` and confirm all tests pass.

## Workflow follow-up

- Archive the change once implemented and verified.
