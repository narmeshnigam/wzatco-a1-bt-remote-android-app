# Build plan

Five gates. Each has an exit test that must pass before the next begins. Report the result of every gate.

## Gate 1 — Prove the transport

- [ ] Gradle project, `:app` module, package `com.narmeshnigam.a1remote`, minSdk 28 / targetSdk 35, Compose enabled, ktlint + detekt wired.
- [ ] `hid/HidDescriptor.kt`: the three-collection report descriptor from `docs/BUILD_SPEC.md` §4, plus report builders. Pure Kotlin.
- [ ] Unit tests for the descriptor bytes and every report builder.
- [ ] `service/HidService.kt`: foreground service that acquires the `BluetoothHidDevice` proxy, registers the app with the SDP settings from the spec, and exposes `sendKey(function)`.
- [ ] A single debug screen with one button that sends Arrow Down.
- [ ] Permission rationale flow for `BLUETOOTH_CONNECT` / `BLUETOOTH_ADVERTISE`.

**Exit test:** on the real phone, `registerApp()` returns true, the projector pairs with the phone from its own Bluetooth settings, and the debug button moves the projector's on-screen focus. Record the phone model and Android version in `docs/OPEN_QUESTIONS.md`.

## Gate 2 — The keypad

- [ ] `data/KeyMap`: `RemoteFunction` enum → report, defaults from `docs/KEY_LAB.md`, DataStore-backed overrides, runtime reload.
- [ ] Keypad screen exactly per `docs/DESIGN_SPEC.md`: status line, power key, D-pad block, four rows of three.
- [ ] Setup screen: three numbered steps, connect/disconnect primary action, live link state.
- [ ] All eight confirmed functions wired: D-pad, OK, Back, Home, Menu, Volume ±, Mute.
- [ ] Disconnected state greys the keys and blocks transmission.

**Exit test:** the projector is fully navigable from the phone with no mouse attached.

## Gate 3 — Make it usable in the dark

- [ ] Auto-repeat: 400 ms delay then 90 ms interval on D-pad and Volume.
- [ ] Haptics on key-down (12 ms); power requires a 600 ms long-press with a double tick.
- [ ] Guaranteed key-up on finger-leave, gesture cancel and composable disposal.
- [ ] Cursor screen: relative mouse deltas at 1.6× acceleration, tap = left click, two-finger tap = Back.
- [ ] Keep-screen-on while the remote is foreground; reconnect with three backoff attempts.
- [ ] Instrumented tests: rotation, process death, touch-target sizes, no-send-while-disconnected.

**Exit test:** ten minutes of real use in a dark room with no misfires and no stuck keys.

## Gate 4 — Key Lab

- [ ] Key Lab screen: function under test, candidate index, verdict list, Send / It worked / No effect.
- [ ] Candidate lists per `docs/KEY_LAB.md`, plus manual-entry mode and sweep mode.
- [ ] A confirmed candidate is promoted into KeyMap immediately and the key's style flips to verified.
- [ ] Findings persist and export as JSON per the schema in `docs/KEY_LAB.md`.

**Exit test:** a findings JSON exists with a verdict for all five unknown functions, and every function that was resolved now works from the keypad.

## Gate 5 — Close it out

- [ ] Manual test matrix in `docs/TEST_PLAN.md` filled in against the A1.
- [ ] `docs/OPEN_QUESTIONS.md` has an answer or an explicit "still unknown" for every entry.
- [ ] Release build, signed, installed.
- [ ] README updated with what works, what does not, and why.

**Exit test:** the app is the daily driver for the projector.
