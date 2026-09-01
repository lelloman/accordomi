package com.lelloman.accordomi.domain.settings

import com.lelloman.accordomi.domain.tone.DetectionRate
import javax.inject.Inject

class UpdateDetectionRateUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(rate: DetectionRate) {
        settingsRepository.setDetectionRate(rate)
    }
}
