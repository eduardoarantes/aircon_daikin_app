# Spec Delta

## Purpose

Lets users read and control the aircon when the phone is not on the adapter's local network, by relaying the existing aircon API through Daikin's cloud and transparently choosing between the local and remote paths.

## ADDED Requirements

### Requirement: Cloud credential enrolment

The system SHALL obtain a cloud server password (`spw`) by reading the adapter's `id` and `pw` from its `basic_info` while on the local network, then requesting `GET /skyfi/common/get_spw?id=<id>&apw=<pw>` from the cloud relay. The resulting credential SHALL be stored on the device and used for subsequent cloud calls. Enrolment SHALL require local network access to the adapter.

#### Scenario: Successful enrolment on the local network

- **WHEN** the app is on the same network as the adapter and the user enables remote access
- **THEN** the app reads `id` and `pw` from `basic_info`, calls `get_spw` with `apw=<pw>`, receives `ret=OK` with an `spw` value, and stores the `id` and the credential for cloud use

#### Scenario: Enrolment attempted without local access

- **WHEN** the user enables remote access while the adapter is not reachable on the local network
- **THEN** the app does not obtain a credential and informs the user that enrolment must be done while connected to the home network

### Requirement: Cloud credential refresh

The system SHALL treat a cloud response of `ret=ACCOUNT NG` as an invalid or stale credential and SHALL re-run enrolment to obtain a fresh `spw` when local access is available, rather than surfacing the raw error.

#### Scenario: Stale credential during a cloud call

- **WHEN** a cloud call returns `ret=ACCOUNT NG` and the adapter is reachable locally
- **THEN** the app re-derives `spw` via `get_spw` and retries the call once with the refreshed credential

#### Scenario: Stale credential while away

- **WHEN** a cloud call returns `ret=ACCOUNT NG` and the adapter is not reachable locally
- **THEN** the app reports that remote access needs to be re-enrolled from the home network and does not loop retrying

### Requirement: Cloud transport for the aircon API

The system SHALL send the existing aircon operations (`get_control_info`, `set_control_info`, `get_zone_setting`, `set_zone_setting`, `get_sensor_info`) to the cloud relay over HTTPS at the same `/skyfi/aircon/<operation>` paths, with the authentication and target parameters `id`, `spw`, `device=A` and `port=30050` added to the request, and SHALL parse responses with the same key=value format as the local path.

#### Scenario: Reading state through the cloud

- **WHEN** a read operation is performed over the cloud transport with a valid credential
- **THEN** the app issues the request to `https://dascontroller.com/skyfi/aircon/<operation>` with `id`, `spw`, `device=A`, `port=30050` and returns the same parsed result it would from the local adapter

#### Scenario: Writing state through the cloud

- **WHEN** a write operation is performed over the cloud transport with a valid credential
- **THEN** the app sends the full control payload plus the authentication and target parameters and treats `ret=OK` as success

### Requirement: Transparent local-to-remote fallback

The system SHALL prefer the local adapter when it is reachable and SHALL route calls through the cloud transport when the adapter cannot be reached locally and a cloud credential is stored, instead of surfacing a connection error. When no cloud credential is stored, the existing local connection error SHALL be surfaced unchanged.

#### Scenario: Away from home with remote access enrolled

- **WHEN** an operation is requested, the local adapter is unreachable, and a cloud credential is stored
- **THEN** the app performs the operation over the cloud transport and returns the result without showing a connection error

#### Scenario: Away from home without remote access

- **WHEN** an operation is requested, the local adapter is unreachable, and no cloud credential is stored
- **THEN** the app surfaces the existing local connection error

#### Scenario: Local access available

- **WHEN** an operation is requested and the local adapter is reachable
- **THEN** the app uses the local path and does not contact the cloud relay

### Requirement: Connection mode is observable

The system SHALL expose whether the current connection is local or remote so the UI can indicate it, and remote writes MAY confirm after a delay because the cloud serves cached reads while writes are queued to the adapter's poll interval.

#### Scenario: UI reflects remote mode

- **WHEN** the app is operating over the cloud transport
- **THEN** the current connection mode is reported as remote to the UI

#### Scenario: Delayed confirmation of a remote write

- **WHEN** a write is sent over the cloud transport
- **THEN** a subsequent remote read MAY still reflect the prior state until the adapter next polls, and the app does not treat this transient mismatch as a failure

### Requirement: Credential confidentiality

The system SHALL store the cloud credential only on the device and SHALL NOT write the `spw`, adapter `pw`, or `id` to logs or any external destination other than the Daikin cloud relay. The user SHALL be able to clear the stored credential, which disables remote access.

#### Scenario: Credential is not logged

- **WHEN** cloud enrolment or a cloud call runs
- **THEN** no log output contains the `spw`, adapter `pw`, or `id`

#### Scenario: User clears remote access

- **WHEN** the user clears the stored credential in settings
- **THEN** the credential is removed from the device and subsequent unreachable-local operations surface the local connection error instead of using the cloud

### Requirement: Manual credential entry

The system SHALL allow the user to enrol remote access by entering their cloud account username (`id`) and password directly, deriving the server password (`spw`) via the relay's `get_spw`. Unlike automatic enrolment this SHALL NOT require local network access to the adapter. The entered password SHALL NOT be stored; only the username and the derived `spw` are retained.

#### Scenario: Manual enrolment away from home

- **WHEN** the user enters a valid username and password in the manual credential form and saves
- **THEN** the app derives `spw` via `get_spw`, stores the username and `spw`, reports that remote access is on, and does not store the entered password

#### Scenario: Manual entry is rejected

- **WHEN** the user enters a username/password that the relay does not accept
- **THEN** the app reports that the details could not be verified and no credential is stored
