package com.lelloman.accordomi.feature.settings

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle

data class SettingsUiState(
    val referencePitchHzText: String = "440.0",
    val isReferencePitchValid: Boolean = true,
    val selectedToneDetectionMethod: ToneDetectionMethod = ToneDetectionMethod.Default,
    val availableToneDetectionMethods: List<ToneDetectionMethod> = ToneDetectionMethod.entries,
    val selectedToneVisualizationStyle: ToneVisualizationStyle = ToneVisualizationStyle.Default,
    val availableToneVisualizationStyles: List<ToneVisualizationStyle> = ToneVisualizationStyle.entries,
)
