package com.lelloman.accordomi.domain.settings

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setReferencePitchHz(referencePitchHz: Double)

    suspend fun setToneDetectionMethod(method: ToneDetectionMethod)
}
