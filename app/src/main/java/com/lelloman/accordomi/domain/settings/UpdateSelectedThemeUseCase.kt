package com.lelloman.accordomi.domain.settings

import javax.inject.Inject

class UpdateSelectedThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(themeId: ThemeId) {
        settingsRepository.setSelectedThemeId(themeId)
    }
}
