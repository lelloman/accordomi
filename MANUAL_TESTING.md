# Manual testing

Use a physical Android device for checks that cannot be represented faithfully by generated sample buffers.

## Microphone tuner smoke test

1. Install a debug or release APK on a device with a working microphone.
2. Launch Accordomi with microphone permission denied. Confirm that no system prompt opens by itself.
3. Tap **Allow**, grant microphone access, and confirm that listening begins.
4. Play A0, A4, C4, C8, a chromatic passage, and a slightly detuned note on a real piano or a calibrated source. Confirm that the displayed note and direction are plausible and settle promptly.
5. Change each detection method while a steady note is sounding. Confirm that capture continues without an audible or visible restart.
6. Change the reference pitch and visualization. Confirm that the displayed tuning changes immediately and capture continues.
7. Background the app for at least one second, then return. Confirm that listening resumes and permission state is correct.
8. Revoke microphone permission in Android app settings and return. Confirm that the permission explanation appears, **Allow** can retry, and **Open app permissions** returns to the correct system page.
9. On a slower available device, play continuous notes for at least one minute. If the processing warning appears, confirm that it clears after the tuner catches up and that readings do not remain progressively stale.

Record the device model, Android version, audio source, detector method, and any unexpected octave or note transitions when reporting a failure.

## Reference-tone playback

1. With microphone permission denied, open **Tone**. Confirm that A4 is selected and playback works without requesting microphone permission.
2. Start and stop playback; check for stable sound and smooth attack/release. Adjust the device media volume.
3. Use note and octave controls to reach A0 and C8. Verify disabled controls at range limits. Selecting another note should stop playback; press Play to hear the new note.
4. Set A4 to 442 Hz in Settings, return to Tone, and check the displayed and independently measured A4 frequency. Check a different octave as well. Phone speakers may reproduce bass notes poorly; use suitable headphones or external output for those measurements.
5. During playback, switch tabs, press Home, lock the phone, and rotate the device. Verify that playback stops and does not restart automatically. Return and start it explicitly.
6. Have another app take audio focus, including a transient interruption. Confirm that the tone stops and remains stopped after focus returns.
7. Repeatedly play/stop and switch notes quickly. Verify that tones never overlap, playback remains available, and no audio resources are leaked.
8. Check English and Italian layouts, large font size, and landscape scrolling.

These playback checks and the on-device accuracy/performance checks were not run in the 2026-09-08 development session because no device was connected.

## Piano mode

Follow [PIANO_TUNING.md](PIANO_TUNING.md). Verify that a completed calibration note
survives leaving the tab and restarting the app, that a profile retains its A4
reference after a Settings change, and that the displayed target agrees with its
88-note table. Revoke microphone permission or background the app during a take;
recording must stop, with no old cents reading retained. Export a profile ZIP,
inspect its JSON and WAVs, and reproduce the targets with `accordomi-targets`.

On a real piano, compare repeated isolated-string strikes and independently inspect
partial frequencies. Listen to octave quality before relying on the curve across
the instrument. Emulator tests establish UI/integration behavior, not acoustic quality.

## LelloDesign UI pass

- Check the four icon+label destinations and open About from Settings.
- Change Light/Dark/System from Settings → Appearance; restart and verify persistence.
- Verify no theme selector appears in the app bar and no extra presets or custom
  editor appear in Settings. An older saved preset/custom selection follows System.
- During piano tuning, alternate sound/silence and check that the meter and
  bottom actions keep their positions and stale current pitch disappears.
- Open Piano details, return, and verify the note/profile and listening state.
- Record both calibration strikes: confirm the note is saved, the accepted
  state remains visible, and Next note advances only when pressed.
- Check 150% text at a narrow width, in light/dark and English/Italian; content
  may scroll while primary capture/listening actions remain accessible.
