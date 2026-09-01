package com.lelloman.accordomi.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lelloman.accordomi.R
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import com.lelloman.accordomi.feature.tone.openAppPermissionSettings
import com.lelloman.accordomi.ui.UiTestTags

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]

    LaunchedEffect(locale) {
        viewModel.onLocaleChanged(locale)
    }

    SettingsScreen(
        uiState = uiState,
        onReferencePitchChanged = viewModel::onReferencePitchChanged,
        onToneDetectionMethodChanged = viewModel::onToneDetectionMethodChanged,
        onToneVisualizationStyleChanged = viewModel::onToneVisualizationStyleChanged,
        onOpenAppPermissionSettings = { context.openAppPermissionSettings() },
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onReferencePitchChanged: (String) -> Unit,
    onToneDetectionMethodChanged: (ToneDetectionMethod) -> Unit,
    onToneVisualizationStyleChanged: (ToneVisualizationStyle) -> Unit,
    onOpenAppPermissionSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .testTag(UiTestTags.SettingsContent)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.reference_pitch_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = uiState.referencePitchHzText,
                    onValueChange = onReferencePitchChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.ReferencePitch),
                    label = { Text(stringResource(R.string.a4_frequency_label)) },
                    suffix = { Text(stringResource(R.string.frequency_unit)) },
                    isError = !uiState.isReferencePitchValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = {
                        if (!uiState.isReferencePitchValid) {
                            Text(stringResource(R.string.reference_pitch_invalid))
                        }
                    },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.tone_detection_method_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.availableToneDetectionMethods.forEach { method ->
                        FilterChip(
                            selected = method == uiState.selectedToneDetectionMethod,
                            onClick = { onToneDetectionMethodChanged(method) },
                            label = { Text(stringResource(method.labelRes())) },
                            modifier = Modifier.testTag(
                                UiTestTags.detectionMethod(method.storageKey),
                            ),
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.tone_visualization_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.availableToneVisualizationStyles.forEach { style ->
                        FilterChip(
                            selected = style == uiState.selectedToneVisualizationStyle,
                            onClick = { onToneVisualizationStyleChanged(style) },
                            label = { Text(stringResource(style.labelRes())) },
                            modifier = Modifier.testTag(
                                UiTestTags.visualizationStyle(style.storageKey),
                            ),
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.microphone_permission_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(
                    onClick = onOpenAppPermissionSettings,
                    modifier = Modifier.testTag(UiTestTags.OpenAppPermissions),
                ) {
                    Text(stringResource(R.string.open_app_permissions))
                }
            }
        }
    }
}

private fun ToneDetectionMethod.labelRes(): Int = when (this) {
    ToneDetectionMethod.Yin -> R.string.tone_detection_yin
    ToneDetectionMethod.AutoCorrelation -> R.string.tone_detection_autocorrelation
    ToneDetectionMethod.McLeod -> R.string.tone_detection_mcleod
}

private fun ToneVisualizationStyle.labelRes(): Int = when (this) {
    ToneVisualizationStyle.Text -> R.string.tone_visualization_text
    ToneVisualizationStyle.Needle -> R.string.tone_visualization_needle
    ToneVisualizationStyle.SideWheel -> R.string.tone_visualization_side_wheel
    ToneVisualizationStyle.PianoKeyboard -> R.string.tone_visualization_piano_keyboard
}
