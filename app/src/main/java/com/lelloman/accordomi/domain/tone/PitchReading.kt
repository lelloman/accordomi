package com.lelloman.accordomi.domain.tone

data class PitchReading(
    val frequencyHz: Double,
    val clarity: Float,
    val noteName: String,
    val centsOff: Double,
    val targetFrequencyHz: Double,
)

