package com.lelloman.accordomi.feature.piano

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.lelloman.accordomi.ui.theme.accordomiColors
import com.lelloman.lellodesign.*
import kotlin.math.abs
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
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
        capture = viewModel::captureTake, next = viewModel::nextCalibrationNote, stop = viewModel::stop, build = viewModel::buildTuning,
        listen = viewModel::listen, note = viewModel::selectNote, refine = viewModel::refineSelectedNote, recalibrate = viewModel::recalibrate,
        export = { export.launch("piano-profile.zip") },
        permission = { permission.launch(Manifest.permission.RECORD_AUDIO) },
        settings = { context.openAppPermissionSettings() },
    ))
}

data class PianoActions(
    val create: (String) -> Unit = {}, val select: (PianoProfile) -> Unit = {},
    val profiles: () -> Unit = {}, val capture: () -> Unit = {}, val next: () -> Unit = {}, val stop: () -> Unit = {},
    val build: () -> Unit = {}, val listen: () -> Unit = {}, val note: (Int) -> Unit = {},
    val refine: () -> Unit = {}, val export: () -> Unit = {}, val permission: () -> Unit = {},
    val settings: () -> Unit = {}, val recalibrate: (Int) -> Unit = {},
)

@Composable
fun PianoScreen(state: PianoUiState, actions: PianoActions) {
    var details by rememberSaveable(state.profile?.id, state.page) { mutableStateOf(false) }
    BackHandler(enabled = details || state.page != PianoPage.Profiles) {
        if (details) details = false else if (!state.busy) actions.profiles()
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().testTag("piano_screen")) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { LelloAlert(stringResource(it), tone = LelloTone.Error, modifier = Modifier.fillMaxWidth()) }
                if (state.exported) LelloAlert(stringResource(R.string.piano_exported), tone = LelloTone.Success)
                if (state.page == PianoPage.Profiles) {
                    Text(stringResource(R.string.piano_profiles), style = MaterialTheme.typography.headlineLarge)
                    Profiles(state, actions)
                } else state.profile?.let { profile ->
                    if (details) {
                        LelloTextButton({ details = false }) { Text(stringResource(R.string.piano_back_to_task)) }
                        PianoDetails(state, actions, profile)
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.piano_reference_short, profile.referenceHz), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LelloTextButton({ details = true }, modifier = Modifier.testTag("piano_details")) { Text(stringResource(R.string.piano_details_action)) }
                        }
                        if (!state.hasPermission) LelloCard(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.microphone_permission_explanation))
                            LelloButton(actions.permission) { Text(stringResource(R.string.allow_permission)) }
                            LelloTextButton(actions.settings) { Text(stringResource(R.string.open_app_permissions)) }
                        }
                        if (state.page == PianoPage.Calibration) CalibrationContent(state, profile)
                        else TuningContent(state, profile)
                    }
                }
            }
            if (!details && state.page != PianoPage.Profiles && state.profile != null) {
                HorizontalDivider()
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp).testTag("piano_actions"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.page == PianoPage.Calibration) CalibrationActions(state, actions)
                    else TuningActions(state, actions)
                }
            }
        }
    }
}

@Composable
private fun CalibrationContent(state: PianoUiState, profile: PianoProfile) {
    val completed = CalibrationNotes.count { note -> profile.samples.any { it.midi == note } }
    Text(stringResource(R.string.piano_calibrate_title), style = MaterialTheme.typography.headlineLarge)
    Text(stringResource(R.string.piano_two_strikes), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(stringResource(R.string.piano_progress, completed, CalibrationNotes.size), style = MaterialTheme.typography.labelLarge)
    LelloStepProgress(completed, CalibrationNotes.size, Modifier.fillMaxWidth(), current = completed.takeIf { it < CalibrationNotes.size })
    val midi = state.calibrationMidi
    if (midi == null) {
        LelloState(stringResource(R.string.piano_calibration_complete), stringResource(R.string.piano_calculate_hint))
    } else {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.piano_play_note), style = MaterialTheme.typography.labelMedium)
            Text(noteLabel(midi), fontSize = 68.sp, lineHeight = 76.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("piano_calibration_note"))
            Text(stringResource(if (state.completedCalibrationMidi != null) R.string.piano_both_accepted else if (state.firstTake == null) R.string.piano_strike_one else R.string.piano_strike_two),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        LelloCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.piano_one_string), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.piano_isolate), style = MaterialTheme.typography.bodyMedium)
        }
        Column(Modifier.heightIn(min = 76.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(when {
                state.completedCalibrationMidi != null -> R.string.piano_note_saved
                state.listening -> R.string.piano_play_now
                state.firstTake != null -> R.string.piano_first_accepted
                else -> R.string.piano_ready_first
            }, TuningMath.noteName(midi)), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(if (state.completedCalibrationMidi != null) R.string.piano_continue_when_ready else if (state.firstTake == null) R.string.piano_first_instructions else R.string.piano_second_instructions),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.listening) Text(stringResource(R.string.piano_windows, state.collectedWindows), style = MaterialTheme.typography.bodySmall)
            state.measurement?.takeIf { it.status != PianoMeasurementStatus.Usable }?.let { MeasurementDetails(it) }
        }
        Text(stringResource(R.string.piano_autosaved), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalibrationActions(state: PianoUiState, actions: PianoActions) {
    if (state.completedCalibrationMidi != null) LelloButton(actions.next, Modifier.fillMaxWidth().testTag("piano_next"), enabled = !state.busy) {
        Text(stringResource(R.string.piano_next_note))
    } else if (state.calibrationMidi == null) LelloButton(actions.build, Modifier.fillMaxWidth().testTag("piano_build"), enabled = !state.busy) {
        Text(stringResource(R.string.piano_build))
    } else if (state.listening) LelloOutlinedButton(actions.stop, Modifier.fillMaxWidth()) { Text(stringResource(R.string.piano_stop)) }
    else LelloButton(actions.capture, Modifier.fillMaxWidth().testTag("piano_record"), enabled = state.hasPermission && !state.busy) {
        Text(stringResource(if (state.firstTake == null) R.string.piano_record_first else R.string.piano_record_second))
    }
}

@Composable
private fun TuningContent(state: PianoUiState, profile: PianoProfile) {
    val target = profile.targets.find { it.midi == state.midi } ?: return
    val cents = state.cents
    val measurement = state.measurement
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.piano_selected_note), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(noteLabel(state.midi), fontSize = 72.sp, lineHeight = 80.sp, fontWeight = FontWeight.Medium)
        val centsLabel = if (cents == null) stringResource(R.string.piano_no_reading) else stringResource(R.string.piano_cents, cents)
        Text(buildAnnotatedString {
            append(centsLabel.substringBeforeLast(' '))
            withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)) { append(" " + centsLabel.substringAfterLast(' ')) }
        },
            style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("piano_cents"))
        PianoMeter(cents)
        val status = when {
            !state.listening -> R.string.piano_paused
            cents == null && measurement?.status == PianoMeasurementStatus.Quiet -> R.string.piano_quiet
            cents == null -> R.string.piano_waiting_note
            state.stability == 2 -> R.string.piano_drifting_short
            abs(cents) <= 1 && state.stability == 1 -> R.string.piano_on_target
            abs(cents) <= 1 -> R.string.piano_settling
            cents < 0 -> R.string.piano_raise
            else -> R.string.piano_lower
        }
        val tone = when {
            state.stability == 2 -> MaterialTheme.accordomiColors.offPitch
            cents != null && abs(cents) <= 1 && state.stability == 1 -> MaterialTheme.accordomiColors.inTune
            else -> MaterialTheme.colorScheme.onSurface
        }
        Column(Modifier.fillMaxWidth().heightIn(min = 76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(status), style = MaterialTheme.typography.titleLarge, color = tone, textAlign = TextAlign.Center,
                modifier = Modifier.testTag("piano_status"))
            Text(stringResource(if (state.stability == 2) R.string.piano_drift_hint else R.string.piano_meter_hint),
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.piano_target_label), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.piano_hz, target.frequencyHz), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("piano_target"))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.piano_current_label), style = MaterialTheme.typography.bodySmall)
                Text(if (cents != null && measurement != null) stringResource(R.string.piano_hz, measurement.firstPartialHz) else "—",
                    style = MaterialTheme.typography.titleMedium)
            }
        }
        HorizontalDivider()
        Text(if (cents == null || measurement == null) stringResource(R.string.piano_signal_waiting)
            else if (measurement.partials.count { it.used } < 3) stringResource(R.string.piano_low_partials)
            else stringResource(R.string.piano_signal_partials, measurement.partials.count { it.used }),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun PianoMeter(cents: Double?) {
    val primary = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outlineVariant
    val band = MaterialTheme.colorScheme.primaryContainer
    val description = stringResource(R.string.piano_meter_description)
    Column(Modifier.fillMaxWidth().height(100.dp).padding(top = 12.dp).testTag("piano_meter").semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left=10.dp.toPx(); val span=size.width-2*left
            val center=size.width/2; val y=size.height*.55f
            drawRoundRect(band, Offset(center-span/40,y-20.dp.toPx()), Size(span/20,40.dp.toPx()), CornerRadius(4.dp.toPx()))
            drawLine(line,Offset(left,y),Offset(size.width-left,y),1.dp.toPx())
            for(i in 0..8) {
                val x=left+span*i/8; val length=if(i==4) 20.dp.toPx() else 7.dp.toPx()
                drawLine(if(i==4) primary else line,Offset(x,y-length),Offset(x,y+length),1.dp.toPx())
            }
            if(cents!=null) {
                val x=left+span*((cents.coerceIn(-20.0,20.0)+20)/40).toFloat()
                drawLine(primary,Offset(x,y-24.dp.toPx()),Offset(x,y+22.dp.toPx()),3.dp.toPx())
                drawCircle(primary,4.dp.toPx(),Offset(x,y-24.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            listOf("−20","−10","0","+10","+20").forEach { Text(it,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun TuningActions(state: PianoUiState, actions: PianoActions) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        listOf(-12,-1,1,12).forEach { step ->
            val enabled = !state.busy && if(step<0) state.midi>21 else state.midi<108
            LelloOutlinedButton({actions.note(state.midi+step)},Modifier.weight(1f),enabled=enabled,contentPadding=PaddingValues(4.dp)) {
                Text(when(step) {
                    -12 -> stringResource(R.string.piano_octave_down)
                    12 -> stringResource(R.string.piano_octave_up)
                    -1 -> "‹ " + TuningMath.noteName((state.midi-1).coerceAtLeast(21))
                    else -> TuningMath.noteName((state.midi+1).coerceAtMost(108)) + " ›"
                },style=MaterialTheme.typography.labelMedium,textAlign=TextAlign.Center)
            }
        }
    }
    if(state.listening) LelloOutlinedButton(actions.stop,Modifier.fillMaxWidth()) {Text(stringResource(R.string.piano_pause_listening))}
    else LelloButton(actions.listen,Modifier.fillMaxWidth().testTag("piano_listen"),enabled=state.hasPermission&&!state.busy) {Text(stringResource(R.string.piano_listen))}
}

@Composable
private fun PianoDetails(state: PianoUiState, actions: PianoActions, profile: PianoProfile) {
    Text(stringResource(R.string.piano_details), style=MaterialTheme.typography.headlineLarge)
    Text(profile.name, style=MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.piano_reference,profile.referenceHz),style=MaterialTheme.typography.bodySmall)
    LelloOutlinedButton(actions.profiles,enabled=!state.busy) {Text(stringResource(R.string.piano_profiles))}
    if(state.listening) LelloOutlinedButton(actions.stop) {Text(stringResource(R.string.piano_pause_listening))}
    if(profile.ready) {
        Text(stringResource(R.string.piano_curve), style=MaterialTheme.typography.titleMedium)
        StretchCurve(profile)
        Text(stringResource(R.string.piano_curve_explanation),style=MaterialTheme.typography.bodySmall)
        val target=profile.targets.find {it.midi==state.midi}
        if(target!=null) {
            Text(stringResource(R.string.piano_stretch,NativeAudio.cents(target.frequencyHz,NativeAudio.equalTemperedHz(state.midi,profile.referenceHz))))
            val measured=profile.samples.any {it.midi==state.midi}
            val outside=state.midi !in profile.samples.minOf {it.midi }..profile.samples.maxOf {it.midi}
            Text(stringResource(if(measured) R.string.piano_measured else if(outside) R.string.piano_outside_samples else R.string.piano_interpolated))
        }
        LelloOutlinedButton(actions.refine,enabled=!state.listening&&!state.busy) {Text(stringResource(R.string.piano_refine))}
        var showTable by rememberSaveable(profile.id) {mutableStateOf(false)}
        LelloTextButton({showTable=!showTable}) {Text(stringResource(R.string.piano_target_table))}
        if(showTable) profile.targets.forEach { t -> Text(stringResource(R.string.piano_target_row,TuningMath.noteName(t.midi),t.frequencyHz,
            NativeAudio.cents(t.frequencyHz,NativeAudio.equalTemperedHz(t.midi,profile.referenceHz)))) }
    }
    Text(stringResource(R.string.piano_tuning_instructions),style=MaterialTheme.typography.bodySmall)
    state.measurement?.let {MeasurementDetails(it)}
    if(profile.samples.isNotEmpty()) {
        Text(stringResource(R.string.piano_saved_notes),style=MaterialTheme.typography.titleMedium)
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            profile.samples.forEach {sample -> LelloOutlinedButton({actions.recalibrate(sample.midi)},enabled=!state.listening&&!state.busy) {Text(TuningMath.noteName(sample.midi))} }
        }
    }
    LelloOutlinedButton(actions.export,enabled=!state.listening&&!state.busy) {Text(stringResource(R.string.piano_export))}
}

@Composable
private fun Profiles(state: PianoUiState, actions: PianoActions) {
    Text(stringResource(R.string.piano_intro))
    if (!state.loaded && state.error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
    state.profiles.forEach { profile ->
        LelloCard(Modifier.fillMaxWidth()) {
            Column(Modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(if (profile.ready) R.string.piano_ready_profile else R.string.piano_draft_profile,
                    profile.samples.size, profile.referenceHz))
                LelloButton({ actions.select(profile) }, enabled = !state.busy) {
                    Text(stringResource(if (profile.ready) R.string.piano_tune else R.string.piano_continue))
                }
            }
        }
    }
    var name by rememberSaveable { mutableStateOf("") }
    LelloTextField(name, { name = it.take(80) }, label = { Text(stringResource(R.string.piano_name)) },
        modifier = Modifier.fillMaxWidth().testTag("piano_name"), singleLine = true)
    LelloButton({ actions.create(name) }, enabled = state.loaded && !state.busy && name.isNotBlank() && state.profiles.size < 20,
        modifier = Modifier.testTag("piano_new")) { Text(stringResource(R.string.piano_new)) }
    Text(stringResource(R.string.piano_reference_hint), style = MaterialTheme.typography.bodySmall)
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
        LelloTextButton({ expanded = !expanded }) { Text(stringResource(R.string.piano_diagnostics)) }
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

private fun noteLabel(midi: Int) = buildAnnotatedString {
    val name = TuningMath.noteName(midi)
    append(name.dropLast(1))
    withStyle(SpanStyle(fontSize = 32.sp)) { append(name.takeLast(1)) }
}
