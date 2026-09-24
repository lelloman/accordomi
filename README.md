# Accordomi

Accordomi is a small Android piano tuner that analyzes live microphone audio on-device. It supports three pitch-detection algorithms, adjustable A4 reference pitch, and text, needle, and side-wheel tuning views.

## Features

- YIN, autocorrelation, and McLeod pitch detection
- Reference-tone playback from A0 to C8, using the configured A4 pitch, with note and octave controls
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
`42d40c85a74bb5d2e5405bc69b385d6e61032c3b`. The generated `normal` flavor
and `paravoidAndroid` flavors both retain `com.lelloman.accordomi` for in-place
distribution changes. The complete shell requires Android 11 (API 30).
The Paravoid toolchain currently uses AGP 8.13.2, Kotlin 2.2.21 and Hilt 2.57.2.
It compiles against SDK 36 while retaining Accordomi's target SDK 37.
The `normalStoreRelease` build type keeps R8 and resource shrinking for LelloStore;
Paravoid's `release` build type uses the unshrunk payload required by the plugin.

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
that opens Paravoid's update controls; the shell bootstrap also opens those controls
if the app payload cannot start.
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

## Production Paravoid release

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
is at `~/.config/accordomi/paravoid-release/baseline-v6`. Use that baseline for
future payload builds targeting the version 6 shell.

The first production pair (version 5) was published. Its original upload receipt is
`~/.config/accordomi/paravoid-release/upload-v5.json`. The generated v5 baseline is
saved alongside it as `baseline-v5`; it applies to payloads for that shell. An API
30 emulator upgraded the published v4 APK
to the exact v5 shell and retained the settings file byte-for-byte (including
442 Hz reference pitch). Physical audio and calibrated piano-profile migration
remain untested. A private copy of the release key and trust policy is stored on
`homelab` under `/mnt/external/homelab/accordomi-release-signing`, covered by the
existing backup path configuration; an actual backup/restore has not been verified.
