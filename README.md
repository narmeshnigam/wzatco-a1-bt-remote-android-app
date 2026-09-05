# WZATCO A1 Remote

An Android app that replaces the lost physical remote of a **WZATCO Alpha A1** projector by making the phone a Bluetooth HID device. Nothing is installed on the projector.

- **Platform:** Android, Kotlin, Jetpack Compose, minSdk 28 (`BluetoothHidDevice` requires API 28), targetSdk 35
- **Transport:** Bluetooth HID Device profile — keyboard, consumer-control and mouse reports
- **Target host:** WZATCO Alpha A1, Android 9, HiSilicon 352

## Repository map

| Path | What it is |
| --- | --- |
| `CLAUDE.md` | Operating instructions for the coding agent. Read first. |
| `AGENT_TASKS.md` | The ordered build plan: five gates, each with an exit test. |
| `docs/BUILD_SPEC.md` | The functional and architectural specification. Source of truth. |
| `docs/DESIGN_SPEC.md` | Visual and interaction spec: exact colors, dp sizes, screen layouts. |
| `docs/KEY_LAB.md` | Key map, candidate lists, discovery protocol, findings schema. |
| `docs/TEST_PLAN.md` | Unit, instrumented and manual test requirements. |
| `docs/OPEN_QUESTIONS.md` | Hardware unknowns. Answer from the device; never design around a guess. |
| `docs/design/` | The design artifacts as HTML — open in a browser to see the intended UI. |
| `docs/HARDWARE_RUNBOOK.md` | The running order for the hardware test session, worst-consequence first. |

## Status

**Gate 1 built, exit test not yet run.** The Gradle project, the HID report descriptor and its
report builders, `HidService`, the permission rationale flow and a one-button debug screen are in
place, and `./gradlew ktlintCheck detekt test lintDebug assembleDebug` is clean with 33 unit tests
passing. What Gate 1 actually proves can only be proved on the hardware — see
`docs/HARDWARE_RUNBOOK.md`. Gate 2 does not start until it passes.

## Build

```bash
./gradlew ktlintCheck detekt test assembleDebug
```

Requires JDK 17+ and an Android SDK with platform 36 installed. `local.properties` points at the
SDK and is not committed.

## The one hard constraint

Gate 1 proves the phone's ROM permits `BluetoothHidDevice.registerApp()`. Some OEM builds restrict the HID Device profile. If that gate fails, no application code can fix it — the phone must change. Do not build UI before Gate 1 passes.
