# WZATCO A1 Remote

An Android app that replaces the lost physical remote of a **WZATCO Alpha A1** projector by making the phone a Bluetooth HID device. Nothing is installed on the projector.

- **Platform:** Android, Kotlin, Jetpack Compose, minSdk 28 (`BluetoothHidDevice` requires API 28), targetSdk 35, compileSdk 36
- **Transport:** Bluetooth HID Device profile — keyboard, consumer-control and mouse reports
- **Target host:** WZATCO Alpha A1, Android 9, HiSilicon 352

## Status

**All five gates are built. None of the hardware questions is answered yet.**

The app compiles clean, 130 unit tests and 13 instrumented tests pass, and every screen has
been run and checked against `docs/DESIGN_SPEC.md` on an emulator. What an emulator cannot
test is the only thing that ultimately matters: whether this phone's ROM permits the HID
Device profile, and whether the projector accepts the phone as a remote.

Run `docs/HARDWARE_RUNBOOK.md` next. It is ordered worst-consequence first.

### What is known to work

Verified in code and on an emulator running stock Android 15:

- `registerApp()` returns true and `onAppStatusChanged(registered=true)` fires, so the
  registration path itself is correct. **This says nothing about OxygenOS**, which is the
  actual question.
- The report descriptor parses, the report builders produce the exact bytes `docs/KEY_LAB.md`
  specifies, and every press is followed by its release even when the key-down fails or throws.
- The keypad, cursor, Key Lab and setup screens render to spec, keys meet the 58 dp minimum,
  and nothing is transmitted while the link is down.

### What is not known

Nothing about the projector. Thirteen open questions in `docs/OPEN_QUESTIONS.md`, none
answered. In particular:

- Whether the OnePlus ROM permits `registerApp()` at all (question 1 — the whole architecture
  rests on it).
- Whether the A1 will pair with an input device rather than only with audio devices (7).
- Whether the A1 acts on mouse reports at all (9).
- What Focus ±, Source, Screen flip and Keystone actually are (4, 12, 13).

### What deliberately does not work yet

- **Screen flip** and **Keystone** transmit nothing. They have no mapping, and a nearby
  function would be worse than none. They are drawn dashed and stay that way until Key Lab
  finds them on real hardware.
- **Focus ±**, **Source** and **Power** ship with a guess as their first Key Lab candidate.
  They are drawn dashed too. A dashed key is the app saying it has not proved this.
- **Power-on** is not offered at all. Until question 2 is answered, the app must not present
  an affordance it cannot honour.

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
| `docs/HARDWARE_RUNBOOK.md` | The running order for the hardware test session. |
| `docs/design/` | The design artifacts as HTML — open in a browser to see the intended UI. |

## Architecture

| Package | Responsibility |
| --- | --- |
| `hid/` | Report descriptor, report builders, key press pairing, cursor motion, candidate lists. Pure Kotlin, **zero Android imports**, so all of it is unit-testable. |
| `service/` | `HidService`: the foreground service owning the `BluetoothHidDevice` proxy, registration, the host connection and transmission. `HidLink`: the process-wide view of it. |
| `data/` | DataStore-backed key map overrides and Key Lab findings, behind interfaces so tests need no device. |
| `vm/` | View models, auto-repeat timing, the Key Lab state machine. |
| `ui/` | Compose screens against an explicit local theme. No Material theming. |

## Build

```bash
./gradlew ktlintCheck detekt test assembleDebug
./gradlew connectedAndroidTest        # with a device or emulator attached
```

Requires JDK 17+ and an Android SDK with platform 36. `local.properties` points at the SDK
and is not committed.

### Release build

Signing material is read from `keystore.properties` at the repository root, which is **not
committed**. Without it the release build still assembles, unsigned, so a fresh clone is not
broken. With it:

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Back up `a1remote-release.jks` and `keystore.properties` somewhere outside this repository.
If they are lost, a new release build cannot be installed over an existing one — the app has
to be uninstalled first, taking its key map and findings with it.

## The one hard constraint

Gate 1 proves the phone's ROM permits `BluetoothHidDevice.registerApp()`. Some OEM builds
restrict the HID Device profile. If that gate fails on the OnePlus, no application code fixes
it — the phone must change, or the fallbacks in `BUILD_SPEC.md` §9 open up.
