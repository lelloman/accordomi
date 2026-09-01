package com.lelloman.accordomi.domain.settings

import javax.inject.Inject

class UpsertCustomThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(theme: CustomTheme) {
        settingsRepository.upsertCustomTheme(theme)
    }
}
