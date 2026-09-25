package com.lelloman.accordomi.feature.tone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lelloman.accordomi.R

@Composable
fun ReferenceToneAppBar(
    state: ReferenceToneUiState,
    enabled: Boolean,
    onSelectNote: (Int) -> Unit,
    onSetFrequency: (Double) -> Unit,
    onTogglePlayback: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var frequencyText by remember(expanded, state.frequencyHz) { mutableStateOf("%.2f".format(state.frequencyHz)) }
    val enteredFrequency = frequencyText.replace(',', '.').toDoubleOrNull()
    val validFrequency = enteredFrequency != null && enteredFrequency.isFinite() && enteredFrequency in 20.0..20_000.0
    val playbackDescription = stringResource(R.string.reference_tone_playback)
    val displayedFrequency = "%.2f".format(state.frequencyHz)
    fun applyFrequency() {
        if (validFrequency) {
            onSetFrequency(enteredFrequency!!)
            expanded = false
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            TextButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.testTag("open_reference_tone"),
            ) {
                Text(if (state.customFrequencyHz == null) "${state.noteName} · $displayedFrequency Hz"
                    else "$displayedFrequency Hz")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Column(Modifier.width(320.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.reference_tone_title), style = MaterialTheme.typography.titleMedium)
                    Text(state.noteName, style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ToneStepButton("−12", stringResource(R.string.reference_tone_octave_down), state.midiNote >= 33) {
                            onSelectNote(state.midiNote - 12)
                        }
                        ToneStepButton("−1", stringResource(R.string.reference_tone_lower), state.midiNote > 21) {
                            onSelectNote(state.midiNote - 1)
                        }
                        ToneStepButton("+1", stringResource(R.string.reference_tone_higher), state.midiNote < 108) {
                            onSelectNote(state.midiNote + 1)
                        }
                        ToneStepButton("+12", stringResource(R.string.reference_tone_octave_up), state.midiNote <= 96) {
                            onSelectNote(state.midiNote + 12)
                        }
                    }
                    OutlinedTextField(
                        value = frequencyText,
                        onValueChange = { frequencyText = it },
                        label = { Text(stringResource(R.string.reference_tone_frequency_label)) },
                        singleLine = true,
                        isError = frequencyText.isNotBlank() && !validFrequency,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { applyFrequency() }),
                        modifier = Modifier.testTag("reference_tone_frequency_input"),
                    )
                    if (frequencyText.isNotBlank() && !validFrequency) {
                        Text(stringResource(R.string.reference_tone_frequency_error),
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { applyFrequency() }, enabled = validFrequency,
                        modifier = Modifier.align(Alignment.End).testTag("reference_tone_set_frequency")) {
                        Text(stringResource(R.string.reference_tone_set_frequency))
                    }
                    if (state.hasError) {
                        Text(stringResource(R.string.reference_tone_error), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Switch(
            checked = state.isPlaying,
            onCheckedChange = { onTogglePlayback() },
            enabled = enabled,
            modifier = Modifier.testTag("reference_tone_playback").semantics {
                contentDescription = playbackDescription
            },
        )
    }
}

@Composable
private fun ToneStepButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = description }) {
        Text(label)
    }
}
