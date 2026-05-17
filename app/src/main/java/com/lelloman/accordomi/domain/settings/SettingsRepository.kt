package com.lelloman.accordomi.domain.settings

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setReferencePitchHz(referencePitchHz: Double)

    suspend fun setToneDetectionMethod(method: ToneDetectionMethod)

    suspend fun setToneVisualizationStyle(style: ToneVisualizationStyle)
}
