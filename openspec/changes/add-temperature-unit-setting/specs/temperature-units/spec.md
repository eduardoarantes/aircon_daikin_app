# Spec Delta

## Purpose

Defines how temperatures are shown to the user, including the Celsius/Fahrenheit preference, so display can change unit without affecting stored values, control payloads, or device limits.

## ADDED Requirements

### Requirement: Temperature unit preference

The app SHALL provide a temperature unit preference with values Celsius and Fahrenheit, defaulting to Celsius, persisted on the device, and settable from the Settings screen.

#### Scenario: Default on first use

- **WHEN** the app is opened with no unit preference saved
- **THEN** temperatures are shown in Celsius

#### Scenario: Changing the unit

- **WHEN** the user selects Fahrenheit in Settings and saves the changes
- **THEN** the preference is saved and temperatures across the app are shown in Fahrenheit until changed back

### Requirement: Display honours the selected unit

Every temperature shown to the user SHALL be displayed in the selected unit with the matching label (`°C` or `°F`): the Controls setpoint and indoor reading, the Schedules and Scenes lists, the Scene and Schedule editors, the Simple mode Set and Room readings, and the Settings simple-mode start temperature.

#### Scenario: Fahrenheit selected

- **WHEN** the unit is Fahrenheit and a temperature of 22 °C is shown anywhere in the app
- **THEN** it is displayed as 72 °F (22 °C → 71.6, rounded to the nearest whole degree) with a `°F` label

#### Scenario: Celsius selected

- **WHEN** the unit is Celsius
- **THEN** temperatures are displayed as whole Celsius degrees with a `°C` label, unchanged from prior behaviour

### Requirement: Conversion is display-only

The preference SHALL affect display only. Stored schedules and scenes, the simple-mode start temperature, control payloads sent to the adapter, and the device limits read from model-info SHALL remain in Celsius regardless of the preference.

#### Scenario: Sending a setpoint

- **WHEN** the user adjusts and applies a setpoint while Fahrenheit is selected
- **THEN** the value sent to the adapter is the whole-Celsius setpoint, not a Fahrenheit value

#### Scenario: Saved data is unit-independent

- **WHEN** the unit preference is changed after a schedule or scene was saved
- **THEN** the saved schedule or scene is unchanged and is simply displayed in the newly selected unit

### Requirement: Stepping stays in whole Celsius

The setpoint steppers and dial SHALL continue to change the value in whole-Celsius increments regardless of the selected unit; no sub-degree or Fahrenheit-granularity value is stored or sent.

#### Scenario: Stepping in Fahrenheit

- **WHEN** Fahrenheit is selected and the user taps the increase button once from 72 °F (22 °C)
- **THEN** the setpoint becomes 23 °C and is displayed as 73 °F, i.e. the underlying value moved by one Celsius degree

#### Scenario: Range limits unchanged

- **WHEN** the user steps toward a limit in either unit
- **THEN** the allowed range is the same Celsius range as today, expressed in the displayed unit

### Requirement: Unsaved settings confirmation

Temperature unit, theme, address, mock mode, Simple mode, wall-panel toggle, zone and start temperature SHALL remain draft settings until saved. Leaving Settings through its back arrow or Android Back with pending changes SHALL show a Save or Discard prompt. Explicit remote-access setup and kiosk actions retain their existing action buttons.

#### Scenario: Save pending changes

- **WHEN** the user changes the unit and chooses Save in the exit prompt
- **THEN** the settings are persisted and the app displays the saved unit

#### Scenario: Discard pending changes

- **WHEN** the user changes any draft setting and chooses Discard
- **THEN** the app exits Settings with the saved settings unchanged

#### Scenario: Continue editing

- **WHEN** the exit prompt is dismissed
- **THEN** Settings stays open with the pending edits intact

#### Scenario: Unchanged settings

- **WHEN** the user leaves Settings without changes, or after reverting edits to their saved values
- **THEN** the app exits without a confirmation prompt

#### Scenario: Invalid Simple mode draft

- **WHEN** Simple mode is enabled without a selected zone
- **THEN** Save is disabled in the exit prompt, and the user can discard or dismiss the prompt to select a zone
