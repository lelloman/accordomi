package com.lelloman.accordomi.domain.settings

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import com.lelloman.accordomi.domain.tone.DetectionRate

data class AppSettings(
    val referencePitchHz: Double = DefaultReferencePitchHz,
    val toneDetectionMethod: ToneDetectionMethod = ToneDetectionMethod.Default,
    val detectionRate: DetectionRate = DetectionRate.Default,
    val toneVisualizationStyle: ToneVisualizationStyle = ToneVisualizationStyle.Default,
    val selectedThemeId: ThemeId = BuiltInTheme.System.id,
    val customThemes: List<CustomTheme> = emptyList(),
) {
    companion object {
        const val DefaultReferencePitchHz = 440.0
    }
}
