package com.lelloman.accordomi.data.pitch

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

object PitchSignalFixtures {
    const val SampleRate = 44_100
    const val FrameSize = 4_096

    val representativePianoFrequencies = listOf(
        27.5,
        55.0,
        110.0,
        261.63,
        440.0,
        880.0,
        1_760.0,
        4_186.01,
    )

    fun sine(
        frequencyHz: Double,
        amplitude: Double = 0.8,
        size: Int = FrameSize,
    ): FloatArray = FloatArray(size) { index ->
        (amplitude * sin(phase(frequencyHz, index))).toFloat()
    }

    fun harmonicTone(
        frequencyHz: Double,
        amplitudes: List<Double> = listOf(0.45, 0.3, 0.15, 0.1),
    ): FloatArray = FloatArray(FrameSize) { index ->
        amplitudes.mapIndexed { harmonicIndex, amplitude ->
            amplitude * sin(phase(frequencyHz * (harmonicIndex + 1), index))
        }.sum().toFloat()
    }

    fun weakFundamental(frequencyHz: Double): FloatArray = harmonicTone(
        frequencyHz = frequencyHz,
        amplitudes = listOf(0.12, 0.55, 0.25, 0.08),
    )

    /** Synthetic stiff-string partials; frequencyHz is the first partial, not the ideal string f0. */
    fun decayingPartials(frequencyHz: Double, inharmonicity: Double = 0.0): FloatArray =
        FloatArray(FrameSize) { index ->
            (1..6).sumOf { partial ->
                val frequency = frequencyHz * partial *
                    sqrt((1 + inharmonicity * partial * partial) / (1 + inharmonicity))
                if (frequency >= SampleRate / 2.0) 0.0 else {
                    0.5 / partial * exp(-index.toDouble() / SampleRate * partial * 3) *
                        sin(phase(frequency, index) + partial * 0.37)
                }
            }.toFloat()
        }

    fun withSeededNoise(
        samples: FloatArray,
        amplitude: Double = 0.08,
        seed: Int = 7,
    ): FloatArray {
        val random = Random(seed)
        return FloatArray(samples.size) { index ->
            (samples[index] + random.nextDouble(-amplitude, amplitude)).toFloat()
        }
    }

    fun withDcOffset(samples: FloatArray, offset: Double = 0.2): FloatArray =
        FloatArray(samples.size) { index -> samples[index] + offset.toFloat() }

    fun clipped(samples: FloatArray, limit: Float = 0.45f): FloatArray =
        FloatArray(samples.size) { index -> samples[index].coerceIn(-limit, limit) }

    fun withTransientPrefix(samples: FloatArray, prefixSize: Int = 256): FloatArray =
        samples.copyOf().also { result ->
            val random = Random(19)
            repeat(minOf(prefixSize, result.size)) { index ->
                result[index] = random.nextDouble(-1.0, 1.0).toFloat()
            }
        }

    private fun phase(frequencyHz: Double, index: Int): Double =
        2.0 * PI * frequencyHz * index / SampleRate
}
