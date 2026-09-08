package com.lelloman.accordomi.data.pitch

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

class AutoCorrelationPitchDetector @Inject constructor() : PitchDetector {
    override val method = ToneDetectionMethod.AutoCorrelation

    override fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult? {
        if (sampleRate <= 0 || samples.size < MinimumSampleCount || samples.any { !it.isFinite() } || samples.maxOf { abs(it) } < MinimumAmplitude) {
            return null
        }

        val minLag = sampleRate / MaximumFrequencyHz
        val maxLag = minOf(sampleRate / MinimumFrequencyHz, samples.size / 2)
        if (minLag >= maxLag) return null

        val frame = CorrelationFrame(samples)
        val correlations = DoubleArray(maxLag + 1)
        for (lag in minLag..maxLag) {
            val divisor = sqrt(frame.energyA(lag) * frame.energyB(lag))
            correlations[lag] = if (divisor <= 0.0) 0.0 else frame.correlation(lag) / divisor
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
        const val MinimumFrequencyHz = 24
        const val MaximumFrequencyHz = 4_800
        const val MinimumCorrelation = 0.6
        const val PeakThreshold = 0.9
    }
}
