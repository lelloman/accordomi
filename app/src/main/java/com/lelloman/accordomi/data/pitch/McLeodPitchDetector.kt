package com.lelloman.accordomi.data.pitch

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import javax.inject.Inject
import kotlin.math.abs

class McLeodPitchDetector @Inject constructor() : PitchDetector {
    override val method = ToneDetectionMethod.McLeod

    override fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult? {
        if (samples.size < MinimumSampleCount || samples.maxOf { abs(it) } < MinimumAmplitude) {
            return null
        }

        val minTau = sampleRate / MaximumFrequencyHz
        val maxTau = minOf(sampleRate / MinimumFrequencyHz, samples.size / 2)
        if (minTau >= maxTau) return null

        val nsdf = DoubleArray(maxTau + 1)
        for (tau in 0..maxTau) {
            var acf = 0.0
            var divisor = 0.0
            val end = samples.size - tau
            for (index in 0 until end) {
                val a = samples[index].toDouble()
                val b = samples[index + tau].toDouble()
                acf += a * b
                divisor += a * a + b * b
            }
            nsdf[tau] = if (divisor == 0.0) 0.0 else 2.0 * acf / divisor
        }

        val cutoff = nsdf.maxOrNull()?.times(PeakThreshold) ?: return null
        val peakTau = firstStrongPeak(
            nsdf = nsdf,
            minTau = minTau,
            maxTau = maxTau,
            cutoff = cutoff,
        ) ?: return null

        val refinedTau = parabolicInterpolation(nsdf, peakTau)
        if (refinedTau <= 0.0) return null

        return PitchDetectionResult(
            frequencyHz = sampleRate / refinedTau,
            clarity = nsdf[peakTau].coerceIn(0.0, 1.0).toFloat(),
        )
    }

    private fun firstStrongPeak(
        nsdf: DoubleArray,
        minTau: Int,
        maxTau: Int,
        cutoff: Double,
    ): Int? {
        var tau = minTau
        while (tau < maxTau) {
            while (tau < maxTau && nsdf[tau] <= 0.0) {
                tau++
            }

            var peakTau = tau
            var peakValue = nsdf[tau]
            while (tau < maxTau && nsdf[tau] > 0.0) {
                if (nsdf[tau] > peakValue) {
                    peakValue = nsdf[tau]
                    peakTau = tau
                }
                tau++
            }

            if (peakValue >= cutoff && peakValue >= MinimumPeakValue) {
                return peakTau
            }
        }

        return null
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
        const val MinimumFrequencyHz = 27
        const val MaximumFrequencyHz = 4_200
        const val PeakThreshold = 0.85
        const val MinimumPeakValue = 0.6
    }
}

