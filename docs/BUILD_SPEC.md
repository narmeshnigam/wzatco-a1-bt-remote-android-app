# Build spec — WZATCO A1 Remote

Version 1.0 · 06 Sep 2026 · derived from *WZATCO A1 Bluetooth Remote Replacement — Technical Blueprint* (05 Sep 2026).

## 1 · Mission

Build a Kotlin Android application named **WZATCO A1 Remote** that turns the phone into a Bluetooth HID device and drives a WZATCO Alpha A1 projector with no software installed on the projector. Twelve functions must reach one screen: D-pad with OK, Back, Home, Menu, Volume up/down, Mute, Power, Focus +/−, Source, Screen flip, Keystone, plus a cursor mode.

Eight of those map to standard HID usages and can be finished without the projector. Five are projector-specific and unknown; the app ships a discovery tool (Key Lab) that resolves them against the real hardware and writes the result back into its own key map.

**Definition of done.** The app installs on the phone, registers as a Bluetooth HID device, pairs with the A1 from the projector's own Bluetooth settings, and drives navigation, OK, Back, Home, Menu, volume and mute reliably from a cold start. Key Lab produces a JSON findings file for every unresolved function. Unit and instrumented suites pass, `./gradlew build` is clean, and the manual test matrix is filled in.

## 2 · Stack and constraints

- Kotlin, Jetpack Compose, single module `:app`. Extract `:hid` only if it earns its own tests.
- **minSdk 28** — `BluetoothHidDevice` arrived in Android 9 (API 28) and is the whole basis of the design. targetSdk 35.
- No third-party Bluetooth libraries. Platform APIs only, so nothing hides a failure mode.
- Persistence: DataStore for the key map and Key Lab findings. No database.
- No projector-side APK and no HomeMate IR dependency in v1. Both stay documented fallbacks (§11).
- Offline. No `INTERNET` permission at all.

## 3 · Architecture

| Component | Responsibility |
| --- | --- |
| `HidService` | Foreground service. Owns the `BluetoothHidDevice` proxy, registration, the single host connection and report transmission. Survives screen-off; stops on explicit disconnect. |
| `HidDescriptor` | The report descriptor byte array and the report builders (§4). Pure Kotlin, fully unit-testable, no Android imports. |
| `KeyMap` | Maps a `RemoteFunction` enum to a report. Ships with the defaults in `KEY_LAB.md`, is overridden by Key Lab findings, and reloads at runtime without a restart. |
| `RemoteViewModel` | Connection state, press handling, auto-repeat, haptics, wire log. One state flow per screen. |
| `KeyLab` | Candidate lists per unresolved function, send/verdict loop, JSON export, promotion of a confirmed candidate into KeyMap. |
| `ui` | Four Compose screens: Keypad, Cursor, Key Lab, Setup. No Material theming — the visual spec is explicit. |

## 4 · HID report descriptor

One descriptor, three top-level collections, distinct report IDs so a keyboard press, a consumer-control press and a cursor move never collide.

- **Report 1 — Keyboard** (Generic Desktop / Keyboard): 1 modifier byte + 1 reserved + 6 key slots. Arrows, Enter, Escape, Application, F-key candidates.
- **Report 2 — Consumer Control**: one 16-bit usage field. Volume, mute, power, AC_Back, AC_Home, zoom candidates.
- **Report 3 — Mouse**: 3 buttons + relative X/Y (8-bit signed) + wheel. Cursor mode only.

Register with `BluetoothHidDeviceAppSdpSettings`: name `WZATCO A1 Remote`, description `Projector remote`, provider `narmeshnigam`, subclass `SUBCLASS1_COMBO`.

Every key press is two reports — a key-down and a matching all-zero key-up. The up report is guaranteed even if the finger leaves the button, the gesture is cancelled, or the composable leaves the tree.

**Gate 1 comes before any UI work.** Prove on the actual phone that `registerApp()` returns true and a paired host receives an arrow-key report. Some OEM ROMs restrict the HID Device profile; if the phone fails this gate, no application code fixes it and the phone must change.

## 5 · Interaction rules

The remote is used in a dark room, one-handed, without looking at the phone. These are requirements, not preferences.

- Minimum touch target 58 × 58 dp; 6 dp gutters. No key smaller than any other in its row.
- Fixed geometry. The keypad never scrolls and rows never reflow — position is the only cue a thumb has.
- Press feedback: the cell fills with the accent for 90 ms; a 12 ms haptic tick fires on key-**down**, not key-up.
- Auto-repeat on D-pad, Volume and Focus: 400 ms delay, then every 90 ms while held. An unverified key never repeats.
- Power, once confirmed, requires a 600 ms long-press and a confirming double haptic. While it is unverified a plain tap routes to Fix Keys — there is nothing to fire.
- **Unverified keys route, they do not send.** A key whose code is not yet confirmed on the A1 (Focus ±, Source, Flip, Keystone and Power at the time of writing) is drawn unverified with a SET UP caption, stays tappable even with no host, and its press opens Fix Keys with that function preselected instead of transmitting a report the projector ignores. Once confirmed it behaves like any other key, with no rebuild.
- Screen stays awake while the remote screen is foreground. No lock-screen presence in v1.
- On host disconnect, confirmed keys grey out immediately and the status line says so. Never queue reports for a dead link. Unverified keys stay live: a route to Fix Keys is not a transmission.

## 6 · Screens

Layout, hierarchy and every value are in `DESIGN_SPEC.md`.

| Screen | Contents |
| --- | --- |
| Keypad | Status line with host name and link state, power key top-right; 3 × 3 D-pad block with OK filled; four rows of three — Back / Home / Menu, Vol − / Mute / Vol +, Focus − / Focus + / Source, Flip / Keystone / Trackpad. |
| Trackpad | Full-height drag surface sending relative mouse deltas at 1.6× acceleration; tap = left click; two-finger tap = Back. A collapsible scroll strip down the right edge sends wheel motion. Below, one compact row: Left click / Double click / Back. Return to the keypad is via the tab bar. |
| Fix Keys | The user-facing name of Key Lab. A button picker across the top; the function under test with its current code and index; a running verdict list; modes Suggested / Type code / Auto-scan; actions Send (primary), It worked, No effect, Did something else. JSON export. Tapping an unverified keypad key opens it with that function preselected. |
| Setup | A three-step stepper — 1 Register (HID registration plus the phone's own Bluetooth On / Off / Restart), 2 Pair (make discoverable, refresh, tap-to-connect bond list), 3 Connect (link status, the standby test, the connect/disconnect primary action). |

## 7 · Permissions and lifecycle

- `BLUETOOTH_CONNECT` and `BLUETOOTH_ADVERTISE` (API 31+), legacy `BLUETOOTH` / `BLUETOOTH_ADMIN` for 28–30, `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `VIBRATE`. No location permission, no `INTERNET`.
- Ask from a rationale screen, never on cold launch. A denial leaves the app usable and honest about what is blocked.
- Unregister the HID app and release the proxy in `onDestroy`; re-register idempotently on next start. Never leak a registration across process death.
- Reconnect: on host disconnect, retry the last known host three times with backoff, then wait for a manual connect.
- Remembered host: the address of the last host that actually reached the connected state is kept in the `settings` DataStore. On service start, once the HID app is registered, the service makes one automatic connect attempt to it if it is still bonded — one attempt per registration (so once more after a Bluetooth restart), never a loop.
- Bluetooth cycling: the service listens for the adapter state. Off tears the link down honestly (stage `BLUETOOTH_OFF`); on registers again without a tap. When registration is refused ten times the message names the likely cause — a registration the stack still holds for a dead process (open question 17) — and the fix, a Bluetooth restart.
- Phone Bluetooth: Setup offers On / Off / Restart. On goes through the system consent dialog (`ACTION_REQUEST_ENABLE`). Off and Restart try the deprecated direct calls and, when the OS refuses them (expected on stock API 33+), open the system Bluetooth settings instead. The app never claims to have toggled a radio it did not (open question 16).
- Portrait only: `MainActivity` declares `screenOrientation="portrait"`. The app is a remote held in one hand.

## 8 · Tests

See `TEST_PLAN.md`. Verification per change:

```bash
./gradlew ktlintCheck detekt test connectedAndroidTest assembleDebug
```

## 9 · Fallbacks — not in v1

If a function survives Key Lab unmapped it is vendor-internal, and only then do these open up: a projector-side companion APK driving the vendor service or Android UI automation, or the existing HomeMate IR blaster for that one function. Do not build either speculatively and do not make HomeMate a dependency. Record the unmapped function in the findings file and stop.

## 10 · Open questions

Tracked in `OPEN_QUESTIONS.md`. Answer them from the hardware; do not design around a guess.
