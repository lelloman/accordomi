package com.lelloman.accordomi.data.pitch

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

class AutoCorrelationPitchDetector @Inject constructor() : PitchDetector {
    override val method = ToneDetectionMethod.AutoCorrelation

    override fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult? {
        if (samples.size < MinimumSampleCount || samples.maxOf { abs(it) } < MinimumAmplitude) {
            return null
        }

        val minLag = sampleRate / MaximumFrequencyHz
        val maxLag = minOf(sampleRate / MinimumFrequencyHz, samples.size / 2)
        if (minLag >= maxLag) return null

        val correlations = DoubleArray(maxLag + 1)

        for (lag in minLag..maxLag) {
            var correlation = 0.0
            var energyA = 0.0
            var energyB = 0.0
            val end = samples.size - lag

            for (index in 0 until end) {
                val a = samples[index].toDouble()
                val b = samples[index + lag].toDouble()
                correlation += a * b
                energyA += a * a
                energyB += b * b
            }

            val normalized = if (energyA == 0.0 || energyB == 0.0) {
                0.0
            } else {
                correlation / sqrt(energyA * energyB)
            }
            correlations[lag] = normalized
        }

        val localMaxima = mutableListOf<Int>()
        for (lag in (minLag + 1) until maxLag) {
            if (
                correlations[lag] >= MinimumCorrelation &&
                correlations[lag] > correlations[lag - 1] &&
                correlations[lag] >= correlations[lag + 1]
            ) {
                localMaxima += lag
            }
        }
        val highestCorrelation = localMaxima.maxOfOrNull { correlations[it] } ?: return null
        val bestLag = localMaxima.firstOrNull {
            correlations[it] >= highestCorrelation * PeakThreshold
        } ?: return null

        val refinedLag = parabolicInterpolation(correlations, bestLag)
        if (refinedLag <= 0.0) return null

        return PitchDetectionResult(
            frequencyHz = sampleRate / refinedLag,
            clarity = correlations[bestLag].coerceIn(0.0, 1.0).toFloat(),
        )
    }

    private fun parabolicInterpolation(values: DoubleArray, lag: Int): Double {
        if (lag <= 1 || lag >= values.lastIndex) return lag.toDouble()

        val left = values[lag - 1]
        val center = values[lag]
        val right = values[lag + 1]
        val denominator = left - 2.0 * center + right
        if (denominator == 0.0) return lag.toDouble()

        return lag + (left - right) / (2.0 * denominator)
    }

    private companion object {
        const val MinimumSampleCount = 512
        const val MinimumAmplitude = 0.01f
        const val MinimumFrequencyHz = 27
        const val MaximumFrequencyHz = 4_200
        const val MinimumCorrelation = 0.6
        const val PeakThreshold = 0.9
    }
}
