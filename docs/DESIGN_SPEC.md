# Design spec — WZATCO A1 Remote

The reference rendering is `docs/design/WZATCO A1 Remote - Prototype.dc.html`. Open it in a browser; it is interactive. **It predates the 2026-09-10 keypad change** — it still shows the 3 × 3 D-pad block and the four rows of three — so read the keypad section below rather than the prototype for that screen. This file is the authority for values.

Design system: **Industry** — square corners (the D-pad dial excepted), hairline borders, steel accent, condensed headings. The remote screens run on the accent's deepest step so the app is usable in an unlit room.

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

No other colors. No gradients and no shadows. The D-pad dial is the one round thing in the app; every other edge is square.

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
- Key grid gutter: 6 dp. Key height: 62 dp.
- D-pad dial: 258 dp across, shrinking to the height available on a short screen and always square. The OK hub's radius is 0.40 of the dial's; each arrow owns a 90° segment of the ring, hinged on the diagonals.
- Power key: 54 × 54 dp, top right of the status row.
- Bottom tab bar: 58 dp tall, four equal cells, 1 dp top border, 1 dp dividers; the active tab is filled `#2C455D`. The four tabs read **Keypad · Trackpad · Keyboard · Setup**.
- Icons: 21 dp in keys, 26 dp in the D-pad, stroke width 1.5, square line caps off (round joins).
- **Non-keypad compaction.** The Trackpad, Fix Keys and Setup screens use an 8 dp vertical rhythm and 12 dp card padding (down from 14 dp) so the whole of each screen fits without scrolling a small contained section. The keypad's geometry below is deliberately untouched — position is the only cue a thumb has in the dark.

## Keypad layout, top to bottom

1. **Status row** — link dot (9 dp), host name over link state; power key at the right.
2. **D-pad dial** — 28 dp clear of the block below it, so the break between the two reads as a break and not as another gutter. A ring of four arrow segments around an accent-filled OK hub. Hairline outlines; the pressed segment fills with `#416180`. The ring is **one touch surface resolved by angle**, not four curved buttons: anywhere on the upper arc is Up, so the thumb never has to find a target it cannot see. The square corners the circle leaves over stay empty.
3. **Lower block** — 130 dp (two key heights and the gutter between them), three equal columns of two rows:

   | | | |
   | --- | --- | --- |
   | **Vol +** | Home | Back |
   | **Vol −** | Menu | Mute |

   The volume column is **one key the height of two**: a single outline spanning both rows with a hairline divider across it, Vol + above and Vol − below. Nothing else on the block is that shape, which is what lets a thumb find it without looking. The other four are ordinary key-height cells.

The dial and the block are centred as a group in the height they are given, so spare space on a tall phone falls above and below the pad rather than under it.

**What is not on this screen, and why.** Focus ±, Source, Screen flip and Keystone are gone. Nothing the app shipped for them moves the A1 (`OPEN_QUESTIONS.md`, question 4), so rather than keep four keys that do nothing they were removed from the app entirely. What drives them is still an open question, not a closed one. Trackpad has no key here either — the tab bar is how that screen is reached.

This geometry is fixed: it is what makes the remote usable by thumb position alone. Do not add, remove or reorder keys without changing this spec first.

## Verified vs unverified

A key is drawn unverified — dashed border, accent-300 label, and a small **"SET UP"** caption under its label — until Fix Keys (Key Lab) confirms its usage on the A1. Confirmation flips it to the solid style at runtime, from the key map, with no rebuild, and the caption disappears. This is the app telling the truth about what it knows.

**An unverified key is not sent — it routes.** Power is the only such key left, and no code tried on the A1 has powered it off (confirmed on hardware). Tapping an unverified key does not transmit a dead report; it opens **Fix Keys** with that button preselected, so the dead key becomes the way to fix it. The Power key does the same: while unverified a plain tap routes to Fix Keys (no 600 ms hold), and only once confirmed does it become the deliberate hold-to-power-off. The moment a code is confirmed, the key sends normally.

## States

| State | Treatment |
| --- | --- |
| Press | Cell fills `#416180` for 90 ms; 12 ms haptic on key-down |
| Disabled (no host) | 45% opacity, no press effect, no report sent |
| Focus (hardware keyboard) | 2 dp accent outline, 2 dp offset |
| Power long-press | Progress is implied by a second haptic tick at 600 ms, then the report |

## Trackpad screen

The tab reads **Trackpad**. A full-height dashed drag surface with a subtle two-line hint inside it, so it reads as a trackpad rather than an empty box. Down its right edge sits a **collapsible scroll strip**: a thin handle by default, tapped to expand into a vertical drag lane that sends wheel motion (the one thing the D-pad and the two-finger gestures cannot reach). Below the surface, one compact row of three equal keys at the minimum touch height (58 dp, not the 62 dp key height): **Left click · Double click · Back**. On the surface, a drag moves the pointer, a single tap is left click, and a two-finger tap is Back. No visible cursor on the phone — the projector owns that. Return to the keypad is via the tab bar; there is no separate return key.

## Keyboard screen

The tab reads **Keyboard**. It types into whatever field the projector has focused, and it is the third tab because a Wi-Fi password or a search box is a thing the user needs at the moment they need it — which is what Fix Keys, a tool used once, was not.

Top to bottom: the title and a two-line hint saying to focus a field on the projector first and that nothing goes out until Send; a **draft box** 108 dp tall with a hairline border, multi-line so a password can be read whole; one line under it in accent-300 giving the character count, or naming the characters that have no key; a row of three keys at the minimum touch height (58 dp) — **Backspace · Space · Enter** — which go straight to the projector rather than into the box, with a line under them saying so; then the status line and the action row: **Clear** and **Send text** (primary, double width). While a run is in flight, Send becomes **Stop** and the status line counts `Typing 7 of 12…`.

**Nothing is transmitted as it is typed.** The draft is written, read back on a screen in the hand rather than across the room, then sent as one run of keystrokes. **A draft with any character this keyboard has no key for cannot be sent at all** — not the typable part of it — because half a password in a field is worse than none: the user cannot see which half arrived. The box turns off autocorrect and auto-capitalisation; a phone silently "correcting" a password would type it into the projector wrong with nothing on screen to say so.

## Key Lab screen — user-facing name **“Fix Keys”**

**Not a tab.** Fix Keys is opened from **Setup → Diagnostics**, and from a tap on an unverified key, as a full-screen overlay with a **Close** key at its foot. It is a workshop, not a remote control: it is reached when a button does nothing, and a remote should not carry a permanent tab for that.

The title reads **Fix Keys**: the operator is fixing the buttons the A1 ignores, and "Key Lab" meant nothing to a real user. The intro says so in one plain sentence. The internal names (`KeyLabScreen`, `KeyLabViewModel`, KEY_LAB.md) keep the old word; only the user-facing strings change.

A **button picker** runs across the top — a horizontally scrollable row of compact chips, one per function under test, the selected one filled with the accent — so the operator can jump straight to the button that needs fixing instead of only advancing by recording verdicts. Power off is the only function left under test, so today the picker holds one chip.

Below it, the button-under-test card (solid hairline border): button name in condensed 22 sp, code index and usage in accent-300 below. The modes read **Suggested · Type code · Auto-scan** (was Candidates · Manual · Sweep). Auto-scan appears only for a function with a range to walk; no shipped function has one — KEY_LAB.md refuses Power a sweep on purpose — so the chip is currently absent rather than present and dead. Verdict list in a dashed-border box, newest last, function · usage on the left and verdict on the right. Action keys at the bottom: **Send** filled accent, *It worked* and *No effect* outlined; a secondary row carries *Did something else* (a side effect) and *Export JSON*.

## Setup screen

A three-step flow, shown one step at a time under a **stepper header** of three cells — `1 Register · 2 Pair · 3 Connect` — flowing left to right, each a numbered badge and label. The active cell is filled with the accent; cells the link has already reached carry an accent border. The active step follows the link's own progress (registering → registered → connecting/connected) and the user can tap back to any step.

- **Register** — the registration status card, plus the phone's own **Bluetooth controls**: a live `PHONE BLUETOOTH · ON/OFF` dot and three keys, **On · Off · Restart**. Restart cycles the radio off and on — the surest fix for a stuck registration. On this phone the OS refuses the direct toggle and the system Bluetooth screen opens instead (question 16, answered); once the radio is back the app registers again by itself. A refused registration says so in the status card and points at Restart. The Register primary action sits at the bottom, enabled only when Bluetooth is on.
- **Pair** — the pairing instruction, a **Make discoverable** and a **Refresh** key (refresh re-reads the bond list by hand), then the paired-device list, tap-to-connect, capped at four rows before it scrolls.
- **Connect** — the connection status (and the auto-connect note: the app reaches for the last connected device on start), the dashed standby-test unknown, and the **Connect to A1 / Disconnect** primary.

Below the step, on every step, a **Diagnostics** row of two keys at the minimum touch height: **Fix Keys** and **Wire log**. Both open as full-screen overlays with a Close key. The wire log is still reachable by long-pressing the status row; this makes it findable without knowing that.

The app remembers the last host it actually connected to and reaches for it automatically the next time it starts.

## Orientation

The app is **portrait only**. It is a remote held in one hand; rotation is locked in the manifest.

## Wire log

Not a shipped screen in v1 — it is the diagnostic panel shown beside the prototype. If you build it, put it behind a long-press on the status row: monospace 12 sp, newest first, capped at the last 50 reports, format `mm:ss  Function  Report usage`.
