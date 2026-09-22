package com.lelloman.accordomi.feature.piano

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lelloman.accordomi.R
import com.lelloman.accordomi.domain.piano.*
import com.lelloman.accordomi.domain.tone.*
import com.lelloman.accordomi.feature.tone.ToneVisualization
import com.lelloman.accordomi.feature.tone.openAppPermissionSettings
import com.lelloman.accordomi.nativeaudio.*

@Composable
fun PianoRoute(viewModel: PianoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::permission)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let(viewModel::export) }
    DisposableEffect(owner, viewModel) {
        fun refresh() { viewModel.permission(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
        refresh()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
            if (event == Lifecycle.Event.ON_STOP) viewModel.stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.stop() }
    }
    PianoScreen(state, PianoActions(
        create = viewModel::create, select = viewModel::select, profiles = viewModel::profiles,
        capture = viewModel::captureTake, stop = viewModel::stop, build = viewModel::buildTuning,
        listen = viewModel::listen, note = viewModel::selectNote, refine = viewModel::refineSelectedNote, recalibrate = viewModel::recalibrate,
        export = { export.launch("piano-profile.zip") },
        permission = { permission.launch(Manifest.permission.RECORD_AUDIO) },
        settings = { context.openAppPermissionSettings() },
    ))
}

data class PianoActions(
    val create: (String) -> Unit = {}, val select: (PianoProfile) -> Unit = {},
    val profiles: () -> Unit = {}, val capture: () -> Unit = {}, val stop: () -> Unit = {},
    val build: () -> Unit = {}, val listen: () -> Unit = {}, val note: (Int) -> Unit = {},
    val refine: () -> Unit = {}, val export: () -> Unit = {}, val permission: () -> Unit = {},
    val settings: () -> Unit = {}, val recalibrate: (Int) -> Unit = {},
)

@Composable
fun PianoScreen(state: PianoUiState, actions: PianoActions) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("piano_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.nav_piano), style = MaterialTheme.typography.headlineMedium)
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("piano_error")) }
            if (state.exported) Text(stringResource(R.string.piano_exported))
            if (state.page == PianoPage.Profiles) Profiles(state, actions)
            else state.profile?.let { profile ->
                OutlinedButton(actions.profiles, enabled = !state.listening && !state.busy) { Text(stringResource(R.string.piano_profiles)) }
                Text(profile.name, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.piano_reference, profile.referenceHz))
                if (!state.hasPermission) {
                    Text(stringResource(R.string.microphone_permission_explanation))
                    Button(actions.permission) { Text(stringResource(R.string.allow_permission)) }
                    TextButton(actions.settings) { Text(stringResource(R.string.open_app_permissions)) }
                }
                if (state.page == PianoPage.Calibration) Calibration(state, actions, profile)
                else Tuning(state, actions, profile)
                OutlinedButton(actions.export, enabled = !state.listening && !state.busy) { Text(stringResource(R.string.piano_export)) }
            }
        }
    }
}

@Composable
private fun Profiles(state: PianoUiState, actions: PianoActions) {
    Text(stringResource(R.string.piano_intro))
    if (!state.loaded && state.error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
    state.profiles.forEach { profile ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(if (profile.ready) R.string.piano_ready_profile else R.string.piano_draft_profile,
                    profile.samples.size, profile.referenceHz))
                Button({ actions.select(profile) }, enabled = !state.busy) {
                    Text(stringResource(if (profile.ready) R.string.piano_tune else R.string.piano_continue))
                }
            }
        }
    }
    var name by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(name, { name = it.take(80) }, label = { Text(stringResource(R.string.piano_name)) },
        modifier = Modifier.fillMaxWidth().testTag("piano_name"), singleLine = true)
    Button({ actions.create(name) }, enabled = state.loaded && !state.busy && name.isNotBlank() && state.profiles.size < 20,
        modifier = Modifier.testTag("piano_new")) { Text(stringResource(R.string.piano_new)) }
    Text(stringResource(R.string.piano_reference_hint), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun Calibration(state: PianoUiState, actions: PianoActions, profile: PianoProfile) {
    val completed = CalibrationNotes.count { note -> profile.samples.any { it.midi == note } }
    Text(stringResource(R.string.piano_progress, completed, CalibrationNotes.size))
    LinearProgressIndicator(progress = { completed.toFloat()/CalibrationNotes.size }, modifier = Modifier.fillMaxWidth())
    val midi = state.calibrationMidi
    if (midi == null) {
        Text(stringResource(R.string.piano_calibration_complete))
        Button(actions.build, enabled = !state.busy, modifier = Modifier.testTag("piano_build")) { Text(stringResource(R.string.piano_build)) }
    } else {
        Text(TuningMath.noteName(midi), style = MaterialTheme.typography.displayMedium, modifier = Modifier.testTag("piano_calibration_note"))
        Text(stringResource(R.string.piano_isolate))
        Text(stringResource(if (state.firstTake == null) R.string.piano_first_instructions else R.string.piano_second_instructions))
        if (state.listening) {
            Text(stringResource(R.string.piano_play_now, TuningMath.noteName(midi)))
            Text(stringResource(R.string.piano_windows, state.collectedWindows))
            OutlinedButton(actions.stop) { Text(stringResource(R.string.piano_stop)) }
        } else Button(actions.capture, enabled = state.hasPermission && !state.busy, modifier = Modifier.testTag("piano_record")) {
            Text(stringResource(if (state.firstTake == null) R.string.piano_record_first else R.string.piano_record_second))
        }
        state.measurement?.let { MeasurementDetails(it) }
    }
    if (profile.samples.isNotEmpty()) {
        Text(stringResource(R.string.piano_saved_notes), style = MaterialTheme.typography.titleSmall)
        profile.samples.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { sample -> TextButton({ actions.recalibrate(sample.midi) }, enabled = !state.listening && !state.busy) {
                    Text(TuningMath.noteName(sample.midi))
                } }
            }
        }
    }
}

@Composable
private fun Tuning(state: PianoUiState, actions: PianoActions, profile: PianoProfile) {
    val target = profile.targets.find { it.midi == state.midi } ?: return
    Text(TuningMath.noteName(state.midi), style = MaterialTheme.typography.displayMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        OutlinedButton({ actions.note(state.midi-12) }, enabled = state.midi > 21) { Text("−12") }
        OutlinedButton({ actions.note(state.midi-1) }, enabled = state.midi > 21) { Text("−1") }
        OutlinedButton({ actions.note(state.midi+1) }, enabled = state.midi < 108) { Text("+1") }
        OutlinedButton({ actions.note(state.midi+12) }, enabled = state.midi < 108) { Text("+12") }
    }
    Text(stringResource(R.string.piano_target, target.frequencyHz), modifier = Modifier.testTag("piano_target"))
    val stretch = NativeAudio.cents(target.frequencyHz, NativeAudio.equalTemperedHz(state.midi, profile.referenceHz))
    Text(stringResource(R.string.piano_stretch, stretch))
    val min = profile.samples.minOf { it.midi }; val max = profile.samples.maxOf { it.midi }
    Text(stringResource(when {
        profile.samples.any { it.midi == state.midi } -> R.string.piano_measured
        state.midi !in min..max -> R.string.piano_outside_samples
        else -> R.string.piano_interpolated
    }), style = MaterialTheme.typography.bodySmall)
    val measurement = state.measurement
    if (state.cents != null && measurement != null) {
        ToneVisualization(PitchReading(measurement.firstPartialHz, measurement.quality.toFloat(), TuningMath.noteName(state.midi), state.cents, target.frequencyHz),
            ToneVisualizationStyle.Needle, Modifier.fillMaxWidth().height(210.dp))
        Text(stringResource(R.string.piano_current, measurement.firstPartialHz))
        Text(stringResource(when {
            state.stability == 2 -> R.string.piano_drifting
            state.stability == 1 && kotlin.math.abs(state.cents) <= 1 -> R.string.piano_on_target
            state.stability == 1 -> R.string.piano_stable
            else -> R.string.piano_settling
        }), style = MaterialTheme.typography.titleMedium)
        if (measurement.partials.count { it.used } < 3) Text(stringResource(R.string.piano_low_partials), color = MaterialTheme.colorScheme.tertiary)
    } else Text(stringResource(if (state.listening) R.string.piano_listening else R.string.ready))
    if (state.listening) OutlinedButton(actions.stop) { Text(stringResource(R.string.piano_stop)) }
    else Button(actions.listen, enabled = state.hasPermission && !state.busy, modifier = Modifier.testTag("piano_listen")) { Text(stringResource(R.string.piano_listen)) }
    Text(stringResource(R.string.piano_tuning_instructions), style = MaterialTheme.typography.bodySmall)
    measurement?.let { MeasurementDetails(it) }
    OutlinedButton(actions.refine, enabled = !state.listening && !state.busy) { Text(stringResource(R.string.piano_refine)) }
    Text(stringResource(R.string.piano_curve), style = MaterialTheme.typography.titleMedium)
    StretchCurve(profile)
    Text(stringResource(R.string.piano_curve_explanation), style = MaterialTheme.typography.bodySmall)
    var showTable by rememberSaveable(profile.id) { mutableStateOf(false) }
    TextButton({ showTable = !showTable }) { Text(stringResource(R.string.piano_target_table)) }
    if (showTable) profile.targets.forEach { t ->
        Text(stringResource(R.string.piano_target_row, TuningMath.noteName(t.midi), t.frequencyHz,
            NativeAudio.cents(t.frequencyHz, NativeAudio.equalTemperedHz(t.midi,profile.referenceHz))))
    }
}

@Composable
private fun MeasurementDetails(measurement: PianoMeasurement) {
    if (measurement.status != PianoMeasurementStatus.Usable) {
        Text(stringResource(when (measurement.status) {
            PianoMeasurementStatus.Quiet -> R.string.piano_quiet
            PianoMeasurementStatus.InsufficientPartials -> R.string.piano_insufficient
            else -> R.string.piano_poor_fit
        }))
    } else {
        var expanded by rememberSaveable { mutableStateOf(false) }
        TextButton({ expanded = !expanded }) { Text(stringResource(R.string.piano_diagnostics)) }
        if (expanded) {
            Text(stringResource(R.string.piano_fit, measurement.inharmonicity, measurement.rmsCents))
            measurement.partials.forEach { p -> Text(stringResource(R.string.piano_partial_row, p.number, p.frequencyHz, p.residualCents,
                stringResource(if (p.used) R.string.piano_used else R.string.piano_excluded))) }
        }
    }
}

@Composable
private fun StretchCurve(profile: PianoProfile) {
    val offsets = remember(profile) { profile.targets.map { NativeAudio.cents(it.frequencyHz,NativeAudio.equalTemperedHz(it.midi,profile.referenceHz)).toFloat() } }
    val low = minOf(-1f,offsets.min()); val high = maxOf(1f,offsets.max())
    val line = MaterialTheme.colorScheme.primary; val grid = MaterialTheme.colorScheme.outlineVariant
    val description = stringResource(R.string.piano_curve_range,low,high)
    Text(description, style = MaterialTheme.typography.bodySmall)
    Canvas(Modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description }) {
        fun point(index: Int, value: Float) = Offset(index/87f*size.width, (high-value)/(high-low)*size.height)
        drawLine(grid,point(0,0f),point(87,0f))
        offsets.zipWithNext().forEachIndexed { index, (a,b) -> drawLine(line,point(index,a),point(index+1,b),strokeWidth = 3f) }
        profile.samples.forEach { sample -> drawCircle(line,4f,point(sample.midi-21,offsets[sample.midi-21])) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("A0"); Text("A4"); Text("C8") }
}
