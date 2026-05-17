package com.lelloman.accordomi.domain.settings

import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import javax.inject.Inject

class UpdateToneVisualizationStyleUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(style: ToneVisualizationStyle) {
        settingsRepository.setToneVisualizationStyle(style)
    }
}

