# Design spec — WZATCO A1 Remote

The reference rendering is `docs/design/WZATCO A1 Remote — Prototype.dc.html`. Open it in a browser; it is interactive. This file is the authority for values.

Design system: **Industry** — square corners, hairline borders, steel accent, condensed headings. The remote screens run on the accent's deepest step so the app is usable in an unlit room.

## Palette

| Role | Value | Use |
| --- | --- | --- |
| Field | `#1D2D3D` | Every remote screen background |
| Paper | `#F2F2F3` | Primary text and icons on the field |
| Accent | `#5980A6` | The single primary action per screen (OK, Send, Connect) |
| Pressed | `#416180` | Key press fill |
| Key border | `#F2F2F3` at 35% | Confirmed key outline, 1 dp |
| Unverified border | `#94BCE3`, dashed 1 dp | Keys not yet confirmed on the A1 |
| Unverified label | `#B5D9FD` | Text and icon of an unverified key |
| Muted label | `#F2F2F3` at 45% | Status sub-labels, hints |
| Link live | `#94BCE3` dot | Connected |
| Link dead | `#7A7A7D` dot | Registered, no host |

No other colors. No gradients, no shadows, no rounded corners anywhere.

## Type

Roboto Condensed (or the platform condensed face) for headings and the OK glyph; the platform sans for everything else.

| Element | Size | Treatment |
| --- | --- | --- |
| Screen title | 25 sp | Condensed |
| Host name | 19 sp | Condensed |
| OK glyph | 22 sp | Condensed, letter-spacing 0.1em |
| Key label | 11 sp | Uppercase, letter-spacing 0.1em |
| Status sub-label | 11 sp | Uppercase, letter-spacing 0.12em, muted |
| Body / hint | 12–13 sp | Regular |

## Metrics

- Screen padding: 18 dp horizontal.
- Key grid gutter: 6 dp. Key height: 62 dp (D-pad cells 86 dp in a 258 dp block).
- Power key: 54 × 54 dp, top right of the status row.
- Bottom tab bar: 58 dp tall, four equal cells, 1 dp top border, 1 dp dividers; the active tab is filled `#2C455D`. The four tabs read **Keypad · Trackpad · Fix Keys · Setup**.
- Icons: 21 dp in keys, 26 dp in the D-pad, stroke width 1.5, square line caps off (round joins).
- **Non-keypad compaction.** The Trackpad, Fix Keys and Setup screens use an 8 dp vertical rhythm and 12 dp card padding (down from 14 dp) so the whole of each screen fits without scrolling a small contained section. The keypad's geometry below is deliberately untouched — position is the only cue a thumb has in the dark.

## Keypad layout, top to bottom

1. **Status row** — link dot (9 dp), host name over link state; power key at the right.
2. **D-pad block** — 258 dp tall, 3 × 3 grid, 6 dp gutters. Up / Left / Right / Down in the edge cells, OK in the centre filled with the accent. Corner cells empty.
3. **Row A** — Back · Home · Menu.
4. **Row B** — Vol − · Mute · Vol +.
5. **Row C** — Focus − · Focus + · Source *(all unverified)*.
6. **Row D** — Flip · Keystone *(unverified)* · Trackpad.

Every row is three equal cells. This geometry is fixed: it is what makes the remote usable by thumb position alone. Do not add, remove or reorder keys without changing this spec first.

## Verified vs unverified

A key is drawn unverified — dashed border, accent-300 label, and a small **"SET UP"** caption under its label — until Fix Keys (Key Lab) confirms its usage on the A1. Confirmation flips it to the solid style at runtime, from the key map, with no rebuild, and the caption disappears. This is the app telling the truth about what it knows.

**An unverified key is not sent — it routes.** Because none of the guessed codes for Focus ±, Source, Flip, Keystone or Power actually drive the A1 (confirmed on hardware), tapping an unverified key does not transmit a dead report; it opens **Fix Keys** with that button preselected, so the dead key becomes the way to fix it. The Power key does the same: while unverified a plain tap routes to Fix Keys (no 600 ms hold), and only once confirmed does it become the deliberate hold-to-power-off. The moment a code is confirmed, the key sends normally.

## States

| State | Treatment |
| --- | --- |
| Press | Cell fills `#416180` for 90 ms; 12 ms haptic on key-down |
| Disabled (no host) | 45% opacity, no press effect, no report sent |
| Focus (hardware keyboard) | 2 dp accent outline, 2 dp offset |
| Power long-press | Progress is implied by a second haptic tick at 600 ms, then the report |

## Trackpad screen

The tab reads **Trackpad**. A full-height dashed drag surface with a subtle two-line hint inside it, so it reads as a trackpad rather than an empty box. Down its right edge sits a **collapsible scroll strip**: a thin handle by default, tapped to expand into a vertical drag lane that sends wheel motion (the one thing the D-pad and the two-finger gestures cannot reach). Below the surface, one compact row of three equal keys at the minimum touch height (58 dp, not the 62 dp key height): **Left click · Double click · Back**. On the surface, a drag moves the pointer, a single tap is left click, and a two-finger tap is Back. No visible cursor on the phone — the projector owns that. Return to the keypad is via the tab bar; there is no separate return key.

## Key Lab screen — user-facing name **“Fix Keys”**

The tab and title read **Fix Keys**: the operator is fixing the buttons the A1 ignores, and "Key Lab" meant nothing to a real user. The intro says so in one plain sentence. The internal names (`KeyLabScreen`, `KeyLabViewModel`, KEY_LAB.md) keep the old word; only the user-facing strings change.

A **button picker** runs across the top — a horizontally scrollable row of compact chips, one per function under test (the six of KEY_LAB.md), the selected one filled with the accent — so the operator can jump straight to the button that needs fixing instead of only advancing by recording verdicts.

Below it, the button-under-test card (solid hairline border): button name in condensed 22 sp, code index and usage in accent-300 below. The three modes read **Suggested · Type code · Auto-scan** (was Candidates · Manual · Sweep). Verdict list in a dashed-border box, newest last, function · usage on the left and verdict on the right. Action keys at the bottom: **Send** filled accent, *It worked* and *No effect* outlined; a secondary row carries *Did something else* (a side effect) and *Export JSON*.

## Setup screen

A three-step flow, shown one step at a time under a **stepper header** of three cells — `1 Register · 2 Pair · 3 Connect` — flowing left to right, each a numbered badge and label. The active cell is filled with the accent; cells the link has already reached carry an accent border. The active step follows the link's own progress (registering → registered → connecting/connected) and the user can tap back to any step.

- **Register** — the registration status card, plus the phone's own **Bluetooth controls**: a live `PHONE BLUETOOTH · ON/OFF` dot and three keys, **On · Off · Restart**. Restart cycles the radio off and on — the surest fix for a stuck registration. On Android 13 the OS may refuse a direct toggle, in which case the system Bluetooth screen opens instead (open question 16). The Register primary action sits at the bottom, enabled only when Bluetooth is on.
- **Pair** — the pairing instruction, a **Make discoverable** and a **Refresh** key (refresh re-reads the bond list by hand), then the paired-device list, tap-to-connect, capped at four rows before it scrolls.
- **Connect** — the connection status (and the auto-connect note: the app reaches for the last connected device on start), the dashed standby-test unknown, and the **Connect to A1 / Disconnect** primary.

The app remembers the last host it actually connected to and reaches for it automatically the next time it starts.

## Orientation

The app is **portrait only**. It is a remote held in one hand; rotation is locked in the manifest.

## Wire log

Not a shipped screen in v1 — it is the diagnostic panel shown beside the prototype. If you build it, put it behind a long-press on the status row: monospace 12 sp, newest first, capped at the last 50 reports, format `mm:ss  Function  Report usage`.
