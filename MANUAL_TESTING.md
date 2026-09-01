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
