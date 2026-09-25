package com.lelloman.accordomi.feature.tone

import com.lelloman.lellodesign.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lelloman.accordomi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceToneSheet(
    onDismiss: () -> Unit,
    initialMidi: Int? = null,
    referenceHz: Double? = null,
    targets: Map<Int, Double> = emptyMap(),
    viewModel: ReferenceToneViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialMidi, referenceHz, targets) {
        viewModel.configure(initialMidi, referenceHz, targets)
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.stopImmediately()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            viewModel.stopImmediately()
        }
    }
    ModalBottomSheet(
        onDismissRequest = { viewModel.stopImmediately(); onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ReferenceToneScreen(state, viewModel::selectNote, viewModel::togglePlayback, Modifier.weight(1f, fill = false))
        LelloTextButton(onClick = { viewModel.stopImmediately(); onDismiss() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("close_reference_tone")) {
            Text(stringResource(R.string.reference_tone_close))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferenceToneScreen(
    state: ReferenceToneUiState,
    onSelectNote: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.reference_tone_title), style = MaterialTheme.typography.headlineSmall)
        Text(state.noteName, style = MaterialTheme.typography.displayLarge)
        Text(stringResource(R.string.reference_tone_frequency, state.frequencyHz), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.reference_tone_reference, state.referencePitchHz))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            LelloOutlinedButton(onClick = { onSelectNote(state.midiNote - 1) }, enabled = state.midiNote > 21) {
                Text(stringResource(R.string.reference_tone_lower))
            }
            LelloOutlinedButton(onClick = { onSelectNote(state.midiNote + 1) }, enabled = state.midiNote < 108) {
                Text(stringResource(R.string.reference_tone_higher))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            LelloOutlinedButton(onClick = { onSelectNote(state.midiNote - 12) }, enabled = state.midiNote >= 33) {
                Text(stringResource(R.string.reference_tone_octave_down))
            }
            LelloOutlinedButton(onClick = { onSelectNote(state.midiNote + 12) }, enabled = state.midiNote <= 96) {
                Text(stringResource(R.string.reference_tone_octave_up))
            }
        }
        LelloButton(onClick = onTogglePlayback, modifier = Modifier.testTag("reference_tone_playback")) {
            Text(stringResource(if (state.isPlaying) R.string.reference_tone_stop else R.string.reference_tone_play))
        }
        Text(stringResource(R.string.reference_tone_help), textAlign = TextAlign.Center)
        if (state.hasError) {
            Text(stringResource(R.string.reference_tone_error), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun ReferenceToneButton(onClick: () -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End) {
        LelloOutlinedButton(onClick, enabled = enabled, modifier = Modifier.testTag("open_reference_tone")) {
            Text(stringResource(R.string.reference_tone_title))
        }
    }
}
