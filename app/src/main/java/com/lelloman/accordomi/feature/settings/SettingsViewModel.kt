package com.lelloman.accordomi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.CustomTheme
import com.lelloman.accordomi.domain.settings.DeleteCustomThemeUseCase
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.settings.ThemePalette
import com.lelloman.accordomi.domain.settings.ObserveSettingsUseCase
import com.lelloman.accordomi.domain.settings.UpdateReferencePitchUseCase
import com.lelloman.accordomi.domain.settings.UpdateDetectionRateUseCase
import com.lelloman.accordomi.domain.settings.UpdateSelectedThemeUseCase
import com.lelloman.accordomi.domain.settings.UpdateToneDetectionMethodUseCase
import com.lelloman.accordomi.domain.settings.UpdateToneVisualizationStyleUseCase
import com.lelloman.accordomi.domain.settings.UpsertCustomThemeUseCase
import com.lelloman.accordomi.domain.settings.customThemeId
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import com.lelloman.accordomi.domain.tone.DetectionRate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val updateReferencePitch: UpdateReferencePitchUseCase,
    private val updateToneDetectionMethod: UpdateToneDetectionMethodUseCase,
    private val updateDetectionRate: UpdateDetectionRateUseCase,
    private val updateToneVisualizationStyle: UpdateToneVisualizationStyleUseCase,
    private val updateSelectedTheme: UpdateSelectedThemeUseCase,
    private val upsertCustomTheme: UpsertCustomThemeUseCase,
    private val deleteCustomTheme: DeleteCustomThemeUseCase,
) : ViewModel() {
    private val editedReferencePitchHzText = MutableStateFlow<String?>(null)
    private val locale = MutableStateFlow(Locale.getDefault())

    val uiState = combine(
        observeSettings(),
        editedReferencePitchHzText,
        locale,
    ) { settings, editedText, currentLocale ->
        val formatter = ReferencePitchNumberFormatter(currentLocale)
        val text = editedText ?: formatter.format(settings.referencePitchHz)
        SettingsUiState(
            selectedThemeId = settings.selectedThemeId,
            customThemes = settings.customThemes,
            referencePitchHzText = text,
            isReferencePitchValid = formatter.parse(text)?.isValidReferencePitch() == true,
            selectedToneDetectionMethod = settings.toneDetectionMethod,
            selectedDetectionRate = settings.detectionRate,
            selectedToneVisualizationStyle = settings.toneVisualizationStyle,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            referencePitchHzText = ReferencePitchNumberFormatter(locale.value).format(
                AppSettings.DefaultReferencePitchHz,
            ),
        ),
    )

    fun onReferencePitchChanged(value: String) {
        editedReferencePitchHzText.value = value
        val parsed = ReferencePitchNumberFormatter(locale.value).parse(value)
        if (parsed != null && parsed.isValidReferencePitch()) {
            viewModelScope.launch {
                updateReferencePitch(parsed)
            }
        }
    }

    fun onLocaleChanged(value: Locale) {
        locale.value = value
    }

    fun onToneDetectionMethodChanged(method: ToneDetectionMethod) {
        viewModelScope.launch {
            updateToneDetectionMethod(method)
        }
    }

    fun onDetectionRateChanged(rate: DetectionRate) {
        viewModelScope.launch {
            updateDetectionRate(rate)
        }
    }

    fun onToneVisualizationStyleChanged(style: ToneVisualizationStyle) {
        viewModelScope.launch {
            updateToneVisualizationStyle(style)
        }
    }

    fun onThemeChanged(themeId: ThemeId) {
        viewModelScope.launch {
            updateSelectedTheme(themeId)
        }
    }

    fun onSaveCustomTheme(
        existingId: ThemeId?,
        name: String,
        palette: ThemePalette,
    ) {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return
        val theme = CustomTheme(
            id = existingId ?: customThemeId(UUID.randomUUID().toString()),
            name = normalizedName,
            palette = palette,
        )
        viewModelScope.launch {
            upsertCustomTheme(theme)
            updateSelectedTheme(theme.id)
        }
    }

    fun onDeleteCustomTheme(themeId: ThemeId) {
        viewModelScope.launch {
            deleteCustomTheme(themeId)
        }
    }

    private fun Double.isValidReferencePitch(): Boolean = this in 400.0..480.0
}
