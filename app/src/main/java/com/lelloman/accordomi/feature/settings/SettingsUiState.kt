package com.lelloman.accordomi.feature.settings

import com.lelloman.accordomi.domain.settings.BuiltInTheme
import com.lelloman.accordomi.domain.settings.CustomTheme
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import com.lelloman.accordomi.domain.tone.DetectionRate

data class SettingsUiState(
    val selectedThemeId: ThemeId = BuiltInTheme.System.id,
    val availableBuiltInThemes: List<BuiltInTheme> = BuiltInTheme.entries,
    val customThemes: List<CustomTheme> = emptyList(),
    val referencePitchHzText: String = "440.0",
    val isReferencePitchValid: Boolean = true,
    val selectedToneDetectionMethod: ToneDetectionMethod = ToneDetectionMethod.Default,
    val availableToneDetectionMethods: List<ToneDetectionMethod> = ToneDetectionMethod.entries,
    val selectedDetectionRate: DetectionRate = DetectionRate.Default,
    val availableDetectionRates: List<DetectionRate> = DetectionRate.entries,
    val selectedToneVisualizationStyle: ToneVisualizationStyle = ToneVisualizationStyle.Default,
    val availableToneVisualizationStyles: List<ToneVisualizationStyle> = ToneVisualizationStyle.entries,
)
