package com.lelloman.accordomi.data.tone

import com.lelloman.accordomi.data.pitch.PitchDetectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PitchStabilizerTest {
    @Test
    fun smoothsSmallFrequencyJumps() {
        val stabilizer = PitchStabilizer()

        stabilizer.update(PitchDetectionResult(frequencyHz = 440.0, clarity = 0.9f))
        val result = stabilizer.update(PitchDetectionResult(frequencyHz = 445.0, clarity = 0.8f))

        assertEquals(441.74, result!!.frequencyHz, 0.1)
    }

    @Test
    fun acceptsAdjacentSemitoneAsNewNote() {
        val stabilizer = PitchStabilizer()

        stabilizer.update(PitchDetectionResult(frequencyHz = 440.0, clarity = 0.9f))
        val result = stabilizer.update(PitchDetectionResult(frequencyHz = 466.16, clarity = 0.8f))

        assertEquals(466.16, result!!.frequencyHz, 0.01)
    }

    @Test
    fun holdsTwoMissingFramesAndClearsOnThird() {
        val stabilizer = PitchStabilizer()

        val initial = stabilizer.update(
            PitchDetectionResult(frequencyHz = 440.0, clarity = 0.9f),
        )

        assertEquals(initial, stabilizer.update(null))
        assertEquals(initial, stabilizer.update(null))
        assertNull(stabilizer.update(null))

        val result = stabilizer.update(PitchDetectionResult(frequencyHz = 445.0, clarity = 0.8f))

        assertEquals(445.0, result!!.frequencyHz, 0.01)
    }

    @Test
    fun smoothsClarityTowardCurrentSignal() {
        val stabilizer = PitchStabilizer()

        stabilizer.update(PitchDetectionResult(frequencyHz = 440.0, clarity = 0.9f))
        val result = stabilizer.update(PitchDetectionResult(frequencyHz = 440.0, clarity = 0.5f))

        assertEquals(0.76f, result!!.clarity, 0.001f)
    }

    @Test
    fun resetClearsPitchAndDropoutState() {
        val stabilizer = PitchStabilizer()

        stabilizer.update(PitchDetectionResult(frequencyHz = 440.0, clarity = 0.9f))
        stabilizer.update(null)
        stabilizer.reset()

        assertNull(stabilizer.update(null))
        val result = stabilizer.update(PitchDetectionResult(frequencyHz = 445.0, clarity = 0.8f))
        assertEquals(445.0, result!!.frequencyHz, 0.0)
    }
}
