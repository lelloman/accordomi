# Piano tuning workflow

The **Piano** tab is separate from the existing chromatic tuner. It learns string
inharmonicity, saves an instrument profile, calculates 88 piano-specific targets,
and measures selected notes against those targets. The chromatic tuner retains
its equal-tempered targets and existing detection methods.

## Calibrate your upright

1. Set the desired A4 reference in Settings, then open **Piano → Calibrate a new piano**.
   Enter a name for the instrument. The new profile keeps this reference even if
   you later change the chromatic tuner's settings.
2. Allow microphone access when requested. The app never starts piano recording
   just because you open the tab.
3. Follow the notes: **A0, A1, A2, A3, C4, A4, A5, E6, A6**. Mute the other strings
   of each unison and release the sustain pedal. Keep the phone in one position.
4. Press **Record first strike**, play the displayed note once, and let it decay.
   The app needs three consistent analysis windows. After acceptance, wait for
   silence, press **Record second strike**, and play the same string again.
5. Both strikes must agree. If they do not, both are repeated. Weak or inconsistent
   measurements are rejected rather than turned into a tuning target. A capture
   attempt ends after 12 seconds if no repeatable measurement is found.
6. Each completed note is saved automatically. You can leave and resume calibration;
   an unfinished first/second-strike pair may need to be repeated after closing the app.
   Tap any saved note to remeasure it.
7. Press **Create piano tuning**. Review the stretch curve and optional 88-note table.
   A4's actual first partial is fixed exactly at the profile reference.

The current analyzer searches within ±80 cents of the expected note. If the piano
is farther out of tune or the selected note is wrong, it may reject measurements.
For a rejected bass note, try a clearer isolated strike and another microphone
position. A failure to measure B is not evidence that B is zero.

## Tune using the profile

Select a saved piano and choose the note with the semitone/octave buttons. Press
**Start piano tuning**. The target is the profile's calculated first-partial
frequency, not the nearest equal-tempered frequency. The app uses audible partials
and the profile's B estimate to measure the current first-partial frequency.

The meter displays cents from that piano target, current frequency, and whether
pitch is stable or drifting. The stability check needs at least two seconds of
consecutive usable observations. It measures acoustic pitch movement; it does
not detect hammer removal or establish that a tuning pin is mechanically set.

The screen distinguishes measured notes, interpolation between samples, and notes
outside the sampled range. End regions use the nearest measured B, with a visible
notice. **Measure this note to refine the profile** adds or replaces a measurement;
then recreate the curve. This is especially useful around string/scale transitions.
Very high notes may have too few audible partials to measure B independently; the
normal tuning meter can still use a previously estimated B with limited confidence.

Tune one isolated string to the target, then tune its companion strings by listening
for unison beats. Automated unison-beat measurement is not part of this version.
Leaving the screen or backgrounding the app stops its microphone session.

## Export and reproduce

**Export profile and recordings (ZIP)** uses Android's file picker. The archive
contains `profile.json` and the WAVs from accepted two-strike measurements. JSON
includes reference pitch, model version, measured notes, raw partial observations,
residuals, quality, each observation's WAV filename and start sample, and all 88
targets. WAVs are mono PCM16 at 44.1 kHz. The 65,536-sample windows overlap by 49,152
samples; the saved WAV reconstructs the continuous stream without duplicating overlap.

Profiles and recordings stay in app-private storage until you explicitly export.
The JSON lists missing recordings if a file was unavailable (for example after a
partial backup restore). Failed or cancelled capture attempts are discarded. No
recording is uploaded automatically.

The shared C target generator can reproduce the curve on a desktop:

```sh
cmake -S native -B build/native -DCMAKE_BUILD_TYPE=Release
cmake --build build/native --parallel 2
# CSV columns: midi,B — one accepted measurement per note
build/native/accordomi-targets samples.csv --reference 440 > targets.jsonl
```

To prepare the CSV from an exported `profile.json`:

```python
import csv, json
with open("profile.json") as source:
    profile = json.load(source)
with open("samples.csv", "w", newline="") as destination:
    writer = csv.writer(destination)
    writer.writerow(["midi", "B"])
    writer.writerows((sample["midi"], sample["B"]) for sample in profile["samples"])
print("Use --reference", profile["reference_hz"])
```

Use `accordomi-analyze` and the plotting script described in [native/README.md](native/README.md)
to inspect the exported WAVs independently. The Android and desktop algorithms
share the same C implementation.

## What this first curve does

Model version 1 interpolates `log(B + 1e-8)` between measured notes and holds the
endpoints outside that range. It solves weighted least squares for cents offsets
from ET using 2:1 and 4:2 octave partial relationships, a smoothness penalty and a
soft ET prior in the middle register. A4 is an exact constraint. It does not force
fifths or fourths to be beatless. Invalid/non-monotonic curves or offsets beyond
100 cents are rejected. Weights live separately from spectral DSP in
`native/src/piano_model.c`; the resulting targets and model version are saved.

Calibration repeatability checks limit first-partial variation to 3 cents and B
variation to an equivalent 2 cents at partial 6. These are initial quality gates,
not calibrated confidence intervals. Stability, noise rejection and fitting tests
use synthetic data. The model still needs evaluation on real recordings and by
listening to your instrument; the app does not claim an aurally validated tuning.
Explicit scale-break segmentation, automatic note selection, alternative tuning
styles and automated unison measurement remain future work.

The redesigned screen keeps capture/listening controls above navigation. After both
calibration strikes agree, the note is saved; press **Next note** to advance.
Open **Details** for the stretch curve, target table, diagnostics, remeasurement
and profile/recording export. **Your pianos** is also available there.
