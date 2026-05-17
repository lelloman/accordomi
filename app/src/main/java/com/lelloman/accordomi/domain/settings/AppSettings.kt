package com.lelloman.accordomi.domain.settings

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod

data class AppSettings(
    val referencePitchHz: Double = DefaultReferencePitchHz,
    val toneDetectionMethod: ToneDetectionMethod = ToneDetectionMethod.Default,
) {
    companion object {
        const val DefaultReferencePitchHz = 440.0
    }
}
