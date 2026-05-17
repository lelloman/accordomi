package com.lelloman.accordomi.data.pitch

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import javax.inject.Inject
import kotlin.math.abs

class YinPitchDetector @Inject constructor() : PitchDetector {
    override val method = ToneDetectionMethod.Yin

    override fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult? {
        if (samples.size < MinimumSampleCount || samples.maxOf { abs(it) } < MinimumAmplitude) {
            return null
        }

        val minTau = sampleRate / MaximumFrequencyHz
        val maxTau = minOf(sampleRate / MinimumFrequencyHz, samples.size / 2)
        if (minTau >= maxTau) return null

        val yin = DoubleArray(maxTau + 1)
        for (tau in 1..maxTau) {
            var sum = 0.0
            val end = samples.size - tau
            for (index in 0 until end) {
                val delta = samples[index] - samples[index + tau]
                sum += delta * delta
            }
            yin[tau] = sum
        }

        var runningSum = 0.0
        yin[0] = 1.0
        for (tau in 1..maxTau) {
            runningSum += yin[tau]
            yin[tau] = if (runningSum == 0.0) {
                1.0
            } else {
                yin[tau] * tau / runningSum
            }
        }

        var tauEstimate = -1
        for (tau in minTau..maxTau) {
            if (yin[tau] < Threshold) {
                tauEstimate = tau
                while (tauEstimate + 1 <= maxTau && yin[tauEstimate + 1] < yin[tauEstimate]) {
                    tauEstimate++
                }
                break
            }
        }
        if (tauEstimate == -1) return null

        val betterTau = parabolicInterpolation(yin, tauEstimate)
        if (betterTau <= 0.0) return null

        return PitchDetectionResult(
            frequencyHz = sampleRate / betterTau,
            clarity = (1.0 - yin[tauEstimate]).coerceIn(0.0, 1.0).toFloat(),
        )
    }

    private fun parabolicInterpolation(yin: DoubleArray, tau: Int): Double {
        if (tau <= 1 || tau >= yin.lastIndex) return tau.toDouble()

        val left = yin[tau - 1]
        val center = yin[tau]
        val right = yin[tau + 1]
        val denominator = left - 2.0 * center + right
        if (denominator == 0.0) return tau.toDouble()

        return tau + (left - right) / (2.0 * denominator)
    }

    private companion object {
        const val MinimumSampleCount = 512
        const val MinimumAmplitude = 0.003f
        const val MinimumFrequencyHz = 27
        const val MaximumFrequencyHz = 4_200
        const val Threshold = 0.2
    }
}
