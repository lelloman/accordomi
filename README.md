# Accordomi

Accordomi is a small Android piano tuner that analyzes live microphone audio on-device. It supports three pitch-detection algorithms, adjustable A4 reference pitch, and text, needle, and side-wheel tuning views.

## Features

- YIN, autocorrelation, and McLeod pitch detection
- Reference-tone playback from A0 to C8, using the configured A4 pitch, with note and octave controls
- Full piano-range note mapping with configurable reference pitch from 400 to 480 Hz
- Locale-aware reference-pitch input and localized English and Italian interfaces
- Stabilized readings with short-dropout tolerance
- A visible warning when audio processing cannot keep up
- Explicit microphone permission controls; permission is never requested automatically

Microphone samples are processed locally. The app declares no internet permission and does not upload audio.

## Shared C engine and piano measurement tools

The existing detectors, smoothing, tuning calculations and reference oscillator
run in a shared C11 engine through JNI. Current tuner features and equal-tempered
targets are preserved. A desktop WAV CLI adds experimental partial detection,
inharmonicity fitting, JSONL/CSV export and SVG diagnostics. Android also exposes
an additive piano measurement API; a calibration screen and stretch tuning are
not implemented yet.

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
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug assembleDebugAndroidTest
```

The debug APK is produced under `app/build/outputs/apk/debug/`. `assembleDebugAndroidTest` compiles the Compose instrumentation suite; running it requires a connected device or emulator.

For real microphone and lifecycle checks, follow [MANUAL_TESTING.md](MANUAL_TESTING.md).

For measured synthetic accuracy, performance, known limitations, and the proposed harmonic-analysis work, see [ALGORITHM_REVIEW.md](ALGORITHM_REVIEW.md).

## Publish to LelloStore

Configure `signing.properties` using `signing.properties.example`.
The script defaults to `https://store.lelloman.com`, issuer
`https://auth.lelloman.com`, and the existing LelloStore public client ID.
Override these with `LELLOSTORE_URL`, `LELLOSTORE_OIDC_ISSUER`, and
`LELLOSTORE_CLIENT_ID` when targeting a different store.
The wrapper builds the signed release APK and delegates authentication and upload
to the shared LelloStore publisher, matching the other Android projects.

```bash
# Build and validate locally without authenticating or uploading:
./scripts/publish-android-to-lellostore.sh --dry-run --json

# Build and upload, with the publisher's interactive confirmation:
./scripts/publish-android-to-lellostore.sh
```

The publisher is resolved from `LELLOSTORE_PUBLISHER`, then the sibling
`../lellostore/scripts/publish-to-lellostore.py`, then
`$HOME/lelloprojects/lellostore/scripts/publish-to-lellostore.py`.
All arguments are forwarded, including `--store-url`, `--issuer`, `--client-id`,
`--beta`, and `--yes --json` for an already authorized noninteractive upload.
The artifact is `app/build/outputs/apk/release/app-release.apk`.
Before publishing an update, increment `versionCode` and update `versionName`
in `app/build.gradle.kts`; the wrapper does not change versions automatically.

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
