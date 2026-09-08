package com.lelloman.accordomi.data.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceOscillatorTest {
    @Test
    fun staysPhaseContinuousAcrossBuffersAtNonIntegerFrequency() {
        val oscillator = ReferenceOscillator(443.7)
        val first = ShortArray(1024).also { oscillator.fill(it) }
        val second = ShortArray(1024).also { oscillator.fill(it) }
        val whole = ShortArray(2048).also { ReferenceOscillator(443.7).fill(it) }
        assertArrayEquals(whole, first + second)
        for (index in 500 until whole.size) {
            val expected = sin(2 * PI * 443.7 * index / 44100) * 0.2 * Short.MAX_VALUE
            assertEquals(expected, whole[index].toDouble(), 1.0)
        }
    }

    @Test
    fun attackAndReleaseAreBoundedAndEndInSilence() {
        val oscillator = ReferenceOscillator(440.0)
        val attack = ShortArray(1024).also { oscillator.fill(it) }
        val release = ShortArray(1024).also { oscillator.fill(it, releasing = true) }
        assertEquals(0, attack[0].toInt())
        assertTrue((attack + release).all { abs(it.toInt()) <= 6554 })
        assertTrue(release.drop(441).all { it == 0.toShort() })
    }
}
