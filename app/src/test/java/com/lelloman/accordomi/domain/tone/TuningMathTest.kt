package com.lelloman.accordomi.domain.tone

import org.junit.Assert.assertEquals
import org.junit.Test

class TuningMathTest {
    @Test
    fun mapsReferencePitchToA4() {
        val reading = TuningMath.readingFor(
            frequencyHz = 440.0,
            clarity = 1f,
            referencePitchHz = 440.0,
        )

        assertEquals("A4", reading.noteName)
        assertEquals(0.0, reading.centsOff, 0.01)
        assertEquals(440.0, reading.targetFrequencyHz, 0.01)
    }

    @Test
    fun mapsMiddleC() {
        val reading = TuningMath.readingFor(
            frequencyHz = 261.63,
            clarity = 1f,
            referencePitchHz = 440.0,
        )

        assertEquals("C4", reading.noteName)
        assertEquals(261.63, reading.targetFrequencyHz, 0.1)
    }
}

