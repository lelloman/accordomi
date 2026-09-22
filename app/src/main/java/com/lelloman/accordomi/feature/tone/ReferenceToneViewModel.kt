package com.lelloman.accordomi.feature.tone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lelloman.accordomi.domain.tone.ReferenceToneOutput
import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.ObserveSettingsUseCase
import com.lelloman.accordomi.domain.tone.TuningMath
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReferenceToneUiState(
    val midiNote: Int = 69,
    val referencePitchHz: Double = AppSettings.DefaultReferencePitchHz,
    val isPlaying: Boolean = false,
    val hasError: Boolean = false,
) {
    val frequencyHz: Double get() = TuningMath.frequencyFor(midiNote, referencePitchHz)
    val noteName: String get() = TuningMath.readingFor(frequencyHz, 1f, referencePitchHz).noteName
}

@HiltViewModel
class ReferenceToneViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val player: ReferenceToneOutput,
) : ViewModel() {
    private val note = MutableStateFlow(69)
    private val playing = MutableStateFlow(false)
    private val error = MutableStateFlow(false)
    private var playback: Job? = null
    private var generation = 0L
    @Volatile private var stopRequested = false

    val uiState = combine(note, observeSettings(), playing, error) { midi, settings, active, failed ->
        ReferenceToneUiState(midi, settings.referencePitchHz, active, failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), ReferenceToneUiState())

    fun selectNote(midi: Int) {
        stopImmediately()
        note.value = midi.coerceIn(21, 108)
    }

    fun togglePlayback() {
        if (playing.value) {
            stopRequested = true
            return
        }
        error.value = false
        stopRequested = false
        playing.value = true
        val frequency = uiState.value.frequencyHz
        val previous = playback
        val currentGeneration = ++generation
        playback = viewModelScope.launch {
            try {
                previous?.join()
                player.play(frequency) { stopRequested }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == currentGeneration) error.value = true
            } finally {
                if (generation == currentGeneration) playing.value = false
            }
        }
    }

    fun stopImmediately() {
        generation++
        playback?.cancel()
        playing.value = false
    }
}
