package com.lelloman.accordomi.domain.tone

import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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

    @Test
    fun mapsMidiZeroAndNegativeMidiNotesToFloorBasedOctaves() {
        assertEquals("C-1", readingForMidi(0).noteName)
        assertEquals("B-2", readingForMidi(-1).noteName)
        assertEquals("C-2", readingForMidi(-12).noteName)
    }

    @Test
    fun mapsBCOctaveBoundaries() {
        assertEquals("B-1", readingForMidi(11).noteName)
        assertEquals("C0", readingForMidi(12).noteName)
        assertEquals("B3", readingForMidi(59).noteName)
        assertEquals("C4", readingForMidi(60).noteName)
    }

    @Test
    fun rejectsInvalidDetectedFrequencies() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach { frequencyHz ->
                assertThrows(IllegalArgumentException::class.java) {
                    TuningMath.readingFor(
                        frequencyHz = frequencyHz,
                        clarity = 1f,
                        referencePitchHz = 440.0,
                    )
                }
            }
    }

    @Test
    fun rejectsInvalidReferencePitches() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach { referencePitchHz ->
                assertThrows(IllegalArgumentException::class.java) {
                    TuningMath.readingFor(
                        frequencyHz = 440.0,
                        clarity = 1f,
                        referencePitchHz = referencePitchHz,
                    )
                }
            }
    }

    private fun readingForMidi(midiNote: Int): PitchReading = TuningMath.readingFor(
        frequencyHz = 440.0 * 2.0.pow((midiNote - 69) / 12.0),
        clarity = 1f,
        referencePitchHz = 440.0,
    )
}
