# Tasks

## 1. Cloud endpoint and auth parameters

- [ ] 1.1 Introduce an endpoint abstraction (base URL + optional auth query map) used by `AirconApiServiceImpl`, with a central definition of the cloud host `https://dascontroller.com` and the constants `device=A`, `port=30050`; verify via a unit test asserting the built URL for each op (local vs cloud) is exactly as expected.
- [ ] 1.2 Add a query-building helper that appends `id`, `spw`, `device`, `port` to both no-query GETs (`get_control_info`, `get_zone_setting`, `get_sensor_info`) and already-queried writes (`set_control_info`, `set_zone_setting`) without producing a malformed `?...?...`; verify with a unit test covering both cases and URL-encoding of existing zone params.

## 2. Cloud credential enrolment and storage

- [ ] 2.1 Add SharedPreferences keys and `NetworkModule` getters/setters for the cloud `id` and `spw`, plus a `clearRemoteCredential()` that removes them; verify by a test (or Robolectric) that set/get/clear round-trips and that `hasRemoteCredential()` reflects it.
- [ ] 2.2 Implement enrolment: read `id` and `pw` from the adapter's `basic_info` over the LAN, call `GET /skyfi/common/get_spw?id=&apw=<pw>`, parse `ret`/`spw`, and store `id`+`spw` on `ret=OK`; verify with a unit test over captured `get_spw` response strings (OK and non-OK) that the correct credential is stored or rejected. (basic_info parsing already exists in `DaikinDiscovery`/reuse.)
- [ ] 2.3 Enforce that enrolment requires local reachability and returns a clear "connect to home network" outcome when the adapter is not found; verify with a unit test of the enrolment result when the LAN read fails.

## 3. Transport selection and fallback

- [ ] 3.1 Extend the failure boundary (the `ConnectException`→`ConnectionException` path and `healConnection`) so that, when local is unreachable and a cloud credential exists, the same operation is retried over the cloud endpoint; when no credential exists, the existing `ConnectionException` is surfaced unchanged; verify with unit tests for all three branches (local ok, remote fallback, no-credential error).
- [ ] 3.2 Handle `ret=ACCOUNT NG` from a cloud call: if local reachable, re-run enrolment and retry once; if not, return a "re-enrol from home" outcome and do not loop; verify with a unit test driving a stale-credential response in both reachability states.
- [ ] 3.3 Ensure the remote write path does not assert read-back equality (cached-state tolerance) so a delayed poll is not treated as failure; verify with a unit test that a post-write read showing prior state is not reported as an error.

## 4. Connection-mode surfacing

- [ ] 4.1 Expose current transport mode (local/remote) as observable state from `NetworkModule`/service and consume it in `MainViewModel`; verify by a unit/ViewModel test that the mode flips to remote when the cloud path is used and back to local otherwise.
- [ ] 4.2 Add a remote-mode indicator in the relevant screen(s) and a Settings action to enable remote access (enrol) and to clear the stored credential; verify by building the app and exercising the toggle, confirming the indicator appears in remote mode and clearing disables cloud fallback.

## 5. Confidentiality

- [ ] 5.1 Audit the cloud code paths to ensure request URLs and the `spw`/`pw`/`id` values are never logged (no Ktor request logging on the cloud path, no `println`/Log of URLs); verify by grepping the diff for logging of those identifiers and by a code review checklist item in the change.

## 6. Integration verification

- [ ] 6.1 Run `./gradlew testDebugUnitTest` and confirm the full suite (including the new tests) passes.
- [ ] 6.2 With the unit OFF (per project safety rule — never turn the aircon on in tests), verify end-to-end against the real relay from an off-LAN network: a cloud read returns current state, and a single harmless write (e.g. fan speed) is accepted with `ret=OK`; record the observed write-confirmation latency. (This resolves the design Open Question about the write payload.)

## Workflow follow-up

- Archive the change once implemented and verified.
