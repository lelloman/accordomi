package com.lelloman.accordomi.ui.theme

import androidx.lifecycle.ViewModel
import com.lelloman.accordomi.domain.settings.UpdateSelectedThemeUseCase
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.lelloman.accordomi.domain.settings.BuiltInTheme
import com.lelloman.accordomi.domain.settings.ObserveSettingsUseCase
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.settings.ThemePalette
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ActiveThemeUiState(
    val selectedThemeId: ThemeId = BuiltInTheme.System.id,
    val customPalette: ThemePalette? = null,
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val updateSelectedTheme: UpdateSelectedThemeUseCase,
) : ViewModel() {
    fun selectTheme(id: ThemeId) { viewModelScope.launch { updateSelectedTheme(id) } }

    val uiState = observeSettings()
        .map { settings ->
            ActiveThemeUiState(
                selectedThemeId = settings.selectedThemeId,
                customPalette = settings.customThemes
                    .firstOrNull { it.id == settings.selectedThemeId }
                    ?.palette,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActiveThemeUiState(),
        )
}
