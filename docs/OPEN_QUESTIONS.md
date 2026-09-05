# Open questions

Answer these from the hardware. Record the answer and the date. Do not design around a guess, and do not delete a question — mark it answered.

| # | Question | Why it matters | Answer | Date |
| --- | --- | --- | --- | --- |
| 1 | Does the phone's ROM permit `BluetoothHidDevice.registerApp()`? | If not, the entire architecture is void on this phone. Gate 1. | **Yes.** `getProfileProxy(HID_DEVICE)` returned true, the proxy connected, and `onAppStatusChanged` reported `registered=true`. `HidDeviceService` is active in `dumpsys bluetooth_manager`. **But `registerApp()` itself returned `false`** — see question 14. | 2026-09-06 |
| 2 | Does the A1 keep its Bluetooth host alive in standby? | Decides whether Bluetooth can power the projector **on** at all, or whether that stays IR / physical button. | | |
| 3 | Does Consumer `0x0030` Power turn the A1 off, or only sleep the Android layer? | Changes what the power key can honestly promise. | | |
| 4 | Are Focus, Source, Flip and Keystone reachable as input events at all? | If a vendor service above the input stack owns them, no HID usage will ever reach them and the fallback path opens. | | |
| 5 | Does the A1 remember the pairing across a full power cycle? | Decides whether Setup is a one-time flow or a recurring chore. | | |
| 6 | What is the phone model and Android version used for the build? | Needed for the findings file and for reproducing any ROM-specific behaviour. | OnePlus DN2101 (Nord 2 5G), Android 13, SDK 33, OxygenOS build `DN2101_11_F.59`. | 2026-09-06 |
| 7 | Does the A1's own Bluetooth settings screen offer pairing with an input device at all, or only with audio devices? | The phone cannot initiate the HID connection until the projector has bonded with it. If the A1 only pairs speakers and headsets, Gate 1 cannot pass however well the phone registers. | **Partly.** Its paired list holds an input device (`RGE_B_DF0415`, shown with the input-device icon), so the A1 does bond input devices. But its scan never lists the phone even while the phone is discoverable — it filters phone-class devices. The bond has to be made from the phone side (runbook §1.5). Whether the A1 then accepts the HID control channel is still open. | 2026-09-06 |
| 8 | Does OxygenOS keep `HidService` alive with battery optimisation disabled, or does it stop the service anyway? | Decides whether the remote survives a screen-off, and whether Gate 3's reconnect logic is a convenience or the only thing that makes the app usable. | | |
| 9 | Does the A1 act on the mouse collection at all — does a drag move a pointer on the projected image, and does a left click activate what is under it? | The cursor screen is the only way to reach anything the D-pad cannot focus. If the ROM ignores report 3, the screen is dead weight and its keys must be drawn unverified. | | |
| 10 | At 1.6× acceleration, does one full-length drag across the phone cross the projected image? | Decides whether the acceleration factor of BUILD_SPEC §6 is usable or has to change. It depends on the A1's pointer resolution and its own pointer acceleration, neither of which is known. | | |
| 11 | Does the A1 act on every press in a 700 ms sweep, or does it coalesce or debounce presses at that rate? | Until this is known a silent sweep is not evidence of anything. If the projector drops presses at that cadence, "no effect" against sixteen swept usages means only that the sweep was too fast. Gate 4. | | |
| 12 | Do keyboard usages `0x68`–`0x73` (F13–F24) reach the A1's applications at all? | Android's own key layouts decide this before any projector code sees the key. If the platform drops F13–F24, the screen-flip sweep tests the ROM's key layout rather than the projector, and a null result says nothing about the vendor keys. Gate 4. | | |
| 14 | Why does `registerApp()` return `false` on OxygenOS `DN2101_11_F.59` while `onAppStatusChanged` reports `registered=true` and the profile works? | The return value is a binder result and this build reports it unreliably. The callback is the authoritative signal, so the app must not treat a `false` return as a refusal on its own. Answered enough to act on; the underlying cause is not known. | Observed 2026-09-06. Treated as: the callback decides. | 2026-09-06 |
| 15 | Does the A1 keep a bond that the phone initiated, given its settings screen never lists the phone? | The phone's Bluetooth log shows a bond with the projector (`BC:6B:FF:41:5A:F9`) at 02:49 on 2026-09-06 that was gone by 03:15, and the projector's paired list did not show the phone. If the projector silently drops bonds from devices it has no profile for, every HID connect will fail with a missing-key error and Setup needs a re-pair step. | | |
| 13 | What, if anything, are consumer usages `0x0180`–`0x018F` on this host? | `KEY_LAB.md` names no usages for this range, so Key Lab names its sweep entries by code alone. If the sweep hits one, the findings file records the number and not a name — the name has to come from the host, not from the app. Gate 4. | | |

**Until question 2 is answered, the app must not present a power-on affordance it cannot honour.**

## How to answer 2 and 3 without guessing

With the phone paired and the projector awake: send Power, note what the projector does and whether the HID link stays connected. Then, from standby, send any key and watch for a reaction. If the link drops the moment the projector sleeps, Bluetooth wake is impossible and question 2 is answered no.

## How to answer 1, 7 and 8

Run `HARDWARE_RUNBOOK.md` §1. It records what to look for and where.

## How to answer 9 and 10

With the phone paired and the projector awake, open the Cursor tab and drag slowly from one edge of the surface to the other. Note whether a pointer appears on the projected image, how far it travelled, and whether it kept up with the thumb. Then tap once over a focusable item and see whether it activates, and put two fingers down and lift them to see whether the projector goes back. Until this is done, nothing in the app may claim the A1 accepts a mouse.

## How to answer 11 and 12 before trusting a sweep

Both are about whether a silent sweep means anything, so answer them before running one for real.

**11.** With the projector on a screen where the volume bar is visible, open Key Lab, switch to manual mode, choose `consumer`, and type `00E9` (Volume +). Press **Send** sixteen times at roughly the sweep's own pace — one press every 700 ms, counted out. If the volume rises sixteen steps, the projector acts on every press at that cadence and a silent sweep is real evidence. If it rises fewer, the sweep is too fast; record the number of steps that actually landed.

**12.** With the projector on any screen that shows a text field, in Key Lab manual mode choose `keyboard` and send `04` (the letter A) to prove the keyboard collection reaches the host at all. Then send `3A` (F1) and `68` (F13) and watch for anything — a focus move, a toast, a beep. If A types and F13 does nothing, F13–F24 are being dropped somewhere above the wire, and the screen-flip sweep result is a fact about Android rather than about the A1.

Record both answers here before filing a findings file that leans on them.
