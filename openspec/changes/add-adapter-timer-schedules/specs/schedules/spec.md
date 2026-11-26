# Spec Delta

## Purpose

Covers schedules that the app stores and runs on the phone, including telling users where these schedules run and what they depend on.

## ADDED Requirements

### Requirement: Phone schedule disclosure

The Schedules page SHALL show a disclosure stating that app schedules run on this phone and only take effect if, at the scheduled time, the phone is switched on and can reach the unit. The disclosure SHALL be visible whether or not any schedules exist.

#### Scenario: Disclosure with no schedules

- **WHEN** the user opens the Schedules page and has no schedules
- **THEN** the disclosure is shown alongside the empty state

#### Scenario: Disclosure with schedules

- **WHEN** the user opens the Schedules page and has one or more schedules
- **THEN** the disclosure is shown on the page without needing to open a schedule

### Requirement: Pointer to the unit timer

When the unit supports its own timer, the disclosure SHALL point the user to the unit timer as the option that works while the phone is off.

#### Scenario: Unit supports a timer

- **WHEN** the Schedules page is shown and the unit reports timer support
- **THEN** the disclosure mentions the unit timer as the alternative that runs without the phone

#### Scenario: Unit does not support a timer

- **WHEN** the Schedules page is shown and the unit does not report timer support
- **THEN** the disclosure is shown without mentioning the unit timer
