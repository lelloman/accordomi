package com.lelloman.accordomi.data.pitch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class YinPitchDetectorTest {
    private val detector = YinPitchDetector()

    @Test
    fun detectsA4FromSineWave() {
        val result = detector.detect(
            samples = sineWave(frequencyHz = 440.0),
            sampleRate = SampleRate,
        )

        assertNotNull(result)
        assertEquals(440.0, result!!.frequencyHz, 1.0)
    }

    @Test
    fun returnsNullForSilence() {
        val result = detector.detect(
            samples = FloatArray(4_096),
            sampleRate = SampleRate,
        )

        assertNull(result)
    }

    private fun sineWave(frequencyHz: Double): FloatArray =
        FloatArray(4_096) { index ->
            sin(2.0 * PI * frequencyHz * index / SampleRate).toFloat()
        }

    private companion object {
        const val SampleRate = 44_100
    }
}

