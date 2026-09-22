package com.lelloman.accordomi.ui.theme

import androidx.lifecycle.ViewModel
import com.lelloman.accordomi.domain.settings.androidTheme
import androidx.lifecycle.viewModelScope
import com.lelloman.accordomi.domain.settings.BuiltInTheme
import com.lelloman.accordomi.domain.settings.ObserveSettingsUseCase
import com.lelloman.accordomi.domain.settings.ThemeId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ActiveThemeUiState(
    val selectedThemeId: ThemeId = BuiltInTheme.System.id,
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    val uiState = observeSettings()
        .map { settings ->
            ActiveThemeUiState(
                selectedThemeId = settings.selectedThemeId.androidTheme().id,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActiveThemeUiState(),
        )
}
