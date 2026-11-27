# Design

## Context

See proposal.md — Why. App schedules are `SchedulerProfile` rows in Room, run by `ScheduleWorker` through WorkManager on the phone. `SchedulesScreen` lists them from `SchedulerViewModel.allProfiles`. `AirconApiService` has no timer or model-info calls today; `MockAirconApiService` implements the same interface.

Probing our unit (AirBase, fw 1_3_0) showed `get_model_info` → `en_quick_timer=1,en_scdltmr=0`, and `get_quick_timer` → `ret=OK,t1_ena=0,t1_pow=1,t1_time=1020,t2_ena=0,t2_pow=0,t2_time=1050`. The official app writes the same fields to `set_quick_timer` and labels them "Turning on at" / "Turning off at".

## Goals / Non-Goals

**Goals:**
- Make the phone dependency of app schedules obvious on the Schedules page.
- Let users set the adapter's two timer slots from the app, with the limits stated up front.

**Non-Goals:**
- The weekly schedule timer (`scdltimer`): our unit reports `en_scdltmr=0`.
- Holiday mode (`set_holiday`), which disables all timers. Our unit reports `holiday=0`; we neither show nor change it.
- Converting existing app schedules into unit timers.
- Editing the timer away from home; that comes with `add-remote-cloud-control`.

## Decisions

- **One section, two kinds.** The Schedules page keeps app schedules as today and adds a "Unit timer" card above the list, opening its own small editor. Alternative considered: a "Run on unit" toggle inside the existing schedule editor. Rejected because the timer can't hold mode, temperature, fan or zones, so most of that editor would be disabled, which is confusing.
- **Model the timer as two slots.** A `QuickTimer(slot1, slot2)` value with each slot `(enabled, powerOn, minutesAfterMidnight)`, parsed and serialised by pure functions so they can be unit-tested against the captured response string.
- **Always write all six fields.** `set_quick_timer` gets both slots each time, built from the last read values plus the user's edit. This avoids depending on whether the adapter accepts partial writes.
- **Write with GET.** Use the same request style the cloud and pydaikin use for writes (full parameter set on a GET); matches what we already confirmed the adapter accepts for control writes.
- **Feature gate from `get_model_info`.** Read once when the Schedules page loads, cache in the view model, and hide the timer card and the disclosure's pointer when support is not reported.
- **Adapter is the source of truth.** Re-read the timer whenever the Schedules page is shown; no local copy in Room.

## Risks / Trade-offs

- [One-off, not recurring] → Each slot fires once and then disables itself (confirmed); it is not a daily timer. UI copy says "at HH:MM", and after a slot fires the next read shows it as not set. Users expecting a repeating schedule must re-set it, which the UI should make clear.
- [Testing could turn the unit on] → Never enable a slot with `pow=1` during tests. Test writes use disabled slots or an enabled OFF slot only, per the project rule.
- [Shared with the official app] → Saving overwrites whatever the official app set. The editor shows current adapter values before editing, so the user sees what they replace.
- [Adapter needs internet] → Daikin says the timer may not run without internet (likely clock sync). We state it in the UI; we can't detect it.

