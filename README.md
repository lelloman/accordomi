# Accordomi

Accordomi is a small Android piano tuner that analyzes live microphone audio on-device. It supports three pitch-detection algorithms, adjustable A4 reference pitch, and text, needle, and side-wheel tuning views.

## Features

- YIN, autocorrelation, and McLeod pitch detection
- Bottom navigation for Tuner, Piano, Settings, and About
- Reference-tone playback from A0 to C8 inside Tuner and Piano, with note and octave controls; Piano uses the active profile’s reference and tuning targets
- Full piano-range note mapping with configurable reference pitch from 400 to 480 Hz
- Locale-aware reference-pitch input and localized English and Italian interfaces
- Stabilized readings with short-dropout tolerance
- A visible warning when audio processing cannot keep up
- A dedicated Piano tab with guided two-strike calibration and resumable instrument profiles
- Individualized 88-note targets, a stretch curve, manual-note tuning, and pitch-drift indication
- Export of piano profiles, partial observations, and accepted WAV recordings as ZIP
- Explicit microphone permission controls; permission is never requested automatically

Microphone samples are processed locally and are not uploaded. The normal app declares no internet permission; the Paravoid shell uses the network for signed updates.

## Shared C engine and piano measurement tools

The existing detectors, smoothing, tuning calculations and reference oscillator
run in a shared C11 engine through JNI. Current tuner features and equal-tempered
targets are preserved in the chromatic tuner. The dedicated Piano tab uses
measured inharmonicity to calculate and tune against an instrument-specific curve.
See [PIANO_TUNING.md](PIANO_TUNING.md) for the calibration and tuning workflow.
Desktop tools provide WAV analysis, target generation, JSONL/CSV export and SVG diagnostics.

See [native/README.md](native/README.md) for builds, examples, recording guidance,
API ownership and measurement limitations.

## Requirements

- Android Studio with Android SDK 37 installed
- JDK 17 for the Android Gradle Plugin
- Android NDK 27.0.12077973 and SDK CMake 3.22.1
- System CMake 3.22+ and a C compiler for JVM/native tests
- An Android device or emulator running API 29 or newer

## Build and verify

From the repository root:

```bash
./gradlew assembleNormalDebug
./gradlew testNormalDebugUnitTest lintNormalDebug assembleNormalDebugAndroidTest
```

The normal debug APK is produced under `app/build/outputs/apk/normal/debug/`. `assembleNormalDebugAndroidTest` compiles the Compose instrumentation suite; running it requires a connected device or emulator.

## Paravoid packaging

This project expects a sibling `../paravoid-android` checkout at commit
`b1dd76bc3a432bcb2bff78833d3cda3980a702be`. The generated `normal` flavor
and `paravoidAndroid` flavors both retain `com.lelloman.accordomi` for in-place
distribution changes. The complete shell requires Android 11 (API 30).
The Paravoid toolchain currently uses AGP 8.13.2, Kotlin 2.2.21 and Hilt 2.57.2.
It compiles against SDK 36 while retaining Accordomi's target SDK 37.
The `normalStoreRelease` build type keeps Android Gradle Plugin R8 and resource
shrinking. Paravoid rejects those build-type switches but offers a separate,
experimental `paravoidMinifyPayload` flag for its payload DEX.

For a local shell build, generate disposable signing and trust keys, then use the
paths printed by the script:

```bash
python3 scripts/prepare-paravoid-local.py
PARAVOID_SIGNING_KEY="$PWD/app/paravoid/keys/release.der" \
PARAVOID_TRUST_POLICY="$PWD/app/paravoid/keys/trust.json" \
  ./gradlew :app:assembleParavoidAndroidDebug
```

Install `app/build/outputs/paravoid/paravoidAndroidDebug/shell.apk`.
The independently signed payload is
`app/build/outputs/paravoid/paravoidAndroidDebug/payload.vpk`.
The shell has one launcher icon. Settings provides a **Manage app updates** button
that opens Paravoid's shell-owned update/recovery screen. Crash recovery uses the
default signed updater, checks automatically and requires an explicit download
and restart. A durable crash record routes the next launch there if Android
blocks opening the screen immediately.
Local keys are ignored by Git and must not be used for a published release.
For a real update channel, supply publisher-owned keys and trust policy plus
`PARAVOID_UPDATE_BASE_URL` (an HTTPS URL ending in `/`). Set
`PARAVOID_SIGNING_KEY_ID` if the signing key uses an ID other than `accordomi-v1`.
Increment `-PparavoidPayloadVersion=<number>` for every published payload. Review
the generated shell baseline and pass `-PparavoidBaselineDirectory=<directory>` to
enforce it on subsequent builds; see
`../paravoid-android/paravoid-gradle-plugin/COMPLETE-VPK.md`.

For real microphone and lifecycle checks, follow [MANUAL_TESTING.md](MANUAL_TESTING.md).

For measured synthetic accuracy, performance, known limitations, and the proposed harmonic-analysis work, see [ALGORITHM_REVIEW.md](ALGORITHM_REVIEW.md).

## Publish to LelloStore

Configure `signing.properties` using `signing.properties.example`.
The script defaults to `https://store.lelloman.com`, issuer
`https://auth.lelloman.com`, and the existing LelloStore public client ID.
Override these with `LELLOSTORE_URL`, `LELLOSTORE_OIDC_ISSUER`, and
`LELLOSTORE_CLIENT_ID` when targeting a different store.
The wrapper builds the signed Paravoid shell and embedded payload, then delegates
shell validation and draft upload to the shared LelloStore publisher. It uses the
production Paravoid key and trust policy under
`~/.config/accordomi/paravoid-release/` by default; environment variables can
override those paths and the update URL.

```bash
# Build and validate locally without authenticating or uploading:
./scripts/publish-android-to-lellostore.sh --dry-run --json

# Build and upload a Paravoid shell draft, with interactive confirmation:
./scripts/publish-android-to-lellostore.sh

# Build a minified payload against the current shell baseline without uploading:
./scripts/publish-android-to-lellostore.sh --minified-payload-version 15 --build-only

# Upload that signed payload as a draft:
./scripts/publish-android-to-lellostore.sh --minified-payload-version 15
```

The publisher is resolved from `LELLOSTORE_PUBLISHER`, then the sibling
`../lellostore/scripts/publish-to-lellostore.py`, then
`$HOME/lelloprojects/lellostore/scripts/publish-to-lellostore.py`.
All arguments are forwarded, including `--store-url`, `--issuer`, `--client-id`,
`--beta`, and `--yes --json` for an already authorized noninteractive upload.
The uploaded artifact is `app/build/outputs/paravoid/paravoidAndroidRelease/shell.apk`.
The accompanying `payload.vpk` is embedded and registered with the shell.
Review the Store draft and publish it separately with the authoritative publisher.
Before uploading a new shell, increment `versionCode`, `versionName`, and the
Paravoid payload version in `app/build.gradle.kts`; the wrapper does not change
versions automatically.
To publish payloads for the older version 7 shell, use the Accordomi build
configuration from commit `5ea0cc4`, `PARAVOID_BASELINE_DIRECTORY` pointing to
`baseline-v7`, and `PARAVOID_SOURCE_DIRECTORY` pointing to Paravoid revision
`1aef36165a4fc5cd5aa5698a42bda9bddb4f9ac4`.
The baseline check remains mandatory: a Paravoid runtime upgrade requires a new
shell APK and cannot be delivered in a VPK.

The `--minified-payload-version` mode builds only a VPK, checks its signed version
and shell contract against `PARAVOID_BASELINE_DIRECTORY` (default `baseline-v9`),
and calls the publisher's `upload-vpk` command. After the Store validates the
draft, use the authoritative publisher's `publish-vpk` command with the draft's
VPK ID and current publication revision. It does not upload another shell APK.

## Project structure

The app uses a small layered architecture:

- `data/audio`: Android microphone capture and frame sequencing
- `native/`: shared C DSP, tuning math, experimental piano measurements and desktop CLI
- `nativeaudio`: JNI transport and structured piano measurement API
- `data/pitch`: Kotlin adapters to native pitch detectors
- `data/tone`: detection orchestration, lag tracking, and stabilization
- `domain`: settings and tuning calculations
- `feature`: Compose screens and ViewModels
- `ui`: navigation, theme, and stable UI test tags

Settings are stored with Preferences DataStore. Hilt provides application dependencies, and Kotlin Flows connect capture, settings, and UI state.

## Attribution

Third-party artwork attribution is recorded in [NOTICE](NOTICE). The launcher-icon working source is retained in `icon-lab.html`.

The interface uses the published LelloDesign Compose library; see [UI adoption and build access](LELLODESIGN.md).

## Payload p12

Payload p12 restores Tuner / Piano / Settings / About bottom navigation and embeds
reference tones in Tuner and Piano. It targets the existing version 7 shell
(contract `8c6200b7975a98075f0d94b406d35f34c5117aef3dc284f2b0ed94aa687fcae9`)
using Paravoid `1aef361`; it does not replace the installed runtime. The main
checkout retains the Paravoid upgrade for the next shell release.

## Production Paravoid release

Release 1.8 (Android version code 9, embedded minified payload p14) uses Paravoid
`b1dd76b`. It fixes the shell update-controls route when crash recovery is
enabled. The update settings are accessible through **Settings → Manage app
updates**, so automatic checks and downloads can be enabled after installation.
The generated `baseline-candidate` belongs under
`~/.config/accordomi/paravoid-release/baseline-v9/paravoidAndroidRelease`
after publication.

### Version 8 shell

Release 1.7 (Android version code 8, embedded minified payload p13) upgrades the
shell to Paravoid `7f74e20`. It enables the Store's authenticated WebSocket push
endpoint with automatic download behavior and manual restart. Automatic checks
and downloads default to off on fresh installs. Its **Manage app updates** button
routes to the recovery screen, so version 9 is needed to enable those options.
Existing user update preferences migrate from the old shell.
The WebSocket connects while the app is visible. Enabled background checks use
Paravoid's scheduler (six-hour interval); this is not an always-on background
push connection. Automatic downloads use unmetered networks by default.
The published shell's `baseline-candidate` is archived under
`~/.config/accordomi/paravoid-release/baseline-v8/paravoidAndroidRelease` for
future compatible VPK builds. APK signing identity and release/trust keys remain
the same.

Version 8 and its embedded p13 VPK are published on LelloStore. The signed
artifacts, mapping and publication receipts are archived alongside prior releases.

### Version 7 shell

Release 1.6 (Android version code 7, embedded minified payload p11) enables
Paravoid's default crash recovery updater. Uncaught managed crashes record details
and route to a shell-owned screen in a separate process, with automatic update
checks and explicit download/restart actions. Enabling this requires installing
the new shell APK; it cannot be delivered to the version 6 shell as a VPK.
The publishing wrapper builds a minified embedded payload. After publication,
save the generated `baseline-candidate` as
`~/.config/accordomi/paravoid-release/baseline-v7/paravoidAndroidRelease` for
future compatible VPK releases. The new shell keeps the existing APK signing
identity, update endpoint and release/trust keys.

### Previous shell

Release 1.5 (Android version code 6, payload version 6) retains the published
APK signing identity and uses keyed, embedded delivery from
`https://store.lelloman.com/api/paravoid/`. The payload release key is separate
from both the APK keystore and the Store head/grant keys. It lives outside this
repository at `~/.config/accordomi/paravoid-release/accordomi-release-2026.pk8`;
keep a secure backup before relying on future payload releases. Never regenerate
that key under the same key ID. The adjacent `trust.json` pins its public key and
the Store's production head/grant public keys.

```sh
PARAVOID_SIGNING_KEY="$HOME/.config/accordomi/paravoid-release/accordomi-release-2026.pk8" \
PARAVOID_SIGNING_KEY_ID=accordomi-release-2026 \
PARAVOID_TRUST_POLICY="$HOME/.config/accordomi/paravoid-release/trust.json" \
PARAVOID_UPDATE_BASE_URL=https://store.lelloman.com/api/paravoid/ \
  ./gradlew :app:assembleParavoidAndroidRelease
```

The pair is under `app/build/outputs/paravoid/paravoidAndroidRelease/`. The
publishing wrapper uploads `shell.apk` with `--distribution-mode paravoid`; the
embedded `payload.vpk` is registered with it. Upload creates a draft for review.
Version 6 and its embedded payload are published. Its upload receipt is at
`~/.config/accordomi/paravoid-release/upload-v6.json`, and its contract baseline
is at `~/.config/accordomi/paravoid-release/baseline-v6`. Use that baseline only for
payload builds targeting the version 6 shell.
Minified payload p10 is the latest published payload on that shell contract.
It preserves DataStore protobuf fields needed to read saved preferences.
Previously, p8 was published on that shell contract. It repairs p7, which
failed to start after an update because R8 removed runtime entry points. The
shell APK version remains 6. Payload R8 8.6.2-dev emits Kotlin metadata 2.2
compatibility warnings. An emulator test covered p8 download and activation
from the Store-issued version 6 shell, app startup, and Settings. Other app paths
have not been exercised with the minified release.

The first production pair (version 5) was published. Its original upload receipt is
`~/.config/accordomi/paravoid-release/upload-v5.json`. The generated v5 baseline is
saved alongside it as `baseline-v5`; it applies to payloads for that shell. An API
30 emulator upgraded the published v4 APK
to the exact v5 shell and retained the settings file byte-for-byte (including
442 Hz reference pitch). Physical audio and calibrated piano-profile migration
remain untested. A private copy of the release key and trust policy is stored on
`homelab` under `/mnt/external/homelab/accordomi-release-signing`, covered by the
existing backup path configuration; an actual backup/restore has not been verified.
