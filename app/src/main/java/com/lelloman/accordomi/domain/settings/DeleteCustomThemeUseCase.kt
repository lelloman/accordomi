package com.lelloman.accordomi.domain.settings

import javax.inject.Inject

class DeleteCustomThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(themeId: ThemeId) {
        settingsRepository.deleteCustomTheme(themeId)
    }
}
