# Design artifacts

Open these in a browser. They are self-contained; nothing to install.

| File | What it is |
| --- | --- |
| `WZATCO A1 Remote — Prototype.dc.html` | **Interactive.** The four screens — Keypad, Cursor, Key Lab, Setup. Tap keys; each press writes the HID report it would transmit into the wire log beside the phone. Dashed keys are the functions not yet confirmed on the A1. This is the layout to build. |
| `WZATCO A1 Remote — Build Spec.dc.html` | The specification as a printable document (same content as `../BUILD_SPEC.md`). |
| `WZATCO_A1_Bluetooth_Remote_Replacement_Technical_Blueprint.docx` | The original 20-section research blueprint the specs derive from. Background and evidence, including the IR and companion-APK fallback paths. |

`support.js`, `doc-page.js` and `_ds/` are the runtime and stylesheet these two HTML files load. Keep them alongside.

The markdown files in `../` are the authority for values. Where the rendering and the markdown disagree, the markdown wins — and the disagreement is a bug worth reporting.
