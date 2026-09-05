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
- Bottom tab bar: 58 dp tall, four equal cells, 1 dp top border, 1 dp dividers; the active tab is filled `#2C455D`.
- Icons: 21 dp in keys, 26 dp in the D-pad, stroke width 1.5, square line caps off (round joins).

## Keypad layout, top to bottom

1. **Status row** — link dot (9 dp), host name over link state; power key at the right.
2. **D-pad block** — 258 dp tall, 3 × 3 grid, 6 dp gutters. Up / Left / Right / Down in the edge cells, OK in the centre filled with the accent. Corner cells empty.
3. **Row A** — Back · Home · Menu.
4. **Row B** — Vol − · Mute · Vol +.
5. **Row C** — Focus − · Focus + · Source *(all unverified)*.
6. **Row D** — Flip · Keystone *(unverified)* · Cursor.

Every row is three equal cells. This geometry is fixed: it is what makes the remote usable by thumb position alone. Do not add, remove or reorder keys without changing this spec first.

## Verified vs unverified

A key is drawn unverified — dashed border, accent-300 label — until Key Lab confirms its usage on the A1. Confirmation flips it to the solid style at runtime, from the key map, with no rebuild. This is the app telling the truth about what it knows.

## States

| State | Treatment |
| --- | --- |
| Press | Cell fills `#416180` for 90 ms; 12 ms haptic on key-down |
| Disabled (no host) | 45% opacity, no press effect, no report sent |
| Focus (hardware keyboard) | 2 dp accent outline, 2 dp offset |
| Power long-press | Progress is implied by a second haptic tick at 600 ms, then the report |

## Cursor screen

Full-height dashed-outline drag surface, one hint line inside it. Below: Left click and Back as equal keys, then a full-width return-to-keypad key. No visible cursor on the phone — the projector owns that.

## Key Lab screen

Function-under-test card (solid hairline border): function name in condensed 22 sp, candidate index and usage in accent-300 below. Verdict list in a dashed-border box, newest last, function · usage on the left and verdict on the right. Three equal action keys at the bottom: **Send** filled accent, *It worked* and *No effect* outlined.

## Setup screen

Three step cards, hairline-bordered, each with an uppercase step label and one sentence. The standby-test card is dashed, because its answer is unknown. Primary action pinned to the bottom: Connect to A1 / Disconnect.

## Wire log

Not a shipped screen in v1 — it is the diagnostic panel shown beside the prototype. If you build it, put it behind a long-press on the status row: monospace 12 sp, newest first, capped at the last 50 reports, format `mm:ss  Function  Report usage`.
