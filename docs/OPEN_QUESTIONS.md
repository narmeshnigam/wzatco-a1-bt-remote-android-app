# Open questions

Answer these from the hardware. Record the answer and the date. Do not design around a guess, and do not delete a question — mark it answered.

| # | Question | Why it matters | Answer | Date |
| --- | --- | --- | --- | --- |
| 1 | Does the phone's ROM permit `BluetoothHidDevice.registerApp()`? | If not, the entire architecture is void on this phone. Gate 1. | | |
| 2 | Does the A1 keep its Bluetooth host alive in standby? | Decides whether Bluetooth can power the projector **on** at all, or whether that stays IR / physical button. | | |
| 3 | Does Consumer `0x0030` Power turn the A1 off, or only sleep the Android layer? | Changes what the power key can honestly promise. | | |
| 4 | Are Focus, Source, Flip and Keystone reachable as input events at all? | If a vendor service above the input stack owns them, no HID usage will ever reach them and the fallback path opens. | | |
| 5 | Does the A1 remember the pairing across a full power cycle? | Decides whether Setup is a one-time flow or a recurring chore. | | |
| 6 | What is the phone model and Android version used for the build? | Needed for the findings file and for reproducing any ROM-specific behaviour. | | |
| 7 | Does the A1's own Bluetooth settings screen offer pairing with an input device at all, or only with audio devices? | The phone cannot initiate the HID connection until the projector has bonded with it. If the A1 only pairs speakers and headsets, Gate 1 cannot pass however well the phone registers. | | |
| 8 | Does OxygenOS keep `HidService` alive with battery optimisation disabled, or does it stop the service anyway? | Decides whether the remote survives a screen-off, and whether Gate 3's reconnect logic is a convenience or the only thing that makes the app usable. | | |
| 9 | Does the A1 act on the mouse collection at all — does a drag move a pointer on the projected image, and does a left click activate what is under it? | The cursor screen is the only way to reach anything the D-pad cannot focus. If the ROM ignores report 3, the screen is dead weight and its keys must be drawn unverified. | | |
| 10 | At 1.6× acceleration, does one full-length drag across the phone cross the projected image? | Decides whether the acceleration factor of BUILD_SPEC §6 is usable or has to change. It depends on the A1's pointer resolution and its own pointer acceleration, neither of which is known. | | |

**Until question 2 is answered, the app must not present a power-on affordance it cannot honour.**

## How to answer 2 and 3 without guessing

With the phone paired and the projector awake: send Power, note what the projector does and whether the HID link stays connected. Then, from standby, send any key and watch for a reaction. If the link drops the moment the projector sleeps, Bluetooth wake is impossible and question 2 is answered no.

## How to answer 1, 7 and 8

Run `GATE1_RUNBOOK.md`. It records what to look for and where.

## How to answer 9 and 10

With the phone paired and the projector awake, open the Cursor tab and drag slowly from one edge of the surface to the other. Note whether a pointer appears on the projected image, how far it travelled, and whether it kept up with the thumb. Then tap once over a focusable item and see whether it activates, and put two fingers down and lift them to see whether the projector goes back. Until this is done, nothing in the app may claim the A1 accepts a mouse.
