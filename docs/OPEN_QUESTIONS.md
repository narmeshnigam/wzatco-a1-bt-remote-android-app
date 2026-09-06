# Open questions

Answer these from the hardware. Record the answer and the date. Do not design around a guess, and do not delete a question — mark it answered.

| # | Question | Why it matters | Answer | Date |
| --- | --- | --- | --- | --- |
| 1 | Does the phone's ROM permit `BluetoothHidDevice.registerApp()`? | If not, the entire architecture is void on this phone. Gate 1. | **Yes — Gate 1 passes end to end.** The phone registers, the projector `NL_415AF8` connects, and 8-byte keyboard reports reach it (`hidd_conn_send_data: report sent`, id=1). The D-pad, cursor and clicks drive the projector's UI. `registerApp()` returns `false` on this ROM but the `onAppStatusChanged` callback reports `registered=true` — see question 14. | 2026-09-06 |
| 2 | Does the A1 keep its Bluetooth host alive in standby? | Decides whether Bluetooth can power the projector **on** at all, or whether that stays IR / physical button. | | |
| 3 | Does Consumer `0x0030` Power turn the A1 off, or only sleep the Android layer? | Changes what the power key can honestly promise. | **Neither — it does nothing.** Pressing Power on the app produces no reaction on the A1 at all: no power change, no sleep, and not even the projector's key-press tone. The Consumer Power usage `0x0030` this app sends is not acted on. Power off has to come from a different, still-unknown usage or stays an IR/physical-button function. The key stays unverified. | 2026-09-06 |
| 4 | Are Focus, Source, Flip and Keystone reachable as input events at all? | If a vendor service above the input stack owns them, no HID usage will ever reach them and the fallback path opens. | **Split.** With the link live, each key was pressed and the projector watched and listened to. **Focus − / Focus + / Source** each make the projector's key-press tone but do nothing on single, double or repeated presses — the usage reaches the A1's input stack but is not bound to focus or source (reachable-but-unmapped; a Fix-Keys usage search is warranted). **Flip / Keystone** produce nothing at all, not even the tone — the usages tried never reach the projector, so they are the first candidates for the projector-side fallback. See the keypad findings note below. | 2026-09-06 |
| 5 | Does the A1 remember the pairing across a full power cycle? | Decides whether Setup is a one-time flow or a recurring chore. | | |
| 6 | What is the phone model and Android version used for the build? | Needed for the findings file and for reproducing any ROM-specific behaviour. | OnePlus DN2101 (Nord 2 5G), Android 13, SDK 33, OxygenOS build `DN2101_11_F.59`. | 2026-09-06 |
| 7 | Does the A1's own Bluetooth settings screen offer pairing with an input device at all, or only with audio devices? | The phone cannot initiate the HID connection until the projector has bonded with it. | **Answered — the A1's Bluetooth screen is irrelevant to this app.** Its scan never lists the phone, and even while the HID link is live and driving the projector, the phone appears in neither *Available equipment* nor the paired list. That screen simply does not render HID hosts. The bond is made from the phone (Settings → Bluetooth → `NL_415AF8`) and the connection is initiated from the app's Setup tab; the projector accepts it. Do not use the projector's own Bluetooth UI to judge the connection. | 2026-09-06 |
| 8 | Does OxygenOS keep `HidService` alive with battery optimisation disabled, or does it stop the service anyway? | Decides whether the remote survives a screen-off, and whether Gate 3's reconnect logic is a convenience or the only thing that makes the app usable. | | |
| 9 | Does the A1 act on the mouse collection at all — does a drag move a pointer on the projected image, and does a left click activate what is under it? | The cursor screen is the only way to reach anything the D-pad cannot focus. | **Yes.** With the HID link live, the operator reported the cursor moved on the projected image and left clicks activated what was under the pointer. Report 3 (mouse) is acted on. | 2026-09-06 |
| 10 | At 1.6× acceleration, does one full-length drag across the phone cross the projected image? | Decides whether the acceleration factor of BUILD_SPEC §6 is usable or has to change. It depends on the A1's pointer resolution and its own pointer acceleration, neither of which is known. | | |
| 11 | Does the A1 act on every press in a 700 ms sweep, or does it coalesce or debounce presses at that rate? | Until this is known a silent sweep is not evidence of anything. If the projector drops presses at that cadence, "no effect" against sixteen swept usages means only that the sweep was too fast. Gate 4. | | |
| 12 | Do keyboard usages `0x68`–`0x73` (F13–F24) reach the A1's applications at all? | Android's own key layouts decide this before any projector code sees the key. If the platform drops F13–F24, the screen-flip sweep tests the ROM's key layout rather than the projector, and a null result says nothing about the vendor keys. Gate 4. | | |
| 14 | Why does `registerApp()` return `false` on OxygenOS `DN2101_11_F.59` while `onAppStatusChanged` reports `registered=true` and the profile works? | The return value is a binder result and this build reports it unreliably. The callback is the authoritative signal, so the app must not treat a `false` return as a refusal on its own. Answered enough to act on; the underlying cause is not known. | Observed 2026-09-06. Treated as: the callback decides. | 2026-09-06 |
| 15 | Does the A1 keep a bond that the phone initiated, given its settings screen never lists the phone? | If the projector silently drops the bond, every HID connect fails and Setup needs a re-pair step. | **The bond persists.** After pairing from the phone at 03:57 the bond survived repeated connects and Bluetooth cycles on the phone. The earlier bond that vanished (02:49→03:15) was to the wrong device (`BC8-Android`), not the projector. Whether the bond survives a projector power cycle is question 5, still open. | 2026-09-06 |
| 13 | What, if anything, are consumer usages `0x0180`–`0x018F` on this host? | `KEY_LAB.md` names no usages for this range, so Key Lab names its sweep entries by code alone. If the sweep hits one, the findings file records the number and not a name — the name has to come from the host, not from the app. Gate 4. | | |
| 16 | On OxygenOS `DN2101_11_F.59` (Android 13), do the Setup screen's **Off** and **Restart** buttons actually toggle the radio via `BluetoothAdapter.disable()/enable()`, or does the app fall back to opening the system Bluetooth settings? | The direct calls are deprecated on API 33 and documented to return `false` for a normal app, but some OEM ROMs still honour them. The Restart button is the surest fix for a stuck registration, so whether it works in one tap or needs the settings screen matters for the flow. | **Falls back.** Tapping Restart opened the system Bluetooth settings: the ROM refused the direct `disable()`, so the radio has to be toggled there by hand. The app never pretended otherwise. | 2026-09-06 |
| 17 | Why does `registerApp()` fail for good after the app was reinstalled, or killed while registered? | Every cold start after such an event would leave the remote dead, with nothing the app could do about it on its own. | **Observed 2026-09-06.** After an uninstall/reinstall the stack logged `unregisterAppUid(): caller UID doesn't match user UID` and refused every `registerApp()` from the new uid: it was still holding the old uid's registration, which our own `unregisterApp()` cannot clear. Only cycling Bluetooth cleared it — the app then registered and connected. The service now registers again by itself when the radio comes back, and its give-up message names this cause and the Restart fix. Whether an ordinary kill (not a reinstall) leaves the same stale state is not yet known. | 2026-09-06 |
| 18 | Can ADB be reached on the A1 itself, over the network or a data port? | Sweeping usages from the phone only ever shows what the projector does *not* answer to. Its own key layout files say which scancode maps to which Android keycode, and `getevent` shows whether a sent usage arrives at all — the difference between "no code works" and "no code we can send works". Without it, Focus + can only be guessed at. | | |

**Until question 2 is answered, the app must not present a power-on affordance it cannot honour.**

## Keypad findings — manual walk on the A1, 2026-09-06

Every function on the keypad was pressed by hand with the link live and the projector watched. Verbatim result per key:

| Function | Result on the A1 | Reading |
| --- | --- | --- |
| Power | No reaction, not even the key-press tone. | Consumer `0x0030` is not acted on (question 3). |
| Up / Down / Left / Right / OK | All work. | D-pad usages confirmed. |
| Back / Home | Both work. | Confirmed. |
| Menu | Single press: key-press tone but no action. **Double press: toggles the projected aspect ratio 16:9 ⇄ 4:3.** | The Menu usage reaches the A1; the projector's own OS binds a *double* press to aspect ratio, not a menu. Single Menu does nothing visible on this firmware. |
| Vol − / Vol + / Mute | All work. | Confirmed. |
| Focus − / Focus + / Source | Key-press tone on press, but no action on single / double / repeated press. | Reachable but unmapped (question 4). |
| Flip / Keystone | Nothing at all, not even the tone. | Usage does not reach the projector (question 4); fallback candidate. |
| Cursor (trackpad, click, drag) | Works. | Confirmed (question 9). |

Consequences for the app: Focus ±, Source, Flip, Keystone and Power all stay **unverified** — none of the standard usages the app currently sends drive them. Focus/Source/Power are worth a Fix-Keys usage search (they at least reach the input stack, or in Power's case are worth trying other usages); Flip/Keystone look like projector-OS-only functions that no HID usage will reach, which is what BUILD_SPEC §9's projector-side fallback exists for. The Menu double-press-for-aspect behaviour is the projector's, not the app's — nothing to change, but worth knowing when the operator expects a menu and gets an aspect flip.

## How to answer 2 and 3 without guessing

With the phone paired and the projector awake: send Power, note what the projector does and whether the HID link stays connected. Then, from standby, send any key and watch for a reaction. If the link drops the moment the projector sleeps, Bluetooth wake is impossible and question 2 is answered no.

## How to answer 1, 7 and 8

Run `HARDWARE_RUNBOOK.md` §1. It records what to look for and where.

## How to answer 9 and 10

With the phone paired and the projector awake, open the Trackpad tab and drag slowly from one edge of the surface to the other. Note whether a pointer appears on the projected image, how far it travelled, and whether it kept up with the thumb. Then tap once over a focusable item and see whether it activates, and put two fingers down and lift them to see whether the projector goes back. Until this is done, nothing in the app may claim the A1 accepts a mouse.

## How to answer 11 and 12 before trusting a sweep

Both are about whether a silent sweep means anything, so answer them before running one for real.

**11.** With the projector on a screen where the volume bar is visible, open Key Lab, switch to manual mode, choose `consumer`, and type `00E9` (Volume +). Press **Send** sixteen times at roughly the sweep's own pace — one press every 700 ms, counted out. If the volume rises sixteen steps, the projector acts on every press at that cadence and a silent sweep is real evidence. If it rises fewer, the sweep is too fast; record the number of steps that actually landed.

**12.** With the projector on any screen that shows a text field, in Key Lab manual mode choose `keyboard` and send `04` (the letter A) to prove the keyboard collection reaches the host at all. Then send `3A` (F1) and `68` (F13) and watch for anything — a focus move, a toast, a beep. If A types and F13 does nothing, F13–F24 are being dropped somewhere above the wire, and the screen-flip sweep result is a fact about Android rather than about the A1.

Record both answers here before filing a findings file that leans on them.

## How to answer 16

On the Setup screen (Register step), with Bluetooth on, tap **Restart**. If the radio visibly cycles off and back on by itself, the direct calls work on this ROM. If instead the system Bluetooth settings screen opens, the OS refused the direct call and the app fell back — note which happened. Same for **Off**: either the radio switches off, or the settings screen opens.
