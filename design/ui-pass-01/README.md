# Accordomi UI pass 01

[Interactive mockups](index.html) · [Light/dark overview](overview.png)

This is a browser-based design prototype, not an Android implementation.
It covers Piano tuning and Calibration in light/dark, with the adopted Gentle
lean mark. Readings, calibration acceptance, profile metadata and the curve are
illustrative. There is no microphone capture or persistence. Numeric note
changes use equal-tempered placeholder values only; production must retain
native piano-profile targets and confidence/stability measurements.

## Decisions

- Four navigation destinations: Tuner, Piano, Tone, Settings. About moves into
  Settings in the proposed implementation. Navigation labels in this prototype
  are visual references, not implemented destinations.
- One app header with the real logo, appearance selector and the LelloDesign
  3:2:1 primary-color seam.
- Large selected note, cents deviation, a persistent meter, and textual
  Raise/Lower/On target/Drifting guidance. No color-only status meaning.
- Target/current frequency are secondary information. Signal quality remains
  visible; full partial diagnostics belong in Piano details.
- Previous/next note and octave controls sit above the listening button.
  They and the bottom navigation remain fixed when content needs scrolling.
- The same meter space is retained for silence, weak signal and paused states.
- Calibration uses the name “Calibrate your piano,” explicit note progress,
  first/second strike states, and one primary action.
- The prototype starts with four notes already saved, on C4 (note five of nine).
  Record advances after a simulated 1.6 seconds. Stop cancels that simulated take.
- Curve, complete target table, remeasurement, diagnostics and export belong in
  the separate Piano details destination. This prototype previews a modal;
  it does not implement those tools or native back navigation.

## LelloDesign reference

Pinned source: `4741327c7221e99191db2373a31dd9274a847c29`.
`palette.css` is an exact export of the resolved `green-light` and `green-dark`
roles from `lellodesign/tokens/colors.json` at that reference.
Control corners are 8 px, panels 12 px, selected navigation 4 px.
Artwork colors remain unchanged across appearances.

Account and connection controls are omitted because Accordomi has no account or
service connection. The green brand palette is the proposed default; existing
custom themes must remain supported when this is implemented in Compose.
The shared Compose library is the starting point for production theme/scaffold
adoption, with existing app-owned navigation and ViewModels retained.

## Review controls and checks

The outer controls simulate six tuning states, 100/125/150% text, and 390/320 px
phone widths. These are review tools and do not belong in the shipped app.
Pause, note/octave changes, Details, appearance and calibration buttons work.
The four mockup states are independent, except the outer tuning-state selector.

Verified in local Chromium:

- Four mockups render with local assets.
- All six tuning states preserve the bottom action position.
- Pause/resume, next note and details opening/closing work.
- Two accepted strikes advance to the next calibration note.
- At 320 px and 150% text the content has no horizontal overflow; it scrolls
  vertically while the actions remain visible.

Native accessibility semantics, TalkBack, localized layouts and Android lifecycle
behavior require validation during implementation. This HTML study does not
replace those checks.
