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
- No confirmed key dispatches while disconnected; unverified keys stay live, because their press is a route to Fix Keys, not a report.
- Screen stays awake on the remote screen and releases on background.

Running it on the DN2101 (OxygenOS 13, Android 13):

- The ROM refuses runtime grants from the shell — `pm grant` and `UiAutomation.grantRuntimePermission` both fail with "Neither user 2000 nor current process has GRANT_RUNTIME_PERMISSIONS" — but honours grants made at install time. Every Gradle install therefore carries `-g` (`installation.installOptions` in `app/build.gradle.kts`), and `BluetoothPermissionsRule` grants only what the app does not already hold. If an activity test still reports "Could not grant", allow *Nearby devices* for the app by hand, or switch on the developer option that unblocks shell grants (*Disable permission monitoring* on OxygenOS/ColorOS; not yet tried on this phone) and re-run.
- By default the connected test task uninstalls the app when it finishes, which wipes the Fix Keys findings and the remembered host. `gradle.properties` sets `android.injected.androidTest.leaveApksInstalledAfterRun=true` so the app stays installed and a re-install over the top keeps its data. On a machine without that property, export the findings (Fix Keys → Export JSON) before running the suite.

## Fake host

A test double for `BluetoothHidDevice` that captures reports, so the whole app is testable without the projector. Everything above except the registration tests must run against it.

## Manual matrix — against the A1

Functional walk on 2026-09-06 (link live, projector watched by eye and ear). This pass records
whether each function *reacts*; latency, auto-repeat, idle and standby endurance are a later,
timed pass and are marked "—" (not measured) rather than guessed. Full verbatim notes are in
`docs/OPEN_QUESTIONS.md` → "Keypad findings".

| Function | Reacts | Latency | Auto-repeat | After 10 min idle | After projector standby | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Up | Yes | — | — | — | — | |
| Down | Yes | — | — | — | — | |
| Left | Yes | — | — | — | — | |
| Right | Yes | — | — | — | — | |
| OK | Yes | — | — | — | — | |
| Back | Yes | — | — | — | — | |
| Home | Yes | — | — | — | — | |
| Menu | Partial | — | — | — | — | Single press: tone, no action. Double press toggles aspect 16:9 ⇄ 4:3 (projector's own binding). |
| Volume + | Yes | — | — | — | — | |
| Volume − | Yes | — | — | — | — | |
| Mute | Yes | — | — | — | — | |
| Power off | No | — | — | — | — | No reaction, not even the key-press tone. Consumer 0x0030 not acted on. Stays unverified. |
| Focus + | No | — | — | — | — | Key-press tone but no action. Reachable but unmapped; needs a Fix-Keys search. |
| Focus − | No | — | — | — | — | As Focus +. |
| Source | No | — | — | — | — | As Focus +. |
| Screen flip | No | — | — | — | — | Nothing at all, not even the tone. Usage never reaches the projector; fallback candidate. |
| Keystone | No | — | — | — | — | As Screen flip. |
| Cursor move | Yes | — | — | — | — | Pointer moves on the projected image. |
| Left click | Yes | — | — | — | — | Activates what is under the pointer. |

Latency in milliseconds, measured by eye against a repeated press — an estimate is fine, an
omission is not, on the timed pass. This first pass is functional only.
