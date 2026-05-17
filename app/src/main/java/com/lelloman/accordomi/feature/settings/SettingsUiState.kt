package com.lelloman.accordomi.feature.settings

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod

data class SettingsUiState(
    val referencePitchHzText: String = "440.0",
    val isReferencePitchValid: Boolean = true,
    val selectedToneDetectionMethod: ToneDetectionMethod = ToneDetectionMethod.Default,
    val availableToneDetectionMethods: List<ToneDetectionMethod> = ToneDetectionMethod.entries,
)
