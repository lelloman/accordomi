package com.lelloman.accordomi.data.pitch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class McLeodPitchDetectorTest {
    private val detector = McLeodPitchDetector()

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
    fun detectsNotesAcrossPianoRange() {
        val pianoNotesHz = listOf(
            27.5,
            55.0,
            110.0,
            220.0,
            329.63,
            440.0,
            880.0,
            1_760.0,
            4_186.01,
        )

        pianoNotesHz.forEach { frequencyHz ->
            val result = detector.detect(
                samples = sineWave(frequencyHz = frequencyHz),
                sampleRate = SampleRate,
            )

            assertNotNull("Expected a result for $frequencyHz Hz", result)
            assertEquals(frequencyHz, result!!.frequencyHz, 1.0)
        }
    }

    @Test
    fun detectsFundamentalFromHarmonicRichWave() {
        val frequencyHz = 110.0
        val samples = FloatArray(4_096) { index ->
            val phase = 2.0 * PI * frequencyHz * index / SampleRate
            (
                0.35 * sin(phase) +
                    0.45 * sin(phase * 2.0) +
                    0.20 * sin(phase * 3.0)
                ).toFloat()
        }

        val result = detector.detect(samples = samples, sampleRate = SampleRate)

        assertNotNull(result)
        assertEquals(frequencyHz, result!!.frequencyHz, 1.0)
    }

    @Test
    fun detectsQuietA4FromSineWave() {
        val result = detector.detect(
            samples = sineWave(frequencyHz = 440.0, amplitude = 0.004f),
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

    private fun sineWave(
        frequencyHz: Double,
        amplitude: Float = 1f,
    ): FloatArray =
        FloatArray(4_096) { index ->
            sin(2.0 * PI * frequencyHz * index / SampleRate).toFloat() * amplitude
        }

    private companion object {
        const val SampleRate = 44_100
    }
}
