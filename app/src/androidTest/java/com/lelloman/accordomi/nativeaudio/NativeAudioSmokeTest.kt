package com.lelloman.accordomi.nativeaudio

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

/** Exercises library packaging and symbol lookup on the device's ABI. */
class NativeAudioSmokeTest {
    @Test fun loadsPackagedLibraryAndRunsEveryDetector() {
        val samples = FloatArray(4096) { (0.5 * sin(2 * PI * 440 * it / 44100)).toFloat() }
        for (method in 0..2) assertEquals(440.0, NativeAudio.detect(samples, 44100, method)!![0], 0.1)
        assertEquals(440.0, NativeAudio.tuning(440.0, 440.0)[1], 0.0)
        assertEquals(PianoMeasurementStatus.Quiet,
            PianoAnalyzer().analyze(FloatArray(4096), 44100, 110.0).status)
    }
}
