package com.lelloman.accordomi.feature.tone

import com.lelloman.accordomi.domain.tone.PitchReading
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle

data class ToneDetectionUiState(
    val hasRecordPermission: Boolean = false,
    val isListening: Boolean = false,
    val reading: PitchReading? = null,
    val visualizationStyle: ToneVisualizationStyle = ToneVisualizationStyle.Default,
    val errorMessage: String? = null,
)
