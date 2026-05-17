package com.lelloman.accordomi.data.pitch

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod

data class PitchDetectionResult(
    val frequencyHz: Double,
    val clarity: Float,
)

interface PitchDetector {
    val method: ToneDetectionMethod

    fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult?
}
