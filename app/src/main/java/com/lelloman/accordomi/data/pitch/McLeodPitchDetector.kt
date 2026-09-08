package com.lelloman.accordomi.data.pitch

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import javax.inject.Inject
import kotlin.math.abs

class McLeodPitchDetector @Inject constructor() : PitchDetector {
    override val method = ToneDetectionMethod.McLeod

    override fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult? {
        if (sampleRate <= 0 || samples.size < MinimumSampleCount || samples.any { !it.isFinite() } || samples.maxOf { abs(it) } < MinimumAmplitude) {
            return null
        }

        val minTau = sampleRate / MaximumFrequencyHz
        val maxTau = minOf(sampleRate / MinimumFrequencyHz, samples.size / 2)
        if (minTau >= maxTau) return null

        val frame = CorrelationFrame(samples)
        val nsdf = DoubleArray(maxTau + 1)
        for (tau in 0..maxTau) {
            val divisor = frame.energyA(tau) + frame.energyB(tau)
            nsdf[tau] = if (divisor <= 0.0) 0.0 else 2.0 * frame.correlation(tau) / divisor
        }

        val keyMaxima = findKeyMaxima(
            nsdf = nsdf,
            minTau = minTau,
            maxTau = maxTau,
        )
        val highestKeyMaximum = keyMaxima.maxOfOrNull { nsdf[it] } ?: return null
        val cutoff = highestKeyMaximum * PeakThreshold
        val peakTau = keyMaxima.firstOrNull { tau ->
            nsdf[tau] >= cutoff && nsdf[tau] >= MinimumPeakValue
        } ?: return null

        val refinedTau = parabolicInterpolation(nsdf, peakTau)
        if (refinedTau <= 0.0) return null

        return PitchDetectionResult(
            frequencyHz = sampleRate / refinedTau,
            clarity = nsdf[peakTau].coerceIn(0.0, 1.0).toFloat(),
        )
    }

    private fun findKeyMaxima(
        nsdf: DoubleArray,
        minTau: Int,
        maxTau: Int,
    ): List<Int> {
        val keyMaxima = mutableListOf<Int>()
        var tau = 1

        // Ignore the positive region around tau = 0. It represents the signal
        // correlated with itself, rather than a candidate pitch period.
        while (tau <= maxTau && nsdf[tau] > 0.0) {
            tau++
        }

        while (tau <= maxTau) {
            while (tau <= maxTau && nsdf[tau] <= 0.0) {
                tau++
            }
            if (tau > maxTau) break

            var peakTau = tau
            var peakValue = nsdf[tau]
            while (tau <= maxTau && nsdf[tau] > 0.0) {
                if (nsdf[tau] > peakValue) {
                    peakValue = nsdf[tau]
                    peakTau = tau
                }
                tau++
            }

            if (peakTau >= minTau) {
                keyMaxima += peakTau
            }
        }

        return keyMaxima
    }

    private fun parabolicInterpolation(values: DoubleArray, tau: Int): Double {
        if (tau <= 1 || tau >= values.lastIndex) return tau.toDouble()

        val left = values[tau - 1]
        val center = values[tau]
        val right = values[tau + 1]
        val denominator = left - 2.0 * center + right
        if (denominator == 0.0) return tau.toDouble()

        return tau + (left - right) / (2.0 * denominator)
    }

    private companion object {
        const val MinimumSampleCount = 512
        const val MinimumAmplitude = 0.003f
        const val MinimumFrequencyHz = 24
        const val MaximumFrequencyHz = 4_800
        const val PeakThreshold = 0.85
        const val MinimumPeakValue = 0.6
    }
}
