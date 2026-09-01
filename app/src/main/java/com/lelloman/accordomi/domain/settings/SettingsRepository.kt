package com.lelloman.accordomi.domain.settings

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import com.lelloman.accordomi.domain.tone.DetectionRate
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setReferencePitchHz(referencePitchHz: Double)

    suspend fun setToneDetectionMethod(method: ToneDetectionMethod)

    suspend fun setDetectionRate(rate: DetectionRate)

    suspend fun setToneVisualizationStyle(style: ToneVisualizationStyle)

    suspend fun setSelectedThemeId(themeId: ThemeId)

    suspend fun upsertCustomTheme(theme: CustomTheme)

    suspend fun deleteCustomTheme(themeId: ThemeId)
}
