# Shared audio engine and piano measurement CLI

Accordomi's existing YIN, normalized autocorrelation and McLeod detectors,
linear FFT correlation, pitch smoothing/dropout handling, equal-temperament
calculations and reference oscillator now run in this C11 library. Android keeps
capture, playback, scheduling, note-name formatting, settings and UI in Kotlin.
Detector thresholds, interpolation, note confirmation and eight-frame dropout
behavior are preserved. There is no automatic stretch correction.

The new **experimental measurement API** analyzes isolated piano strings using a
known note identity. It does not yet provide piano profiles, target optimization,
a calibration screen, automatic note identification, drift warnings or unison
measurement. These remain separate follow-up milestones after recording validation.

## Build and validate on Linux / macOS

Requires a C11 compiler and CMake 3.22+. Python 3 enables CLI integration tests and
plotting; no Python dependencies are needed. A JDK is needed only for host JNI.
Windows is not currently part of the build/test matrix.

```sh
cmake -S native -B build/native -DCMAKE_BUILD_TYPE=Release
cmake --build build/native --parallel 2
ctest --test-dir build/native --output-on-failure
```

Memory/undefined-behavior checks (supported Clang/GCC hosts):

```sh
cmake -S native -B build/native-sanitize -DAC_SANITIZE=ON -DCMAKE_BUILD_TYPE=Debug
cmake --build build/native-sanitize --parallel 2
ctest --test-dir build/native-sanitize --output-on-failure
```

Android uses NDK `27.0.12077973`, SDK CMake `3.22.1`, and all four default Android
ABIs, with flexible 16 KB page-size support enabled. Gradle's JVM test tasks automatically build/load the host JNI library using
system CMake and the local JDK. The existing regression tests exercise C through
the same Kotlin wrappers as the app. JNI names are retained in minified releases.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
# Requires a device/emulator; includes native loading/detection smoke coverage:
./gradlew connectedDebugAndroidTest
```

## Analyze a recording

```sh
# A2, MIDI 45; JSON Lines, one result per complete analysis window:
build/native/accordomi-analyze A2.wav --midi 45 > A2.jsonl
python3 scripts/plot-piano-measurements.py A2.jsonl A2.svg

# Per-partial CSV; a rejected empty window gets a partial=0 row:
build/native/accordomi-analyze A2.wav --midi 45 --format csv > A2.csv

# Compare the existing chromatic detectors on the identical WAV:
build/native/accordomi-analyze A2.wav --midi 45 --method yin > A2-yin.jsonl
build/native/accordomi-analyze A2.wav --midi 45 --method autocorrelation > A2-ac.jsonl
build/native/accordomi-analyze A2.wav --midi 45 --method mcleod > A2-mcleod.jsonl
```

Options: `--reference 400..480` (default 440), `--channel N` (one-based, default 1),
`--window N`, `--hop N`, `--format jsonl|csv` (CSV only for piano). Channels are
selected, never mixed. Supported WAV: little-endian RIFF, PCM16/24/32 or IEEE
float32, 8–192 kHz. Compressed audio and WAVE_FORMAT_EXTENSIBLE are rejected.

Piano defaults to 65,536 samples / 16,384 hop (1.486 s / 0.372 s at 44.1 kHz).
Existing detectors default to 4,096 / 1,024, matching Android. No resampling or
incomplete-window padding occurs. Exit codes: 0 = at least one usable result,
2 = input/configuration/output error, 3 = no usable results (including short WAVs).
Diagnostics go to stderr so stdout can be redirected safely.

JSONL includes engine version, source path, supplied MIDI note, reference pitch,
sample rate, selected channel, window/hop lengths and exact start sample. Times
are relative to the WAV start, not wall-clock recording timestamps. It retains
measured partial frequencies/amplitudes, model predictions, residuals in cents,
inlier flags and fit quality. CSV is a flattened subset; preserve the WAV and
JSONL for reproducibility. Zero-valued fit fields on rejected frames are not
measurements: always inspect `status` before using them.

## Measurement model and limits

The frequency convention is the **actual first partial** F1:

```
P(n) = n * F1 * sqrt((1 + B*n*n) / (1 + B))
```

The ideal flexible-string frequency would instead be `F1 / sqrt(1+B)`.
The analyzer subtracts DC, applies a Hann window, zero-pads to at least four times
the observation length, and interpolates log-magnitude spectral peaks. Padding
improves interpolation, not physical frequency resolution. Relative amplitude
and local prominence gates suppress weak/noisy peaks. These are not calibrated
microphone SNR measurements.

A bounded grid search assigns up to 24 partial numbers, searching F1 within
**±80 cents** of the supplied ET note and B in **[0, 0.02]**. Out-of-range notes or
stiffness values are unsupported, not proof of a bad instrument. Numbered partials
are then fit with pairwise consensus and relative-error-weighted linear regression
of `(P(n)/n)^2` on `n^2`. Observations beyond 2.5 cents are excluded; at least four
inliers and 60% agreement are required. The public `ac_fit_partials` also accepts
independently measured/numbered observations for offline experiments.

`quality` is a heuristic based on inlier count, fraction and residuals, **not a
probability, confidence interval or proof of a correct B estimate**. Four partials
are a conservative acceptance rule. High treble may be rejected because too few
partials lie below Nyquist, especially with a missing fundamental.

Streaming buffers arbitrary PCM block sizes and produces overlapping window
observations with sample positions. Each window is fitted independently; there is
no cross-window partial identity tracking or calibrated repeatability decision
yet. Plots expose changes over time without hiding them behind smoothing.

Synthetic tests cover the full piano range for all three existing detectors,
numbered-partial contamination, weak/noisy decaying partials, missing fundamentals,
several stiffness values, high-treble rejection, silence/noise rejection and stream
chunk independence. They do **not** establish accuracy on a real piano. Close
unisons, false beats, clipping, strong resonances, device processing and incorrect
partial assignment can still produce misleading fits. Use isolated strings;
unison assessment is not implemented. Low fit error alone is insufficient.

## Recording experiment

1. Record 10–20 notes spanning the keyboard, including both sides of the wound/plain
   string transition. Keep the played note identity and instrument details.
2. Mute other strings of each unison. Keep phone position and capture settings fixed;
   use lossless PCM at 44.1 or 48 kHz. Check for clipping and avoid automatic processing
   when the recording device supports unprocessed capture.
3. Save three separate strikes per note, with several seconds of decay each. Retain
   the attack and inspect later windows; the harness does not automatically select
   a stable decay segment.
4. Export JSONL and SVG for each take. Compare F1 and B across time and repeated
   strikes, rejected windows, available partial count and residual structure.
5. Independently check partial frequencies on a subset of recordings before using
   these estimates to build a tuning curve. Actual phone/room validation is outstanding.

No real piano recordings are included in the repository. The upright's height is
not used as a substitute for measured inharmonicity.

## Integration and ownership

`include/accordomi.h` is the C API; no Android or WAV dependencies enter the engine.
Workspaces and streams have explicit create/destroy ownership and are not shared
between threads. Processing uses preallocated scratch storage. A stream invokes
its callback synchronously; copy results to retain them and do not reenter/destroy
the stream from its callback.

JNI uses managed direct buffers, resized under a lock, for reusable workspaces.
There are no retained JNI array pointers or native handles requiring finalizers.
Pitch/correlation calls share one serialized workspace; piano calls use another.
JNI array copies and result objects can still allocate; allocation-free C does not
mean allocation-free Android. Oscillator/stabilizer state belongs to each Kotlin
instance and should remain confined to its owning session.

`PianoAnalyzer` exposes structured measurements to Android callers; run it on a
worker dispatcher with long windows rather than the UI thread. Current capture
still supplies the legacy 4,096-sample frames and is not wired to this experimental
API. An Android recording/calibration workflow is follow-up work.
