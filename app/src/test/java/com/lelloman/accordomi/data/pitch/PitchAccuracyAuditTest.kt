package com.lelloman.accordomi.data.pitch

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import org.junit.Test

/** Reproducible synthetic audit. Timings are host JVM measurements, not Android benchmarks. */
class PitchAccuracyAuditTest {
    @Test
    fun reportAccuracyAndRuntime() {
        for (detector in listOf(YinPitchDetector(), AutoCorrelationPitchDetector(), McLeodPitchDetector())) {
            for ((name, signal) in listOf<Pair<String, (Double) -> FloatArray>>(
                "sine" to { PitchSignalFixtures.sine(it) },
                "harmonics" to { PitchSignalFixtures.harmonicTone(it) },
                "missing fundamental" to { PitchSignalFixtures.harmonicTone(it, listOf(0.0, 0.55, 0.3, 0.15)) },
                "decaying partials" to { PitchSignalFixtures.decayingPartials(it) },
                "inharmonic partials B=0.0002" to { PitchSignalFixtures.decayingPartials(it, 0.0002) },
            )) {
                val errors = (21..108).map { midi ->
                    val frequency = 440.0 * 2.0.pow((midi - 69) / 12.0)
                    val result = detector.detect(signal(frequency), PitchSignalFixtures.SampleRate)
                    val error = result?.let { abs(1200.0 * ln(it.frequencyHz / frequency) / ln(2.0)) }
                        ?: Double.POSITIVE_INFINITY
                    if (error > 5) println("AUDIT outlier ${detector.method} $name MIDI=$midi frequency=$frequency error=$error")
                    error
                }
                println("AUDIT ${detector.method} $name: within 1 cent=${errors.count { it <= 1 }}/88, within 5=${errors.count { it <= 5 }}/88, worst=${errors.max()} cents")
            }
            val samples = PitchSignalFixtures.harmonicTone(110.0)
            repeat(20) { detector.detect(samples, PitchSignalFixtures.SampleRate) }
            val durations = List(50) {
                val start = System.nanoTime()
                detector.detect(samples, PitchSignalFixtures.SampleRate)
                (System.nanoTime() - start) / 1e6
            }.sorted()
            println("AUDIT ${detector.method} runtime median=${durations[25]} ms, p95=${durations[47]} ms")
        }
    }
}
