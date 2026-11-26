# Spec Delta

## Purpose

Lets users view and edit the timer built into the Daikin adapter, which switches the unit on or off at a time of day without the phone being involved.

## ADDED Requirements

### Requirement: Timer availability

The app SHALL offer the unit timer only when the unit reports quick timer support (`en_quick_timer=1` in `get_model_info`). When support is absent or cannot be read, the unit timer SHALL NOT be offered.

#### Scenario: Supported unit

- **WHEN** `get_model_info` returns `en_quick_timer=1`
- **THEN** the Schedules page offers the unit timer

#### Scenario: Unsupported or unknown

- **WHEN** `get_model_info` returns `en_quick_timer=0`, omits it, or cannot be read
- **THEN** the Schedules page does not offer the unit timer

### Requirement: Show current timer slots

The app SHALL read the adapter's two timer slots from `get_quick_timer` and show, for each enabled slot, whether it turns the unit on or off and at what time of day. Times are stored as minutes after midnight (`t1_time=1020` means 17:00).

#### Scenario: One slot enabled

- **WHEN** `get_quick_timer` returns `t1_ena=1,t1_pow=1,t1_time=1020,t2_ena=0,...`
- **THEN** the unit timer shows "Turn on at 17:00" and shows the second slot as not set

#### Scenario: No slots enabled

- **WHEN** both `t1_ena` and `t2_ena` are `0`
- **THEN** the unit timer shows that no timer is set

### Requirement: Edit timer slots

The user SHALL be able to set each of the two slots to turn the unit on or off at a chosen time of day, and to disable each slot. Saving SHALL write all six fields (`t1_ena`, `t1_pow`, `t1_time`, `t2_ena`, `t2_pow`, `t2_time`) via `set_quick_timer` and SHALL treat only `ret=OK` as success.

#### Scenario: Save an off timer

- **WHEN** the user sets slot 2 to turn off at 23:00 and saves
- **THEN** the app sends `t2_ena=1,t2_pow=0,t2_time=1380` together with slot 1's current values, and shows the saved timer after `ret=OK`

#### Scenario: Disable a slot

- **WHEN** the user disables slot 1 and saves
- **THEN** the app sends `t1_ena=0` and keeps slot 1's other values unchanged

#### Scenario: Write rejected

- **WHEN** `set_quick_timer` returns anything other than `ret=OK`, or the unit cannot be reached
- **THEN** the app shows that the timer was not saved and keeps showing the adapter's previous values

### Requirement: Slots fire once

Each timer slot SHALL be treated as one-off: it fires at its time of day and the unit then disables it, so it does not repeat. The UI SHALL present slots as single occurrences ("at HH:MM", not a recurring daily time).

#### Scenario: Slot shown as one-off

- **WHEN** the user views or edits an enabled slot
- **THEN** it is presented as firing once at the given time, not as a daily repeat

#### Scenario: Slot clears after firing

- **WHEN** a slot has fired and the Schedules page is re-read
- **THEN** that slot shows as not set, because the unit disabled it

### Requirement: Timer limits are stated

The unit timer UI SHALL state its limits before or while editing: it fires once and does not repeat, it only turns the unit on or off, it uses the unit's last settings for mode, temperature, fan and zones, it has no day-of-week choice, it runs on the unit rather than the phone, and it may not run if the unit has no internet connection.

#### Scenario: Limits shown when editing

- **WHEN** the user opens the unit timer editor
- **THEN** the limits above are visible without extra navigation

### Requirement: Shared state with other controllers

The app SHALL treat the adapter as the source of truth for timer slots and SHALL re-read them each time the Schedules page is shown, because the official Daikin app edits the same slots.

#### Scenario: Timer changed in the official app

- **WHEN** the timer was changed in another app and the user returns to the Schedules page
- **THEN** the unit timer shows the values now stored on the adapter
