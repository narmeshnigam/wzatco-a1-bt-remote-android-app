# Gate 1 runbook — prove the transport

Gate 1 answers one question: does this phone's ROM let the app register as a Bluetooth HID
device, and does the projector accept it? No application code can fix a "no" here.

Nothing below needs the keypad UI. The app at this gate is a diagnostic screen.

## What you need

- The OnePlus phone, Bluetooth on.
- The WZATCO Alpha A1, powered on, with its own Bluetooth settings screen reachable.
- Optionally a USB cable and `adb`, for logcat. The screen shows the same facts.

## 1 · Install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and open it.

## 2 · Take the app off OxygenOS's battery management

Do this **before** the test, not after it fails. OxygenOS stops foreground services it thinks
are idle, and a stopped service silently drops the HID registration.

1. Settings → Apps → App management → **WZATCO A1 Remote** → **Battery usage**
   → set to **Unrestricted** (older builds: **Allow background activity** on, **Allow auto-launch** on).
2. Settings → Battery → **Battery optimisation** (may be under "More settings" or the ⋮ menu)
   → All apps → **WZATCO A1 Remote** → **Don't optimise**.
3. If the phone offers "Sleep standby optimisation" or "Deep optimisation", leave the app out of it.

## 3 · Grant permissions

Open the app. It shows a rationale screen first and asks for nothing on launch.

- Tap **Grant permissions**.
- Allow **Nearby devices**. This one is required.
- Allow **Notifications**. Refusing it leaves the app working but hides the service notification,
  which is exactly what keeps OxygenOS from stopping the link — so allow it for the test.

The debug screen appears and the service starts.

## 4 · Read the result — this is the gate

The panel on the debug screen lists each platform call and what it returned:

| Row | Pass | Meaning of a failure |
| --- | --- | --- |
| `BLUETOOTHMANAGER` | `true` | The system has no Bluetooth service at all. |
| `ADAPTER` | `true` | No Bluetooth adapter. |
| `ADAPTER ENABLED` | `true` | Bluetooth is switched off. Turn it on. |
| `GETPROFILEPROXY()` | `true` | **`FALSE` means the ROM refused the HID Device profile.** Gate 1 fails here. |
| `PROXY CONNECTED` | `true` | The proxy request was accepted but the callback never came. |
| `REGISTERAPP()` | `true` | **`FALSE` means the ROM will not host a HID Device app.** Gate 1 fails here. |
| `ONAPPSTATUSCHANGED` | `true` | The stack accepted the call and then refused the registration. |

The status line under the host name should read **Registered, no host**.

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

## 5 · Let the projector find the phone

Registering as a HID device does not make the phone discoverable. The projector has to be the
one that pairs.

1. On the phone, open **Settings → Bluetooth** and leave that screen open. The phone is
   discoverable only while it is showing.
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

## 6 · Send the key

Point the projector at a screen with something selectable — its own settings list will do.

Tap **SEND ARROW DOWN**.

- The projector's on-screen focus should move down one item.
- The wire log at the bottom of the app should show two lines:
  `DOWN  down id=1 [00 00 51 00 00 00 00 00] -> true` and `DOWN  up   id=1 [00 00 …] -> true`.

`-> true` means the Bluetooth stack accepted the report. If the log says `true` and the projector
does not move, the report left the phone and the projector ignored it — a different failure from
a refused registration, and worth recording as such.

## 7 · What to report back

1. Phone model and Android version (Settings → About device). Goes into open question 6.
2. The exact value each of the four logcat lines printed.
3. Whether the projector's Bluetooth screen found the phone, and what name it showed.
4. Whether the focus moved.
5. Whether the link survived a screen-off and back (open question 8).

## Known limits at this gate

- There is no keypad. One button, one function. That is the whole point of the gate.
- The app cannot make itself discoverable yet; step 5 uses the system Bluetooth screen. The
  Setup screen at Gate 2 will do this itself.
- Reconnect after a dropped link is Gate 3. At this gate, use **REGISTER** to start again.
