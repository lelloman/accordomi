package com.lelloman.accordomi.nativeaudio

import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class NativeAudioTest {
    @Test fun exposesPianoObservationsThroughJni() {
        val samples = FloatArray(65536) { i ->
            (1..10).sumOf { n ->
                val frequency = n * 110.0 * sqrt((1 + 0.0004 * n * n) / 1.0004)
                0.15 / n * sin(2 * PI * frequency * i / 44100 + n * 0.7)
            }.toFloat()
        }
        val result = PianoAnalyzer().analyze(samples, 44100, 110.0)
        assertEquals(PianoMeasurementStatus.Usable, result.status)
        assertEquals(110.0, result.firstPartialHz, 0.01)
        assertEquals(0.0004, result.inharmonicity, 0.00001)
        assertTrue(result.partials.count { it.used } >= 8)
        assertTrue(result.partials.all { it.frequencyHz.isFinite() && it.predictedHz > 0.0 })
        assertEquals(PianoMeasurementStatus.Quiet,
            PianoAnalyzer().analyze(FloatArray(65536), 44100, 110.0).status)
    }

    @Test fun scratchBuffersCanResizeAndBeUsedFromMultipleThreads() {
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (0 until 24).map { iteration ->
                executor.submit<Double> {
                    val count = if (iteration % 2 == 0) 4096 else 8192
                    val samples = FloatArray(count) { (0.5 * sin(2 * PI * 440 * it / 44100)).toFloat() }
                    NativeAudio.detect(samples, 44100, iteration % 3)!![0]
                }
            }
            futures.forEach { assertEquals(440.0, it.get(), 0.1) }
        } finally { executor.shutdownNow() }
    }

    @Test fun rejectsInvalidPianoParameters() {
        val analyzer = PianoAnalyzer()
        assertEquals(PianoMeasurementStatus.InvalidInput,
            analyzer.analyze(FloatArray(4096), 0, 110.0).status)
        assertEquals(PianoMeasurementStatus.InvalidInput,
            analyzer.analyze(FloatArray(4096), 44100, Double.NaN).status)
        assertThrows(IllegalArgumentException::class.java) {
            analyzer.analyze(FloatArray(0), 44100, 110.0)
        }
    }
}
