package com.lelloman.accordomi.feature.tone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lelloman.accordomi.domain.tone.ObserveToneDetectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ToneDetectionViewModel @Inject constructor(
    observeToneDetection: ObserveToneDetectionUseCase,
) : ViewModel() {
    private val hasRecordPermission = MutableStateFlow(false)

    val uiState = hasRecordPermission
        .flatMapLatest { granted ->
            if (!granted) {
                flowOf(ToneDetectionUiState())
            } else {
                observeToneDetection()
                    .map { reading ->
                        ToneDetectionUiState(
                            hasRecordPermission = true,
                            isListening = true,
                            reading = reading,
                        )
                    }
                    .catch { error ->
                        emit(
                            ToneDetectionUiState(
                                hasRecordPermission = true,
                                errorMessage = error.message ?: "Audio recording failed.",
                            ),
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ToneDetectionUiState(),
        )

    fun onRecordPermissionChanged(granted: Boolean) {
        hasRecordPermission.value = granted
    }
}

