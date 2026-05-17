package com.lelloman.accordomi.domain.settings

import javax.inject.Inject

class UpdateReferencePitchUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(referencePitchHz: Double) {
        settingsRepository.setReferencePitchHz(referencePitchHz)
    }
}

