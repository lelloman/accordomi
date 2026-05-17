package com.lelloman.accordomi.domain.settings

import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import javax.inject.Inject

class UpdateToneDetectionMethodUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(method: ToneDetectionMethod) {
        settingsRepository.setToneDetectionMethod(method)
    }
}

