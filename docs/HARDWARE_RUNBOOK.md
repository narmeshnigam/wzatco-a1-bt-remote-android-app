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

### 1.5 · Let the projector find the phone

Registering as a HID device does not make the phone discoverable. The projector has to be the
one that pairs.

1. In the app, open the **Setup** tab and tap **Make discoverable**, then accept the system
   prompt. (Opening the phone's own Settings → Bluetooth and leaving it on screen also works.)
2. On the projector: **Settings → Bluetooth** (or *Remote & accessories*) → scan.
3. The phone should appear under its Bluetooth name. Pair from the projector.
4. Accept the pairing prompt on the phone.

If the projector's Bluetooth screen only lists audio devices and never shows the phone, that is
open question 7 and it is a real answer — record it.

Back in the app, the link dot goes light, the host name appears, and the status line reads
**Connected**. `adb logcat` shows:

```
I/A1HidService: Callback.onConnectionStateChanged(device=…, state=CONNECTED)
```

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
3. Whether the projector's Bluetooth screen found the phone, and what name it showed.
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
- **Cursor**: switch to the **Cursor** tab, drag on the dashed surface. See open questions 9
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

## §6 · Sign-off

- `TEST_PLAN.md`'s manual matrix filled in, one row per function.
- `OPEN_QUESTIONS.md` has an answer or an explicit "still unknown" for every row.
- The release build installed and in daily use:
  ```bash
  ./gradlew assembleRelease
  adb install -r app/build/outputs/apk/release/app-release.apk
  ```
- `README.md` updated with what works, what does not, and why.
