# Design

## Context

See proposal.md — Why. Today `AirconApiServiceImpl(httpClient, baseUrl)` builds every request as `"$baseUrl/skyfi/aircon/<op>"`, where `baseUrl` is `http://<adapter-ip>` from SharedPreferences (`NetworkModule`). `DaikinDiscovery` finds the adapter by UDP broadcast; `NetworkModule.healConnection()` rediscovers on failure; a `java.net.ConnectException` becomes a `ConnectionException` surfaced to the UI. The cloud relay reuses the **exact same `/skyfi/aircon/*` paths and key=value format** — the only differences are host (`https://dascontroller.com`), four extra query params (`id`, `spw`, `device=A`, `port=30050`), and a one-time login to obtain `spw`. This keeps the change additive.

## Goals / Non-Goals

**Goals:**
- Reuse the existing request-building and parsing code for both transports.
- Make fallback automatic and invisible in the normal case (home → away).
- Keep the cloud credential on-device and out of logs.

**Non-Goals:**
- Pairing/enrolling a brand-new account from scratch (`create_onetime_key_ex` → `add_account_ex`). We rely on the unit already being registered for out-of-home use, which it is. Enrolment here means only deriving `spw` for our app.
- Multi-unit / multi-adapter support (app already assumes a single unit; cloud uses `device=A`).
- Changing or specifying the existing LAN behaviour beyond adding the fallback branch.

## Decisions

- **Transport selection by base URL, not a new API type.** Keep one `AirconApiService` shape. Introduce a small notion of "endpoint" = (base URL, optional auth query). Local endpoint = `http://<ip>` with no auth; cloud endpoint = `https://dascontroller.com` with `id/spw/device/port`. `AirconApiServiceImpl` appends the auth query to each request when present. Alternative considered: a separate `CloudAirconApiService` — rejected as duplicating all the parsing/encoding (zones especially) for no behavioural difference.
  - Append auth params correctly to calls that already carry a query string (`set_control_info`, `set_zone_setting`) and to those that do not (the GETs). Build the query with a single helper rather than string concatenation to avoid a malformed `?a=b?c=d`.
- **Fallback lives where failure is already handled.** The LAN path already self-heals in `NetworkModule.healConnection()` / the `ConnectException` catch. Extend that boundary: on local unreachable, if a cloud credential exists, retry the same operation against the cloud endpoint; otherwise surface `ConnectionException` as today. This keeps callers (`MainViewModel`) largely unchanged.
- **Credential model.** Store `id` + `spw` in SharedPreferences (new keys). Derive `spw` at enrolment via `get_spw?id=&apw=<adapter pw>`. On any cloud `ret=ACCOUNT NG`, treat `spw` as stale: if local is reachable, re-derive and retry once; if not, mark remote as needing re-enrolment. We store `spw` (not the adapter `pw`) as the working credential, but enrolment always goes back through `pw`→`get_spw`, so a rotated `spw` self-heals when home.
- **Mode reporting.** Expose current transport (local/remote) as observable state from `NetworkModule`/the service so `MainViewModel` can show a remote indicator. Writes over cloud confirm after the adapter's ~60 s poll; the app must not treat a read that still shows the old state as a failed write (no aggressive read-back assertion on the remote path).

## Risks / Trade-offs

- **Undocumented third-party dependency** → The relay (`dascontroller.com`, region `au`) and its protocol can change without notice. Mitigation: isolate the host/paths/params in one place; fail gracefully to the existing local error; never block local control on cloud availability.
- **Credential in query string** → `spw`/`id`/`pw` travel in HTTPS URLs and could leak via logs. Mitigation: no logging of request URLs on the cloud path; store credential in SharedPreferences only; provide a user "clear" action.
- **Stale-read after remote write looks like a bug** → Users may think a write failed because the next read is unchanged for up to a minute. Mitigation: surface remote mode and optimistic UI state; avoid read-back failure assertions remotely.
- **Enrolment requires being home at least once** → Remote cannot be set up purely from away. Accepted: matches the official app, which also derives server state while adjacent to the unit.
- **`device=A` / `port=30050` assumptions** → Correct for this single-unit AirBase; other setups may differ. Accepted given single-unit non-goal; values are centralised so they can be revisited.

## Confidentiality review checklist (task 5.1)

Reviewers of the cloud path confirm each of these:

- [ ] No Ktor `Logging` plugin is installed on the client used for cloud calls (request URLs carry `spw`/`id`).
- [ ] No `Log.*` / `println` of a request URL or of the `spw`, adapter `pw`, or `id` anywhere on the cloud/enrolment path.
- [ ] Cloud transport errors are rethrown as a fixed-message `ConnectionException` with no `cause`, so the URL (and the credential in it) cannot reach the UI (`MainViewModel` puts `e.message` into `_error`) or a worker's `Log.e(..., e.message)`.
- [ ] Enrolment network reads swallow their exceptions (which can embed the URL and `pw`) without logging.
- [ ] The stored credential never leaves the device. The credential is kept in its own prefs file (`aircon_remote_creds`) excluded from Auto Backup and device-transfer via `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`; other settings in `aircon_app_prefs` still back up.

## Open Questions

- ~~Does any cloud write require a parameter the LAN write omits?~~ **Resolved (2026-11-27):** a cloud `set_control_info` carrying the same fields as the LAN write (`pow,mode,stemp,f_rate,f_dir,f_airside,f_auto`), sent as a GET with `id,spw,device=A,port=30050`, returned `ret=OK` and was applied by the adapter after ~62 s (the ~60 s poll lag). No extra payload is needed.
