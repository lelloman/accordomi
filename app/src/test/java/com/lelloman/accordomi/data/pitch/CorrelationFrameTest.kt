package com.lelloman.accordomi.data.pitch

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class CorrelationFrameTest {
    @Test
    fun matchesDirectLinearCorrelationAndDifferenceIncludingNonPowerOfTwoFrames() {
        for (size in listOf(513, 4096)) {
            val random = Random(18)
            val samples = FloatArray(size) { random.nextFloat() + 0.4f }
            val mean = samples.sumOf { it.toDouble() } / size
            val centered = DoubleArray(size) { samples[it] - mean }
            val frame = CorrelationFrame(samples)
            for (lag in listOf(0, 1, 10, size / 2, size - 1)) {
                var correlation = 0.0
                var difference = 0.0
                var energyA = 0.0
                var energyB = 0.0
                for (index in 0 until size - lag) {
                    val a = centered[index]
                    val b = centered[index + lag]
                    correlation += a * b
                    difference += (a - b) * (a - b)
                    energyA += a * a
                    energyB += b * b
                }
                assertEquals(correlation, frame.correlation(lag), 1e-8)
                assertEquals(difference, frame.difference(lag), 1e-8)
                assertEquals(energyA, frame.energyA(lag), 1e-8)
                assertEquals(energyB, frame.energyB(lag), 1e-8)
            }
        }
    }
}
