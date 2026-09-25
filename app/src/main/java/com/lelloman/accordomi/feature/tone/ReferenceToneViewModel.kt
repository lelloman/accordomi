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
    val targetFrequencyHz: Double? = null,
    val customFrequencyHz: Double? = null,
) {
    val frequencyHz: Double get() = customFrequencyHz ?: targetFrequencyHz ?: TuningMath.frequencyFor(midiNote, referencePitchHz)
    val noteName: String get() = TuningMath.noteName(midiNote)
}

@HiltViewModel
class ReferenceToneViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val player: ReferenceToneOutput,
) : ViewModel() {
    private data class Tuning(val referenceHz: Double? = null, val targets: Map<Int, Double> = emptyMap())
    private val tuning = MutableStateFlow(Tuning())
    private val note = MutableStateFlow(69)
    private val customFrequency = MutableStateFlow<Double?>(null)
    private val playing = MutableStateFlow(false)
    private val error = MutableStateFlow(false)
    private var playback: Job? = null
    private var generation = 0L
    @Volatile private var stopRequested = false

    val uiState = combine(note, observeSettings(), playing, error, tuning) { midi, settings, active, failed, context ->
        ReferenceToneUiState(midi, context.referenceHz ?: settings.referencePitchHz, active, failed, context.targets[midi])
    }.combine(customFrequency) { state, frequency ->
        state.copy(customFrequencyHz = frequency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), ReferenceToneUiState())

    fun configure(midi: Int?, referenceHz: Double?, targets: Map<Int, Double>) {
        stopImmediately()
        tuning.value = Tuning(referenceHz, targets.toMap())
        customFrequency.value = null
        if (midi != null) note.value = midi.coerceIn(21, 108)
    }

    fun selectNote(midi: Int) {
        stopImmediately()
        note.value = midi.coerceIn(21, 108)
        customFrequency.value = null
    }

    fun setFrequency(frequencyHz: Double) {
        if (!frequencyHz.isFinite() || frequencyHz !in 20.0..20_000.0) return
        stopImmediately()
        customFrequency.value = frequencyHz
    }

    fun togglePlayback() {
        if (playing.value) {
            stopRequested = true
            return
        }
        error.value = false
        stopRequested = false
        playing.value = true
        val frequency = customFrequency.value ?: tuning.value.targets[note.value]
            ?: TuningMath.frequencyFor(note.value, tuning.value.referenceHz ?: uiState.value.referencePitchHz)
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
