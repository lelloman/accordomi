package com.lelloman.accordomi.feature.piano

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lelloman.accordomi.R
import com.lelloman.accordomi.core.di.DefaultDispatcher
import com.lelloman.accordomi.data.piano.PianoExportService
import com.lelloman.accordomi.domain.piano.*
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.nativeaudio.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class PianoPage { Profiles, Calibration, Tuning }
data class CalibrationTake(val summary: List<Double>, val observations: List<PianoMeasurement>, val recording: String)
data class PianoUiState(
    val profiles: List<PianoProfile> = emptyList(), val profile: PianoProfile? = null,
    val page: PianoPage = PianoPage.Profiles, val hasPermission: Boolean = false,
    val loaded: Boolean = false, val busy: Boolean = false, val listening: Boolean = false,
    val midi: Int = 69, val overrideCalibrationMidi: Int? = null,
    val firstTake: CalibrationTake? = null, val measurement: PianoMeasurement? = null,
    val collectedWindows: Int = 0, val cents: Double? = null, val stability: Int = 0,
    val error: Int? = null, val exported: Boolean = false,
) {
    val calibrationMidi: Int? get() = overrideCalibrationMidi ?: CalibrationNotes.firstOrNull { note -> profile?.samples?.none { it.midi == note } != false }
}

@HiltViewModel
class PianoViewModel @Inject constructor(
    private val repository: PianoProfiles,
    private val source: PianoCaptureSource,
    private val settings: SettingsRepository,
    private val exporter: PianoExportService,
    @param:DefaultDispatcher private val worker: CoroutineDispatcher,
) : ViewModel() {
    private val state = MutableStateFlow(PianoUiState())
    val uiState = state.asStateFlow()
    private var captureJob: Job? = null
    private var generation = 0L

    init {
        viewModelScope.launch {
            repository.profiles.catch { state.update { it.copy(error = R.string.piano_storage_error) } }.collect { profiles ->
                state.update { current -> current.copy(profiles = profiles, loaded = true,
                    profile = current.profile?.let { active -> profiles.find { it.id == active.id } ?: active }) }
            }
        }
    }
    private fun discardPendingTake() {
        val recording = state.value.firstTake?.recording ?: return
        if (state.value.profiles.none { p -> p.samples.any { recording in it.recordings } })
            viewModelScope.launch { runCatching { source.discard(recording) } }
    }
    fun permission(granted: Boolean) {
        if (!granted) stop()
        state.update { it.copy(hasPermission = granted) }
    }
    fun stop() {
        generation++; captureJob?.cancel()
        state.update { it.copy(listening = false, measurement = null, cents = null, stability = 0, collectedWindows = 0) }
    }
    fun profiles() { discardPendingTake(); stop(); state.update { it.copy(page = PianoPage.Profiles, firstTake = null, error = null) } }
    fun select(profile: PianoProfile) {
        discardPendingTake(); stop()
        state.update { it.copy(profile = profile, page = if (profile.ready) PianoPage.Tuning else PianoPage.Calibration,
            firstTake = null, overrideCalibrationMidi = null, error = null, midi = 69) }
    }
    fun create(name: String) {
        if (state.value.busy || !state.value.loaded || name.isBlank()) return
        stop(); state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val reference = settings.settings.first().referencePitchHz
                val profile = PianoProfile(UUID.randomUUID().toString(), name.trim().take(80), reference, System.currentTimeMillis())
                repository.save(profile); select(profile)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(error = R.string.piano_storage_error) } }
            finally { state.update { it.copy(busy = false) } }
        }
    }
    fun selectNote(midi: Int) {
        val resume = state.value.listening
        stop(); state.update { it.copy(midi = midi.coerceIn(21,108), error = null) }
        if (resume) listen()
    }
    fun refineSelectedNote() = recalibrate(state.value.midi)
    fun recalibrate(midi: Int) {
        if (state.value.listening || state.value.busy || midi !in 21..108) return
        discardPendingTake(); stop(); state.update { it.copy(page = PianoPage.Calibration, overrideCalibrationMidi = midi, firstTake = null, error = null) }
    }
    fun captureTake() {
        val before = state.value
        val profile = before.profile ?: return
        val midi = before.calibrationMidi ?: return
        if (!before.hasPermission || before.listening || before.busy) return
        val previous = captureJob; val token = ++generation
        state.update { it.copy(listening = true, error = null, measurement = null, collectedWindows = 0) }
        captureJob = viewModelScope.launch {
            val recording = "${profile.id}-$midi-${UUID.randomUUID()}.wav"
            var retained = false
            try {
                previous?.join()
                val observations = mutableListOf<PianoMeasurement>()
                var lastSequence: Long? = null
                var summary: DoubleArray? = null
                withTimeout(12_000) {
                    source.readings(NativeAudio.equalTemperedHz(midi,profile.referenceHz), recording = recording).first { frame ->
                        if (token != generation) throw CancellationException()
                        val m = frame.measurement
                        if (lastSequence?.let { frame.sequence != it + 1 } == true) observations.clear()
                        lastSequence = frame.sequence
                        state.update { it.copy(measurement = m) }
                        if (m.status != PianoMeasurementStatus.Usable || m.quality < .45) observations.clear()
                        else {
                            observations += m
                            if (observations.size > 3) observations.removeAt(0)
                        }
                        state.update { it.copy(collectedWindows = observations.size) }
                        if (observations.size == 3) summary = summarize(observations)
                        summary != null
                    }
                }
                val take = CalibrationTake(requireNotNull(summary).toList(), observations.toList(), recording)
                val first = state.value.firstTake
                if (first == null) {
                    retained = true
                    state.update { it.copy(firstTake = take, error = null) }
                } else {
                    val combined = NativeAudio.calibrationSummary(
                        doubleArrayOf(first.summary[0], take.summary[0]), doubleArrayOf(first.summary[1], take.summary[1]),
                        doubleArrayOf(first.summary[2], take.summary[2]))
                    if (combined == null) {
                        source.discard(first.recording)
                        state.update { it.copy(firstTake = null, error = R.string.piano_repeat_error) }
                    } else {
                        val sample = PianoSample(midi, combined[0], combined[1], combined[2], System.currentTimeMillis(),
                            listOf(first.recording, recording), first.observations + take.observations)
                        val updated = profile.copy(samples = (profile.samples.filterNot { it.midi == midi } + sample).sortedBy { it.midi }, targets = emptyList())
                        // Once a paired take is being committed, cancellation must not
                        // delete its WAV after DataStore has persisted the filename.
                        withContext(NonCancellable) {
                            repository.save(updated); retained = true
                            state.update { current ->
                                if (current.profile?.id == updated.id) current.copy(profile = updated,
                                    firstTake = null, overrideCalibrationMidi = null, measurement = null)
                                else current
                            }
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) { if (token == generation) state.update { it.copy(error = R.string.piano_capture_timeout) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (token == generation) state.update { it.copy(error = R.string.piano_capture_error) } }
            finally {
                if (!retained) withContext(NonCancellable) { runCatching { source.discard(recording) } }
                if (token == generation) state.update { it.copy(listening = false, collectedWindows = 0) }
            }
        }
    }
    private fun summarize(observations: List<PianoMeasurement>) = NativeAudio.calibrationSummary(
        observations.map { it.firstPartialHz }.toDoubleArray(), observations.map { it.inharmonicity }.toDoubleArray(),
        observations.map { it.quality }.toDoubleArray())

    fun buildTuning() {
        val profile = state.value.profile ?: return
        if (state.value.busy || state.value.listening || CalibrationNotes.any { note -> profile.samples.none { it.midi == note } }) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val samples = profile.samples.sortedBy { it.midi }
                val result = withContext(worker) { NativeAudio.pianoTargets(samples.map { it.midi }.toIntArray(),
                    samples.map { it.inharmonicity }.toDoubleArray(),profile.referenceHz) }
                if (result == null) state.update { it.copy(error = R.string.piano_curve_error) }
                else {
                    val updated = profile.copy(targets = List(88) { PianoTarget(it+21,result[it+88],result[it]) })
                    repository.save(updated); select(updated)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(error = R.string.piano_storage_error) } }
            finally { state.update { it.copy(busy = false) } }
        }
    }
    fun listen() {
        val before = state.value
        val target = before.profile?.targets?.find { it.midi == before.midi } ?: return
        if (!before.hasPermission || before.listening || before.busy) return
        val previous = captureJob; val token = ++generation
        state.update { it.copy(listening = true, error = null, measurement = null, cents = null) }
        captureJob = viewModelScope.launch {
            val history = mutableListOf<PianoFrame>()
            try {
                previous?.join()
                source.readings(target.frequencyHz,target.inharmonicity).collect { frame ->
                    if (token != generation) throw CancellationException()
                    val usable = frame.measurement.status == PianoMeasurementStatus.Usable
                    if (!usable || history.lastOrNull()?.let { frame.sequence != it.sequence+1 } == true) history.clear()
                    if (usable) { history += frame; if (history.size > 9) history.removeAt(0) }
                    state.update { it.copy(measurement = frame.measurement,
                        cents = if (usable) NativeAudio.cents(frame.measurement.firstPartialHz,target.frequencyHz) else null,
                        stability = NativeAudio.stability(history.map { it.measurement.firstPartialHz }.toDoubleArray(), history.map { it.seconds }.toDoubleArray())) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (token == generation) state.update { it.copy(error = R.string.piano_capture_error) } }
            finally { if (token == generation) state.update { it.copy(listening = false, cents = null, stability = 0) } }
        }
    }
    fun export(uri: Uri) {
        val profile = state.value.profile ?: return
        state.update { it.copy(busy = true, error = null, exported = false) }
        viewModelScope.launch {
            try { exporter.export(profile,uri); state.update { it.copy(exported = true) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { state.update { it.copy(error = R.string.piano_export_error) } }
            finally { state.update { it.copy(busy = false) } }
        }
    }
}
