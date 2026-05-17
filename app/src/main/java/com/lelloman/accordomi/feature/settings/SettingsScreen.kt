package com.lelloman.accordomi.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import com.lelloman.accordomi.feature.tone.openAppPermissionSettings

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsScreen(
        uiState = uiState,
        onReferencePitchChanged = viewModel::onReferencePitchChanged,
        onToneDetectionMethodChanged = viewModel::onToneDetectionMethodChanged,
        onToneVisualizationStyleChanged = viewModel::onToneVisualizationStyleChanged,
        onOpenAppPermissionSettings = { context.openAppPermissionSettings() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onReferencePitchChanged: (String) -> Unit,
    onToneDetectionMethodChanged: (ToneDetectionMethod) -> Unit,
    onToneVisualizationStyleChanged: (ToneVisualizationStyle) -> Unit,
    onOpenAppPermissionSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Settings") })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Reference pitch",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = uiState.referencePitchHzText,
                    onValueChange = onReferencePitchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("A4 frequency") },
                    suffix = { Text("Hz") },
                    isError = !uiState.isReferencePitchValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = {
                        if (!uiState.isReferencePitchValid) {
                            Text("Use a value between 400.0 and 480.0 Hz.")
                        }
                    },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tone detection method",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.availableToneDetectionMethods.forEach { method ->
                        FilterChip(
                            selected = method == uiState.selectedToneDetectionMethod,
                            onClick = { onToneDetectionMethodChanged(method) },
                            label = { Text(method.displayName) },
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tone visualization",
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.availableToneVisualizationStyles.forEach { style ->
                        FilterChip(
                            selected = style == uiState.selectedToneVisualizationStyle,
                            onClick = { onToneVisualizationStyleChanged(style) },
                            label = { Text(style.displayName) },
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Microphone permission",
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = onOpenAppPermissionSettings) {
                    Text("Open app permissions")
                }
            }
        }
    }
}
