# Hardware runbook

The app was built through all five gates before any of it met the projector. This is the
running order for the test session, worst-consequence first: if §1 fails, nothing after it
can work, and the fallback in `BUILD_SPEC.md` §9 opens instead.

Record answers as you go — `OPEN_QUESTIONS.md` has a row for each and none of them may be
guessed at.

| § | What it proves | If it fails |
| --- | --- | --- |
| 1 | The phone's ROM permits the HID Device profile | Stop. No application code fixes this; the phone has to change. |
| 2 | The projector pairs with the phone and honours arrow keys | Stop. The A1 may not accept HID input at all (open question 7). |
| 3 | The keypad drives the A1 with no mouse attached | Gate 2's exit test. |
| 4 | The remote is usable in the dark for ten minutes | Gate 3's exit test. |
| 5 | Key Lab resolves the five unknown functions | Gate 4's exit test. Some may stay unmapped — that is a result, not a failure. |
| 6 | Sign-off | Gate 5. |

---

## §1 · Prove the transport

This answers one question: does this phone's ROM let the app register as a Bluetooth HID
device, and does the projector accept it? No application code can fix a "no" here.

### 1.0 · What you need

- The OnePlus phone, Bluetooth on.
- The WZATCO Alpha A1, powered on, with its own Bluetooth settings screen reachable.
- Optionally a USB cable and `adb`, for logcat. The screen shows the same facts.

### 1.1 · Install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and open it.

### 1.2 · Take the app off OxygenOS's battery management

Do this **before** the test, not after it fails. OxygenOS stops foreground services it thinks
are idle, and a stopped service silently drops the HID registration.

1. Settings → Apps → App management → **WZATCO A1 Remote** → **Battery usage**
   → set to **Unrestricted** (older builds: **Allow background activity** on, **Allow auto-launch** on).
2. Settings → Battery → **Battery optimisation** (may be under "More settings" or the ⋮ menu)
   → All apps → **WZATCO A1 Remote** → **Don't optimise**.
3. If the phone offers "Sleep standby optimisation" or "Deep optimisation", leave the app out of it.

### 1.3 · Grant permissions

Open the app. It shows a rationale screen first and asks for nothing on launch.

- Tap **Grant permissions**.
- Allow **Nearby devices**. This one is required.
- Allow **Notifications**. Refusing it leaves the app working but hides the service notification,
  which is exactly what keeps OxygenOS from stopping the link — so allow it for the test.

The debug screen appears and the service starts.

### 1.4 · Read the result — this is the gate

Long-press the host name at the top of the screen to open **Diagnostics**. It lists each
platform call and what it returned:

| Row | Pass | Meaning of a failure |
| --- | --- | --- |
| `BLUETOOTHMANAGER` | `true` | The system has no Bluetooth service at all. |
| `ADAPTER` | `true` | No Bluetooth adapter. |
| `ADAPTER ENABLED` | `true` | Bluetooth is switched off. Turn it on. |
| `GETPROFILEPROXY()` | `true` | **`FALSE` means the ROM refused the HID Device profile.** Gate 1 fails here. |
| `PROXY CONNECTED` | `true` | The proxy request was accepted but the callback never came. |
| `REGISTERAPP()` | `true` | **`FALSE` means the ROM will not host a HID Device app.** Gate 1 fails here. |
| `ONAPPSTATUSCHANGED` | `true` | The stack accepted the call and then refused the registration. |

The status line under the host name should read **HID · registered, no host**.

The same facts are in logcat, verbatim:

```bash
adb logcat -c && adb logcat -s A1HidService:V
```

You are looking for these three lines:

```
I/A1HidService: getSystemService(BluetoothManager) returned android.bluetooth.BluetoothManager@…
I/A1HidService: BluetoothAdapter.getProfileProxy(HID_DEVICE) returned true
I/A1HidService: BluetoothHidDevice.registerApp() returned true
I/A1HidService: Callback.onAppStatusChanged(pluggedDevice=null, registered=true)
```

**Record what these four lines actually said.** If `registerApp()` returned `false`, or
`onAppStatusChanged` reported `registered=false`, stop — that is the answer to open question 1
and the fallback in BUILD_SPEC §9 opens up. Nothing further in this runbook will work.

### 1.5 · Pair from the phone, then connect from the app

The projector's own Bluetooth screen never lists the phone. On 2026-09-06 the A1 scanned with the
phone discoverable and showed nothing under *Available equipment*, while its paired list held two
speakers and one input device (`RGE_B_DF0415`). Its scan hides phone-class devices; it is a UI
filter, not a stack limit. So the pairing runs the other way round:

1. Identify the projector. Its Bluetooth name is **`NL_415AF8`**, address `BC:6B:FF:41:5A:F9`,
   device class *Computer*, device-ID record vendor `0x00E0` / product `0x1200` — the values a
   stock Android Bluetooth stack registers. (Seen from a Mac's Bluetooth panel on 2026-09-06.)
   A device called `BC8-Android` is **not** the projector: it advertises only audio profiles and
   never answers a page.
2. On the phone: **Settings → Bluetooth → pair with `NL_415AF8`**. Accept any prompt on either
   side.
3. In the app, open **Setup**. The projector appears under *Paired devices · tap to connect*.
   Tap it. The app calls `BluetoothHidDevice.connect()`; a stock Android 9 host accepts an
   incoming HID connection from any bonded device.

Back in the app, the link dot goes light, the host name appears, and the status line reads
**Connected**. `adb logcat` shows:

```
I/A1HidService: Callback.onConnectionStateChanged(device=…, state=CONNECTED)
```

If the stack log instead shows `LOWER_LAYER_CONNECT_CFM_NEG` on PSM `0x0011`, the projector
refused the HID control channel. Record the exact reason code; it is the answer to open
question 7.

### 1.6 · Send the key

Point the projector at a screen with something selectable — its own settings list will do.

On the **Keypad** tab, tap the **down arrow** of the D-pad.

- The projector's on-screen focus should move down one item.
- The wire log (long-press the host name) should show two lines:
  `DOWN Down Arrow  down id=1 [00 00 51 00 00 00 00 00] -> true` and a matching `up`.

`-> true` means the Bluetooth stack accepted the report. If the log says `true` and the projector
does not move, the report left the phone and the projector ignored it — a different failure from
a refused registration, and worth recording as such.

### 1.7 · What to report back

1. Phone model and Android version (Settings → About device). Goes into open question 6.
2. The exact value each of the four logcat lines printed.
3. Whether the phone bonded to `NL_415AF8`, and whether the projector's paired list shows the phone afterwards.
4. Whether the focus moved.
5. Whether the link survived a screen-off and back (open question 8).

If the log says `-> true` and the projector does not move, the report left the phone and the
projector ignored it. That is a different failure from a refused registration and is worth
recording as such — it points at open question 7, not at the ROM.

---

## §2 · Drive the projector from the keypad

Gate 2's exit test: the projector is fully navigable from the phone with **no mouse attached**.
Unplug the mouse before you start — otherwise you cannot tell which device did the work.

Work through the eight confirmed functions on the **Keypad** tab and fill in
`TEST_PLAN.md`'s manual matrix as you go. One row per function, latency by eye against a
repeated press.

| Key | Expect |
| --- | --- |
| D-pad up / down / left / right | Focus moves one item per press |
| OK | Activates the focused item |
| Back | Goes back one level |
| Home | Returns to the A1's launcher |
| Menu | Opens the context menu, where the current app has one |
| Vol − / Vol + | Volume steps, on-screen indicator moves |
| Mute | Audio mutes and unmutes |

The four keys with a **dashed** border — Focus ±, Source, Flip, Keystone — and the power key
are *not* part of this section. They are unproven by definition and §5 is where they get
tested. Pressing Flip or Keystone transmits nothing at all, deliberately: they have no
mapping yet.

If a confirmed key does nothing, check the wire log first (long-press the host name). `-> true`
means the report left the phone.

## §3 · Ten minutes in a dark room

Gate 3's exit test. No misfires, no stuck keys.

- **Auto-repeat**: hold the down arrow. Focus should start repeating after about 0.4 s and then
  step about eleven times a second. Same for Volume. Release and it must stop immediately —
  a key that keeps repeating after release is a bug worth stopping for.
- **Haptics**: every key-down should tick. It fires on press, not release.
- **Power**: hold the power key at the top right. It should do nothing until 600 ms, then give
  a second, doubled tick before it transmits. A tap must do nothing at all. **Read the warning
  under §5 before you press it.**
- **Trackpad**: switch to the **Trackpad** tab, drag on the dashed surface. See open questions 9
  and 10 — nobody has confirmed the A1 acts on mouse reports at all.
- **Screen**: the phone must not blank while the app is in front.
- **Standby**: put the projector into standby, wait, and press a key. Then wake it. This is
  open question 2 and it decides whether the power key can ever turn the A1 *on*.
- **Idle**: leave it ten minutes untouched, then press a key. If nothing happens, check
  whether the link dropped and whether the three reconnect attempts are in the wire log.

## §4 · Answer the standing questions

`OPEN_QUESTIONS.md` rows 2, 3, 5, 7, 8, 9 and 10 all get answered in this session. Each has
its own "how to answer" note in that file. Fill in the answer and the date; if something is
still unknown after trying, write "still unknown" rather than leaving it blank.


## §5 · Key Lab — resolve the five unknowns

Gate 4's exit test: a findings JSON with a verdict for every unresolved function, and every
function that resolved now works from the keypad.

**Read this first.** Key Lab's Power list starts with Consumer `0x0030`. Do not run the Power
function until you are willing for the projector to switch off, and do §3's standby test after
it, not before — open question 2 is what decides whether Bluetooth can ever turn it back *on*.
If the answer turns out to be no, the projector's own button is the only way back.

Point the projector at something where a change is unmistakable: a menu with a visible focus
ring for Flip and Keystone, a picture with visible sharpness for Focus.

### 5.1 · Walk the candidate lists

Open the **Key Lab** tab. It starts at Focus + , candidate 1 of 4.

1. **Send** transmits exactly one key-down/key-up pair. Nothing else is transmitted, so
   whatever the projector does is attributable to that one usage.
2. Watch the projector, then mark it:
   - **It worked** — the function did what its name says. The candidate is promoted into the
     key map immediately; go back to the **Keypad** tab and confirm the key is now drawn solid
     rather than dashed, and that pressing it does the same thing. Key Lab advances to the next
     function.
   - **No effect** — nothing happened. Advances to the next candidate.
   - **Side effect** — the projector did *something*, but not this function. Record it. This is
     the most valuable verdict in the file: it says the usage reaches the projector and is bound
     to something else, which narrows the search far more than silence does.
3. When a list runs out, Key Lab moves to that function's swept range if it has one.

### 5.2 · Before you trust a sweep

**Auto-scan (sweep) mode proves nothing until open question 11 is answered.** It walks a range at 700 ms per
step, and if the A1 coalesces or debounces presses at that rate a real hit can pass unseen.

Answer it first: on the keypad, press **Vol +** sixteen times at roughly the sweep's pace and
count the volume steps. Sixteen steps means the cadence is safe. Fewer means slow down — do the
range by hand in **Manual** mode instead, and record that in question 11.

Then, for Screen flip, answer question 12 the same way before sweeping F13–F24: send keyboard
`0x04` (the letter A) in Manual mode over a text field. If nothing arrives, the keyboard
collection is not reaching applications and the whole F13–F24 sweep is meaningless.

### 5.3 · Manual mode

Type any usage by hand. Keyboard usages are `0x00`–`0xFF`; consumer usages are
`0x0000`–`0x03FF` and **not** the full 16 bits — the descriptor declares that ceiling
deliberately, because a wider range makes the host's HID parser reject the whole descriptor.

### 5.4 · Export

**Export JSON** writes the findings file wherever you choose. Do this even if every verdict is
`no_effect`: a function with no `mapped` verdict is a real result, and it is what opens the
fallback in `BUILD_SPEC.md` §9.

A function that survives Key Lab unmapped stays dashed on the keypad and transmits nothing.
That is the app being honest, not the app being broken. Do not hand-edit the key map to make a
key look finished.

## §6 · Sign-off

- `TEST_PLAN.md`'s manual matrix filled in, one row per function.
- `OPEN_QUESTIONS.md` has an answer or an explicit "still unknown" for every row.
- The release build installed and in daily use:
  ```bash
  ./gradlew assembleRelease
  adb install -r app/build/outputs/apk/release/app-release.apk
  ```
- `README.md` updated with what works, what does not, and why.
