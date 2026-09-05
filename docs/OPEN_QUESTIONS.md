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

**Until question 2 is answered, the app must not present a power-on affordance it cannot honour.**

## How to answer 2 and 3 without guessing

With the phone paired and the projector awake: send Power, note what the projector does and whether the HID link stays connected. Then, from standby, send any key and watch for a reaction. If the link drops the moment the projector sleeps, Bluetooth wake is impossible and question 2 is answered no.
