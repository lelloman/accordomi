# Pitch detection review — 2026-09-08

The detectors are substantially cheaper after this change and accurate on clean synthetic tones. They are **not yet validated as precision piano tuning tools**: high-register octave mistakes and bias from inharmonic partials remain. No Android device was connected for playback, microphone, thermal, or on-device timing measurements.

## Measured results

`PitchAccuracyAuditTest` sweeps MIDI 21–108 at A4=440 Hz, using 4096 samples at 44.1 kHz. Errors are absolute cents relative to the supplied fundamental (the first partial for the inharmonic fixture). These are deterministic generated signals, not recordings. Pure-tone fixtures start at zero phase; these results do not establish worst-case accuracy over every phase, frequency, amplitude, or instrument.

| Measurement | YIN | Autocorrelation | McLeod |
| --- | ---: | ---: | ---: |
| Original worst pure-tone error | 6.927 cents | 0.574 cents | 0.574 cents |
| Revised worst pure-tone error | 0.567 cents | 0.574 cents | 0.574 cents |
| Revised pure tones within 1 cent | 88/88 | 88/88 | 88/88 |
| Harmonic-rich tones within 5 cents | 88/88 | 87/88 | 88/88 |
| Missing-fundamental tones within 5 cents | 87/88 | 86/88 | 86/88 |
| Decaying harmonic tones within 5 cents | 88/88 | 88/88 | 88/88 |
| Inharmonic tones, B=0.0002, within 1 cent | 6/88 | 6/88 | 6/88 |
| Inharmonic tones, B=0.0002, within 5 cents | 86/88 | 86/88 | 86/88 |

The missing-fundamental outliers are C8 for YIN and A7/C8 for the other two methods, approximately an octave low. Autocorrelation also selects an octave low for harmonic-rich C8. The inharmonic fixture has six decaying, phase-offset partials, excludes components above Nyquist, and is one illustrative stiffness value, not a representative piano dataset. Its first-partial error is also not a measurement of perceived pitch or of an appropriate stretched tuning target.

The added regression tests require all 88 clean piano notes to be within 1 cent at A4=440 Hz. At the supported reference limits, A4=400 and 480 Hz, all 88 notes must be within 2 cents: C8 at A4=480 has approximately 1.2 cents of interpolation bias with fewer than ten samples per period. The older 20-cent robustness tests remain for noise, clipping, attacks, and weak fundamentals; passing those does not imply sub-cent accuracy.

Before optimization, median host JVM detection time was approximately 3.48–3.59 ms/frame. Revised runs measured approximately 0.34–0.56 ms/frame, roughly 6–10 times faster. The audit uses 20 warmup calls and 50 timed calls per method on a fixed 110 Hz harmonic frame. These are informal measurements on this development host under Gradle, not JMH or Android benchmarks; timing is deliberately not a CI assertion.

Run the audit with:

```sh
./gradlew testDebugUnitTest --tests '*PitchAccuracyAuditTest'
```

Output, including outlier notes and median/p95 timing, is in `app/build/test-results/testDebugUnitTest/TEST-com.lelloman.accordomi.data.pitch.PitchAccuracyAuditTest.xml`.

## Implementation findings and changes

- All three detectors previously did direct lag-by-sample loops, approximately O(N × maximumLag). They now share an O(N log N) zero-padded FFT autocorrelation implementation, with prefix sums for the two overlapping energies. Padding to at least twice the frame length prevents circular wraparound. Numerical tests compare correlation, energies, and squared differences against independent direct sums, including non-power-of-two frame lengths.
- The frame mean is removed for autocorrelation and NSDF. YIN's squared differences are unaffected by removing a constant. Constant DC, nonfinite samples, invalid sample rates, silence, and seeded broadband noise have rejection tests.
- YIN still uses cumulative-mean normalization and its threshold to select a period. Final interpolation now uses the unnormalized squared difference rather than the normalized values, removing the measured high-note bias. This remains a practical YIN variant using shrinking overlap; it does not implement every stage or alternative described in the original paper. [YIN publication](https://pubmed.ncbi.nlm.nih.gov/12002874/).
- McLeod retains NSDF positive-lobe peak selection, ignoring the initial zero-lag lobe. Autocorrelation retains normalized local-peak selection. Both use relative peak thresholds that can favor a multiple of the true period when the sampled first peak is weak. The remaining octave outliers are preserved in the audit rather than hidden by changing fixtures. [McLeod's original paper and thesis](https://www.cs.otago.ac.nz/graphics/Geoff/tartini/papers.html).
- Search bounds now cover the piano at both reference-pitch limits, including A0=25 Hz and C8≈4566.6 Hz. Previously the hardcoded 27–4200 Hz range did not cover those settings.
- FFT scratch storage is local to each call, so singleton detectors do not acquire shared mutable state. This costs roughly 160–195 KiB of numerical arrays per analyzed 4096-sample frame. Android allocation/GC and sustained CPU profiling remain necessary; cached workspaces should only be introduced with clear ownership and measured benefit.

Capture still uses a 92.9 ms frame and 23.2 ms hop, so reduced compute time does not remove the observation delay. A0 contributes only about 2.55 periods to a frame at standard reference pitch. Conflation bounds the processing queue; frame skipping and the lag indicator already exist. Smoothing and holding eight missing analyzed frames can make the display feel slow and vary in elapsed duration with the chosen detection rate. A future UX improvement should use elapsed-time dropout limits and measure settling time on real plucks, rather than only tuning detector thresholds.

The current microphone source is Android `MIC`, and device processing and clock accuracy have not been characterized. Compare supported unprocessed capture against `MIC` on actual phones before changing the source globally. Current clarity values describe each method's periodicity score, not a calibrated probability that the displayed note is correct.

## Harmonic analysis: proposed next increment

Harmonics already affect these periodicity detectors; none currently measures or displays individual partials. A spectrum is useful when it helps explain an ambiguous note or guides tuning. Piano partials can lie above exact integer multiples, so assuming a perfectly harmonic spectrum is insufficient. [Anderson and Strong's piano study](https://scholarsarchive.byu.edu/facpub/1002/) documents this inharmonicity and its effect on pitch.

1. **Partials view:** show measured frequency and relative amplitude for the first six to eight partials, with cents departure from a harmonic series and an explicit unstable/insufficient-signal state. Use a Hann-windowed spectrum with interpolated peaks and adaptive longer windows for bass notes. The current autocorrelation FFT is unwindowed; reusing its raw bins directly as a precise spectral display would be misleading. Zero padding interpolates a spectrum but does not add physical resolving power.
2. **Octave cross-check:** compare support for f, f/2, and 2f using several measured partials and agreement across time. Use this to flag ambiguity before automatically correcting a note. Validate against the existing A7/C8 failures, missing fundamentals, attacks, and recordings with multiple strings. A harmonic-product spectrum alone should not become the default based on a few synthetic examples.
3. **Piano mode:** fit first-partial frequency and a stiffness parameter using multiple stable partials. One model is `f_n = n × f_1 × sqrt((1 + B n²) / (1 + B))`. An instrument-specific tuning curve can follow once those estimates are repeatable across the keyboard. Keep equal-tempered tuning as a distinct explicit target; do not silently apply a universal stretch curve.

Before shipping correction or stretch tuning, collect consented real-note recordings with independently measured partial frequencies; sweep noise, phase, decay, stiffness, and close unisons synthetically; measure cents error, octave-error rate, rejection rate, and settling time separately. Profile median/p95 processing time, dropped frames, allocation, and sustained power on a slower Android device. These harmonic features are proposed here, not implemented in this change.
