# Operating instructions — WZATCO A1 Remote

You are building this app end to end. Read `docs/BUILD_SPEC.md` and `AGENT_TASKS.md` before writing code.

## What this app is

The phone registers as a Bluetooth HID device and drives a WZATCO Alpha A1 projector (Android 9, HiSilicon 352) whose physical remote is lost. Twelve functions must reach one screen. Eight map to standard HID usages and can be finished without the projector. Five are projector-specific and unknown; the app ships a discovery tool ("Key Lab") that resolves them against real hardware and writes the result into its own key map.

## Ground rules

1. **Follow the specs.** `docs/BUILD_SPEC.md` governs behaviour and architecture; `docs/DESIGN_SPEC.md` governs every visual and interaction value. Both are precise on purpose. If a spec is wrong, say so and propose a change — do not silently diverge.
2. **Work gate by gate.** `AGENT_TASKS.md` is ordered. Do not start a gate before the previous one's exit test passes. Report the outcome of each gate.
3. **Platform APIs only.** No third-party Bluetooth library. No Firebase, no analytics, no crash reporter, no ads.
4. **No network.** The manifest must not declare `INTERNET`. The app must build and run without it.
5. **Never invent hardware facts.** Anything you cannot verify goes in `docs/OPEN_QUESTIONS.md` with the question stated plainly. A guess presented as a finding is the worst possible failure here.
6. **Unverified functions stay visibly unverified.** Focus +/−, Source, Screen flip and Keystone render in the unverified style until Key Lab confirms them on the A1. Do not promote them by assumption.
7. **Test what you write.** Every pure-Kotlin unit (descriptor bytes, report builders, key map, repeat timing, Key Lab logic) gets a unit test in the same change.
8. **Keep the build clean.** Treat a new compiler or lint warning as a failure.

## Conventions

- Package: `com.narmeshnigam.a1remote`
- App label: `WZATCO A1 Remote`
- Single module `:app`. Extract `:hid` only when it has its own test suite worth isolating.
- Layers: `hid/` (descriptor + reports, pure Kotlin, zero Android imports), `service/` (foreground service, Bluetooth proxy), `data/` (DataStore key map + findings), `ui/` (Compose screens), `vm/` (view models).
- Kotlin coroutines + `StateFlow`. No RxJava, no LiveData.
- Compose with an explicit local theme object — do **not** use Material 3 default theming; the palette and metrics are fixed in `docs/DESIGN_SPEC.md`.
- Commit per gate, message prefixed `G1:`…`G5:`.

## Verify before you report a gate done

```bash
./gradlew ktlintCheck detekt test assembleDebug
./gradlew connectedAndroidTest   # when a device is attached
```

## When you are blocked by hardware

Several things can only be learned from the projector: whether Bluetooth survives standby, whether Consumer Power actually powers off, whether the projector-specific keys reach the input stack at all. When you hit one of these:

1. Stop. Do not implement a speculative workaround.
2. Write the question into `docs/OPEN_QUESTIONS.md`.
3. State exactly what the operator should do with the phone and projector to answer it.
4. Continue with work that does not depend on the answer.
