package com.lelloman.accordomi.feature.tone

import com.lelloman.accordomi.domain.tone.PitchReading

data class ToneDetectionUiState(
    val hasRecordPermission: Boolean = false,
    val isListening: Boolean = false,
    val reading: PitchReading? = null,
    val errorMessage: String? = null,
)

