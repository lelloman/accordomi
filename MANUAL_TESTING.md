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
