package com.lelloman.accordomi.feature.tone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lelloman.accordomi.R

@Composable
fun ReferenceToneRoute(viewModel: ReferenceToneViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
    ReferenceToneScreen(state, viewModel::selectNote, viewModel::togglePlayback)
}

@Composable
fun ReferenceToneScreen(
    state: ReferenceToneUiState,
    onSelectNote: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.reference_tone_title), style = MaterialTheme.typography.headlineSmall)
        Text(state.noteName, style = MaterialTheme.typography.displayLarge)
        Text(stringResource(R.string.reference_tone_frequency, state.frequencyHz), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.reference_tone_reference, state.referencePitchHz))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onSelectNote(state.midiNote - 1) }, enabled = state.midiNote > 21) {
                Text(stringResource(R.string.reference_tone_lower))
            }
            OutlinedButton(onClick = { onSelectNote(state.midiNote + 1) }, enabled = state.midiNote < 108) {
                Text(stringResource(R.string.reference_tone_higher))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onSelectNote(state.midiNote - 12) }, enabled = state.midiNote >= 33) {
                Text(stringResource(R.string.reference_tone_octave_down))
            }
            OutlinedButton(onClick = { onSelectNote(state.midiNote + 12) }, enabled = state.midiNote <= 96) {
                Text(stringResource(R.string.reference_tone_octave_up))
            }
        }
        Button(onClick = onTogglePlayback) {
            Text(stringResource(if (state.isPlaying) R.string.reference_tone_stop else R.string.reference_tone_play))
        }
        Text(stringResource(R.string.reference_tone_help), textAlign = TextAlign.Center)
        if (state.hasError) {
            Text(stringResource(R.string.reference_tone_error), color = MaterialTheme.colorScheme.error)
        }
    }
}
