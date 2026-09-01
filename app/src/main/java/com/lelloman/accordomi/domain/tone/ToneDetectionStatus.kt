package com.lelloman.accordomi.domain.tone

data class ToneDetectionStatus(
    val reading: PitchReading?,
    val isLagging: Boolean,
)
