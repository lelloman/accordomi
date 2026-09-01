package com.lelloman.accordomi.data.pitch

import kotlin.math.abs
import kotlin.math.ln
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

abstract class PitchDetectorContract {
    protected abstract val detector: PitchDetector

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

    private fun assertDetects(samples: FloatArray, expectedFrequencyHz: Double) {
        val result = detector.detect(samples, PitchSignalFixtures.SampleRate)
        assertNotNull("Expected $expectedFrequencyHz Hz from ${detector.method}", result)
        val centsError = abs(1200.0 * ln(result!!.frequencyHz / expectedFrequencyHz) / ln(2.0))
        assertTrue(
            "Expected $expectedFrequencyHz Hz from ${detector.method}, " +
                "got ${result.frequencyHz} Hz ($centsError cents off)",
            centsError <= MaximumErrorCents,
        )
    }

    private companion object {
        const val MaximumErrorCents = 20.0
    }
}
