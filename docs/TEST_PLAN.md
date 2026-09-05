# Test plan

## Unit — pure Kotlin, no device

- Descriptor bytes match the spec, collection by collection.
- Every `RemoteFunction` produces the exact expected report pair.
- A key-up always follows a key-down, including on cancel and disposal.
- Auto-repeat timing: first repeat at 400 ms, then every 90 ms; stops on release.
- Key map: default lookup, DataStore override, runtime reload, unknown function returns null rather than a wrong report.
- Key Lab: candidate advance on miss, promotion on hit, function advance, findings JSON round-trip.
- Mouse deltas: acceleration curve, 8-bit signed clamping.

## Instrumented — device required

- Service registers and unregisters cleanly across rotation and process death; no leaked registration.
- Permission grant and denial paths; denial leaves the app usable.
- Every key is reachable and at least 58 dp in both axes.
- No report is transmitted while disconnected.
- Screen stays awake on the remote screen and releases on background.

## Fake host

A test double for `BluetoothHidDevice` that captures reports, so the whole app is testable without the projector. Everything above except the registration tests must run against it.

## Manual matrix — against the A1

Fill this in at Gate 5. One row per function.

| Function | Reacts | Latency | Auto-repeat | After 10 min idle | After projector standby | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Up | | | | | | |
| Down | | | | | | |
| Left | | | | | | |
| Right | | | | | | |
| OK | | | | | | |
| Back | | | | | | |
| Home | | | | | | |
| Menu | | | | | | |
| Volume + | | | | | | |
| Volume − | | | | | | |
| Mute | | | | | | |
| Power off | | | | | | |
| Focus + | | | | | | |
| Focus − | | | | | | |
| Source | | | | | | |
| Screen flip | | | | | | |
| Keystone | | | | | | |
| Cursor move | | | | | | |
| Left click | | | | | | |

Latency in milliseconds, measured by eye against a repeated press — an estimate is fine, an omission is not.
