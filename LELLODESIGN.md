# LelloDesign adoption

Accordomi consumes the published Maven artifact
`com.lelloman:lellodesign-compose:0.2.1` from Fucina. There is no source-module
substitution or vendored copy of the shared controls.

Library implementation commits: `27769ea` (shared controls), `5fbda8e` (step progress)
in `lellodesign` (verified release record `b0bd88e`). The release adds the reusable controls needed by this UI pass.
It passed unit tests, release lint, sample assembly and six emulator tests.
The library sample subsequently rebuilt using the published registry dependency.

## Ownership

| Shared LelloDesign component | Accordomi usage |
| --- | --- |
| `LelloTheme`, palettes, shapes, typography | Green light/dark default; semantic adapter for existing custom and legacy themes |
| `LelloScaffold`, `LelloBottomNavigation`, seam | Responsive app frame; four mobile destinations and real product artwork |
| `LelloAppearanceSelector` | Header Light/Dark/System preference; app settings own persistence |
| `LelloButton`, `LelloOutlinedButton`, `LelloTextButton` | Actions throughout piano, chromatic tuner, tone player and settings |
| `LelloTextField`, `LelloFilterChip` | Profile names, reference pitch and preference choices |
| `LelloSettingsSection`, `LelloPaletteSwatch` | Grouped settings and palette previews |
| `LelloThemeEditorDialog`, color controls | Custom theme editing; Accordomi only maps its persisted palette roles |
| `LelloStepProgress` | Accessible segmented calibration progress |
| `LelloCard`, `LelloAlert`, `LelloState` | Instructions, permissions, failures, completion and profile presentation |

Basic Compose text/layout and Material progress indicators/dividers use the
library's theme, as supported by its API. The pitch meter, piano note selection,
calibration workflow and stretch visualization remain product-specific.

There is no account or service connection in Accordomi, so the optional shared
account/connection controls are intentionally omitted. Existing custom palettes
and saved theme IDs remain supported; Light/Dark/System now select the shared
green palette. Choosing an appearance from the header intentionally replaces the
active custom preset but does not delete it. Calibration reference pitch stays
frozen in the saved piano profile.

## Screens

- Piano tuning reserves the meter even while waiting or paused, shows the actual
  C/profile target, correction guidance, confidence and drift, and fixes note and
  listening actions above navigation. Large text may scroll the content.
- Calibration accepts two separately recorded strikes, saves the note, and waits
  for Next note before advancing. Saved notes survive restarts.
- Piano details contains the stretch curve, target table, partial measurements,
  remeasurement, profile switching and WAV/profile export. Opening/closing it
  preserves the current note. Remeasurement/export remain disabled during capture.
- About is reachable from Settings. Existing tuner and tone functionality remains.
- English and Italian labels cover the added flow and library controls.

## Build access

Fucina requires the existing LAN/VPN, DNS and trusted certificate setup. Gradle
reads `FUCINA_MAVEN_TOKEN`, then `FUCINA_MAVEN_TOKEN_FILE`, then the workstation's
protected `~/.config/fucina/lellodesign-maven-consume.token`. It routes only the
LelloDesign Maven module to this registry. Tokens are never written to the repo
or packaged in the APK. CI should inject the consumer credential.

The application now targets Java 17 bytecode to match the shared library.

## Validation

Use `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
assembleRelease`, then run the instrumentation APK on an emulator.
`PianoRedesignTest` checks the fixed controls and meter, reading loss, details
navigation, and large-text access. It captures real Compose light/dark previews
using deterministic test measurements under the test application's external
files directory (`ui-review/`). These are fixture screenshots, not recordings
of an acoustically validated tuning session.
