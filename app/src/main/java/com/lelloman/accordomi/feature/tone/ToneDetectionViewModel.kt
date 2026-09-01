package com.lelloman.accordomi.feature.tone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.ObserveSettingsUseCase
import com.lelloman.accordomi.domain.tone.ObserveToneDetectionUseCase
import com.lelloman.accordomi.domain.tone.ToneDetectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ToneDetectionViewModel @Inject constructor(
    observeToneDetection: ObserveToneDetectionUseCase,
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {
    private val hasRecordPermission = MutableStateFlow(false)
    private val retryGeneration = MutableStateFlow(0L)

    val uiState = hasRecordPermission
        .flatMapLatest { granted ->
            if (!granted) {
                flowOf(ToneDetectionUiState())
            } else {
                retryGeneration.flatMapLatest {
                    observeToneDetection()
                        .combine(observeSettings()) { status: ToneDetectionStatus, settings: AppSettings ->
                            ToneDetectionUiState(
                                hasRecordPermission = true,
                                isListening = true,
                                reading = status.reading,
                                visualizationStyle = settings.toneVisualizationStyle,
                                isLagging = status.isLagging,
                            )
                        }
                        .onStart {
                            emit(
                                ToneDetectionUiState(
                                    hasRecordPermission = true,
                                    isListening = true,
                                ),
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
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = StopTimeoutMillis,
                replayExpirationMillis = 0,
            ),
            initialValue = ToneDetectionUiState(),
        )

    fun onRecordPermissionChanged(granted: Boolean) {
        hasRecordPermission.value = granted
    }

    fun onRetry() {
        retryGeneration.value++
    }

    private companion object {
        const val StopTimeoutMillis = 500L
    }
}
