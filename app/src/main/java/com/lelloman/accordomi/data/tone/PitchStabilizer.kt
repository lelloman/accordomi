package com.lelloman.accordomi.data.tone

import com.lelloman.accordomi.data.pitch.PitchDetectionResult
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

class PitchStabilizer {
    private var previous: PitchDetectionResult? = null
    private var consecutiveMissingFrames = 0

    fun reset() {
        previous = null
        consecutiveMissingFrames = 0
    }

    fun update(result: PitchDetectionResult?): PitchDetectionResult? {
        if (result == null) {
            val previousResult = previous ?: return null
            consecutiveMissingFrames++
            return if (consecutiveMissingFrames <= MaximumHeldMissingFrames) {
                previousResult
            } else {
                reset()
                null
            }
        }
        consecutiveMissingFrames = 0

        val stabilized = previous?.let { previousResult ->
            val distanceCents = 1200.0 * log2(result.frequencyHz / previousResult.frequencyHz)
            if (abs(distanceCents) >= NewNoteThresholdCents) {
                result
            } else {
                PitchDetectionResult(
                    frequencyHz = smoothFrequency(
                        previousHz = previousResult.frequencyHz,
                        currentHz = result.frequencyHz,
                    ),
                    clarity = previousResult.clarity +
                        (result.clarity - previousResult.clarity) * SmoothingAlpha.toFloat(),
                )
            }
        } ?: result

        previous = stabilized
        return stabilized
    }

    private fun smoothFrequency(previousHz: Double, currentHz: Double): Double {
        val ratio = currentHz / previousHz
        return previousHz * ratio.pow(SmoothingAlpha)
    }

    private fun log2(value: Double): Double = ln(value) / ln(2.0)

    private companion object {
        const val SmoothingAlpha = 0.35
        const val NewNoteThresholdCents = 50.0
        const val MaximumHeldMissingFrames = 2
    }
}
