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
    fun acceptsLargeJumpsAsNewNotes() {
        val stabilizer = PitchStabilizer()

        stabilizer.update(PitchDetectionResult(frequencyHz = 440.0, clarity = 0.9f))
        val result = stabilizer.update(PitchDetectionResult(frequencyHz = 523.25, clarity = 0.8f))

        assertEquals(523.25, result!!.frequencyHz, 0.01)
    }

    @Test
    fun resetsOnMissingPitch() {
        val stabilizer = PitchStabilizer()

        stabilizer.update(PitchDetectionResult(frequencyHz = 440.0, clarity = 0.9f))
        assertNull(stabilizer.update(null))
        val result = stabilizer.update(PitchDetectionResult(frequencyHz = 445.0, clarity = 0.8f))

        assertEquals(445.0, result!!.frequencyHz, 0.01)
    }
}
