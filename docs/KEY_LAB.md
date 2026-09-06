# Key map and Key Lab

## Default key map

**Confirmed** rows are standard usages an Android 9 host is expected to honour — implement them as written. **Candidate** rows are guesses; ship them as the first entry of that function's Key Lab list and render the key unverified until hardware confirms it.

| Function | Report | Usage | Status |
| --- | --- | --- | --- |
| Up / Down / Left / Right | Keyboard | `0x52` / `0x51` / `0x50` / `0x4F` | Confirmed |
| OK | Keyboard | `0x28` Enter | Confirmed |
| Back | Consumer | `0x0224` AC_Back (alt: Keyboard `0x29` Esc) | Confirmed |
| Home | Consumer | `0x0223` AC_Home | Confirmed |
| Menu | Keyboard | `0x65` Application | Confirmed |
| Volume + / − | Consumer | `0x00E9` / `0x00EA` | Confirmed |
| Mute | Consumer | `0x00E2` | Confirmed |
| Cursor move / click | Mouse | relative X/Y, button 1 | Confirmed |
| Power off | Consumer | `0x0030` Power | Candidate |
| Focus + | Consumer | `0x022D` Zoom In | Candidate |
| Focus − | Consumer | `0x022E` Zoom Out | Candidate |
| Source | Consumer | `0x0089` Media Select TV | Candidate |
| Screen flip | unknown | to be discovered | Unmapped |
| Keystone | unknown | to be discovered | Unmapped |

## Why Key Lab exists

The A1's projector-specific keys cannot be guessed from a desk. They may be standard usages, vendor keycodes, IR-only commands, or hooks into a vendor Android service above the input stack. Key Lab walks candidate lists against the real projector and records what happened.

## Protocol

1. Pick the function. Show candidate *n* of *m* with its exact usage.
2. **Send** transmits one key-down/key-up pair and nothing else, so the projector's reaction is unambiguous.
3. The operator marks **It worked** or **No effect**. A hit promotes that candidate into the key map immediately and advances to the next function; a miss advances to the next candidate.
4. Findings persist across restarts and export as JSON.

Also required: a **manual mode** where the operator types any usage code by hand, and a **sweep mode** that walks a range with a 700 ms gap and a visible index, so the operator can call out where the projector reacted.

## Candidate lists

| Function | Candidates, in order |
| --- | --- |
| Focus + | Consumer `0x022D` Zoom In · Keyboard `0x3E` F5 · Consumer `0x0225` AC_Forward · Keyboard `0x57` Keypad + · Keyboard `0x2E` Equals · Keyboard `0x4B` Page Up · Consumer `0x022F` Zoom · swept keyboard range `0x3A`–`0x45` (F1–F12) |
| Focus − | Consumer `0x022E` Zoom Out · Keyboard `0x3F` F6 · Keyboard `0x56` Keypad − · Keyboard `0x2D` Minus · Keyboard `0x4E` Page Down · swept keyboard range `0x3A`–`0x45` (F1–F12) |
| Source | Consumer `0x0089` Media Select TV · Keyboard `0x3D` F4 · Consumer `0x009C` Channel Increment · Consumer `0x009D` Channel Decrement · swept keyboard range `0x3A`–`0x45` (F1–F12) |
| Screen flip | Keyboard `0x3C` F3 · Keyboard `0x3A` F1 · swept keyboard range `0x68`–`0x73` (F13–F24) |
| Keystone | Keyboard `0x3B` F2 · Keyboard `0x40` F7 · swept consumer range `0x0180`–`0x018F` |
| Power off | Consumer `0x0030` Power · Consumer `0x0032` Sleep · Keyboard `0x66` Power (no sweep: a sweep that walked into a working power-off would take the projector down before the operator could say which code did it) |

Two corrections to this table, 2026-09-06:

- It previously called Consumer `0x009D` "Channel +". In the HID usage tables `0x009C` is Channel Increment and `0x009D` is Channel Decrement, so the name was wrong for the code. Both are now listed under their real names.
- Focus ± and Source gained the F1–F12 sweep. The shipped Focus + usage draws a key tone from the A1 but moves nothing, which means the usage arrives and no handler wants it; a vendor function bound to a plain function key is the next thing worth walking, and F1–F12 is inert on Android otherwise, so the sweep cannot type, navigate or switch anything off while it runs.

## Reading the A1's own key layout

Walking usages from the phone only ever shows what the projector does *not* answer to. The projector's own key layout files say which scancode it maps to which Android keycode, which is the difference between "no code works" and "no code we can send works". With ADB reachable on the A1 (open question 18):

```bash
adb connect <projector-ip>:5555
adb shell ls /system/usr/keylayout/          # vendor .kl files
adb shell cat /system/usr/keylayout/Generic.kl | grep -i "zoom\|focus"
adb shell getevent -lp                       # input devices, including the phone once connected
adb shell getevent -l                        # then press Focus + on the phone and read the event
```

`getevent -l` while the phone sends Focus + is the decisive test: it shows the exact Linux key code the A1 receives, or shows nothing, and that settles whether the function is reachable over HID at all.

## Findings JSON

```json
{
  "app_version": "1.0.0",
  "phone": "<manufacturer> <model> / Android <release>",
  "host": "WZATCO A1",
  "recorded_at": "2026-09-06T21:14:00+05:30",
  "results": [
    {
      "function": "FOCUS_UP",
      "report": "consumer",
      "usage": "0x022D",
      "usage_name": "Zoom In",
      "verdict": "mapped",
      "note": "focus stepped in one increment per press"
    },
    {
      "function": "SCREEN_FLIP",
      "report": "keyboard",
      "usage": "0x3C",
      "usage_name": "F3",
      "verdict": "no_effect",
      "note": null
    }
  ]
}
```

`verdict` is one of `mapped`, `no_effect`, `side_effect` (the projector did something, but not this function — always worth recording), `untested`.

## Rule

A function with no `mapped` verdict stays unverified in the UI and unmapped in the key map. Never fake it, never hide it, never substitute a nearby function.
