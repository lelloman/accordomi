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

    val uiState = combine(
        observeSettings(),
        editedReferencePitchHzText,
    ) { settings, editedText ->
        val text = editedText ?: settings.referencePitchHz.toReferencePitchText()
        SettingsUiState(
            referencePitchHzText = text,
            isReferencePitchValid = text.toDoubleOrNull()?.isValidReferencePitch() == true,
            selectedToneDetectionMethod = settings.toneDetectionMethod,
            selectedToneVisualizationStyle = settings.toneVisualizationStyle,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            referencePitchHzText = AppSettings.DefaultReferencePitchHz.toReferencePitchText(),
        ),
    )

    fun onReferencePitchChanged(value: String) {
        editedReferencePitchHzText.value = value
        val parsed = value.toDoubleOrNull()
        if (parsed != null && parsed.isValidReferencePitch()) {
            viewModelScope.launch {
                updateReferencePitch(parsed)
            }
        }
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

    private fun Double.toReferencePitchText(): String = "%.1f".format(this)

    private fun Double.isValidReferencePitch(): Boolean = this in 400.0..480.0
}
