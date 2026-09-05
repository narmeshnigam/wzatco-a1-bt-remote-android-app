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

## Status

Specification and design complete. No app code yet. Start at `AGENT_TASKS.md` Gate 1.

## The one hard constraint

Gate 1 proves the phone's ROM permits `BluetoothHidDevice.registerApp()`. Some OEM builds restrict the HID Device profile. If that gate fails, no application code can fix it — the phone must change. Do not build UI before Gate 1 passes.
