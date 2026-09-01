package com.lelloman.accordomi.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.ObserveSettingsUseCase
import com.lelloman.accordomi.domain.settings.UpdateReferencePitchUseCase
import com.lelloman.accordomi.domain.settings.UpdateToneDetectionMethodUseCase
import com.lelloman.accordomi.domain.settings.UpdateToneVisualizationStyleUseCase
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
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
    private val updateToneVisualizationStyle: UpdateToneVisualizationStyleUseCase,
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
            referencePitchHzText = text,
            isReferencePitchValid = formatter.parse(text)?.isValidReferencePitch() == true,
            selectedToneDetectionMethod = settings.toneDetectionMethod,
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

    fun onToneVisualizationStyleChanged(style: ToneVisualizationStyle) {
        viewModelScope.launch {
            updateToneVisualizationStyle(style)
        }
    }

    private fun Double.isValidReferencePitch(): Boolean = this in 400.0..480.0
}
