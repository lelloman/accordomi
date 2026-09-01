package com.lelloman.accordomi.data.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlappingAudioFrameBufferTest {
    @Test
    fun waitsForACompleteWindowThenEmitsAtEachHop() {
        val buffer = OverlappingAudioFrameBuffer(frameSize = 8, hopSize = 2)

        assertTrue(buffer.append(shortArrayOf(0, 1, 2), 3).isEmpty())
        val first = buffer.append(shortArrayOf(3, 4, 5, 6, 7), 5)
        val second = buffer.append(shortArrayOf(8, 9), 2)

        assertEquals(1, first.size)
        assertArrayEquals((0..7).normalized(), first.single(), 0f)
        assertEquals(1, second.size)
        assertArrayEquals((2..9).normalized(), second.single(), 0f)
    }

    @Test
    fun handlesPartialReadsAcrossHopBoundaries() {
        val buffer = OverlappingAudioFrameBuffer(frameSize = 4, hopSize = 2)

        val frames = buildList {
            addAll(buffer.append(shortArrayOf(0, 1, 2), 3))
            addAll(buffer.append(shortArrayOf(3, 4, 5), 1))
            addAll(buffer.append(shortArrayOf(4), 1))
            addAll(buffer.append(shortArrayOf(5), 1))
        }

        assertEquals(2, frames.size)
        assertArrayEquals((0..3).normalized(), frames[0], 0f)
        assertArrayEquals((2..5).normalized(), frames[1], 0f)
    }

    private fun IntRange.normalized(): FloatArray =
        map { it / Short.MAX_VALUE.toFloat() }.toFloatArray()
}
