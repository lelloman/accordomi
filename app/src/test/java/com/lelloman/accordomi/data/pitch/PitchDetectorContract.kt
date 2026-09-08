package com.lelloman.accordomi.data.pitch

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class PitchDetectorContract {
    protected abstract val detector: PitchDetector

    @Test
    fun detectsEveryPianoKeyWithinOneCent() {
        for (midi in 21..108) {
            val frequency = 440.0 * 2.0.pow((midi - 69) / 12.0)
            assertDetects(PitchSignalFixtures.sine(frequency), frequency, maximumErrorCents = 1.0)
        }
    }

    @Test
    fun coversPianoRangeAtReferencePitchLimits() {
        for (reference in listOf(400.0, 480.0)) {
            for (midi in 21..108) {
                val frequency = reference * 2.0.pow((midi - 69) / 12.0)
                // At C8/A4=480 Hz there are fewer than ten samples per period;
                // three-point interpolation has about 1.2 cents of residual bias.
                assertDetects(PitchSignalFixtures.sine(frequency), frequency, maximumErrorCents = 2.0)
            }
        }
    }

    @Test
    fun rejectsDcNoiseAndInvalidInput() {
        assertNull(detector.detect(FloatArray(4096) { 0.5f }, 44100))
        assertNull(detector.detect(FloatArray(4096) { Float.NaN }, 44100))
        assertNull(detector.detect(FloatArray(4096) { Float.POSITIVE_INFINITY }, 44100))
        assertNull(detector.detect(PitchSignalFixtures.sine(440.0), 0))
        assertNull(detector.detect(PitchSignalFixtures.withSeededNoise(FloatArray(4096), 0.5), 44100))
    }

    @Test
    fun detectsRepresentativeNotesAcrossPianoRange() {
        PitchSignalFixtures.representativePianoFrequencies.forEach { frequencyHz ->
            assertDetects(PitchSignalFixtures.sine(frequencyHz), frequencyHz)
        }
    }

    @Test
    fun detectsChromaticAndDetunedNotes() {
        listOf(277.18, 443.0, 932.33).forEach { frequencyHz ->
            assertDetects(PitchSignalFixtures.sine(frequencyHz), frequencyHz)
        }
    }

    @Test
    fun detectsHarmonicRichTone() {
        assertDetects(PitchSignalFixtures.harmonicTone(110.0), 110.0)
    }

    @Test
    fun detectsToneWithWeakFundamental() {
        assertDetects(PitchSignalFixtures.weakFundamental(110.0), 110.0)
    }

    @Test
    fun detectsToneWithSeededNoise() {
        assertDetects(
            PitchSignalFixtures.withSeededNoise(PitchSignalFixtures.sine(440.0)),
            440.0,
        )
    }

    @Test
    fun detectsToneWithDcOffset() {
        assertDetects(
            PitchSignalFixtures.withDcOffset(PitchSignalFixtures.sine(220.0)),
            220.0,
        )
    }

    @Test
    fun detectsClippedTone() {
        assertDetects(PitchSignalFixtures.clipped(PitchSignalFixtures.sine(440.0)), 440.0)
    }

    @Test
    fun detectsToneAfterTransient() {
        assertDetects(
            PitchSignalFixtures.withTransientPrefix(PitchSignalFixtures.sine(329.63)),
            329.63,
        )
    }

    @Test
    fun rejectsSilenceAndPartialBuffers() {
        assertNull(detector.detect(FloatArray(PitchSignalFixtures.FrameSize), PitchSignalFixtures.SampleRate))
        assertNull(detector.detect(PitchSignalFixtures.sine(440.0, size = 256), PitchSignalFixtures.SampleRate))
        assertNull(detector.detect(floatArrayOf(), PitchSignalFixtures.SampleRate))
    }

    private fun assertDetects(samples: FloatArray, expectedFrequencyHz: Double, maximumErrorCents: Double = MaximumErrorCents) {
        val result = detector.detect(samples, PitchSignalFixtures.SampleRate)
        assertNotNull("Expected $expectedFrequencyHz Hz from ${detector.method}", result)
        val centsError = abs(1200.0 * ln(result!!.frequencyHz / expectedFrequencyHz) / ln(2.0))
        assertTrue(
            "Expected $expectedFrequencyHz Hz from ${detector.method}, " +
                "got ${result.frequencyHz} Hz ($centsError cents off)",
            centsError <= maximumErrorCents,
        )
    }

    private companion object {
        const val MaximumErrorCents = 20.0
    }
}
