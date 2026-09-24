package com.lelloman.accordomi.feature.settings

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import com.lelloman.lellodesign.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lelloman.accordomi.R
import com.lelloman.accordomi.domain.settings.BuiltInTheme
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.settings.ThemePalette
import com.lelloman.accordomi.domain.settings.resolvePalette
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import com.lelloman.accordomi.domain.tone.DetectionRate
import com.lelloman.accordomi.feature.tone.openAppPermissionSettings
import com.lelloman.accordomi.ui.UiTestTags

@Composable
fun SettingsRoute(
    onAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val updatesComponent = remember(context) {
        ComponentName(context.packageName, "com.lelloman.paravoidandroid.delivery.ShellUpdatesActivity")
    }
    val hasUpdateControls = remember(context, updatesComponent) {
        try {
            context.packageManager.getActivityInfo(updatesComponent, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    LaunchedEffect(locale) {
        viewModel.onLocaleChanged(locale)
    }

    SettingsScreen(
        uiState = uiState,
        onReferencePitchChanged = viewModel::onReferencePitchChanged,
        onToneDetectionMethodChanged = viewModel::onToneDetectionMethodChanged,
        onDetectionRateChanged = viewModel::onDetectionRateChanged,
        onToneVisualizationStyleChanged = viewModel::onToneVisualizationStyleChanged,
        onThemeChanged = viewModel::onThemeChanged,
        onOpenAppPermissionSettings = { context.openAppPermissionSettings() },
        onOpenUpdates = if (hasUpdateControls) {
            { context.startActivity(Intent().setComponent(updatesComponent)) }
        } else null,
        onAbout = onAbout,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onReferencePitchChanged: (String) -> Unit,
    onToneDetectionMethodChanged: (ToneDetectionMethod) -> Unit,
    onDetectionRateChanged: (DetectionRate) -> Unit,
    onToneVisualizationStyleChanged: (ToneVisualizationStyle) -> Unit,
    onThemeChanged: (ThemeId) -> Unit,
    onOpenAppPermissionSettings: () -> Unit,
    onOpenUpdates: (() -> Unit)? = null,
    onAbout: () -> Unit = {},
) {
    val systemDark = isSystemInDarkTheme()
    Column(modifier = Modifier.fillMaxSize()) {

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
            LelloSettingsSection(stringResource(R.string.settings_tuning)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.reference_pitch_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                LelloTextField(
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
                    text = stringResource(R.string.tone_visualization_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.availableToneVisualizationStyles.forEach { style ->
                        LelloFilterChip(
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
            }
            LelloSettingsSection(stringResource(R.string.settings_appearance)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.theme_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.availableBuiltInThemes.forEach { theme ->
                        LelloFilterChip(
                            selected = theme.id == uiState.selectedThemeId,
                            onClick = { onThemeChanged(theme.id) },
                            leadingIcon = {
                                ThemeSwatch(theme.resolvePalette(systemDark))
                            },
                            label = { Text(stringResource(theme.labelRes())) },
                        )
                    }
                }
            }
            }
            LelloSettingsSection(stringResource(R.string.settings_advanced)) {
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
                        LelloFilterChip(
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
                    text = stringResource(R.string.detection_rate_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.detection_rate_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.availableDetectionRates.forEach { rate ->
                        LelloFilterChip(
                            selected = rate == uiState.selectedDetectionRate,
                            onClick = { onDetectionRateChanged(rate) },
                            label = { Text(stringResource(rate.labelRes())) },
                            modifier = Modifier.testTag(
                                UiTestTags.detectionRate(rate.storageKey),
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
                LelloButton(
                    onClick = onOpenAppPermissionSettings,
                    modifier = Modifier.testTag(UiTestTags.OpenAppPermissions),
                ) {
                    Text(stringResource(R.string.open_app_permissions))
                }
            }
            }
            if (onOpenUpdates != null) {
                LelloSettingsSection(stringResource(R.string.settings_updates)) {
                    LelloOutlinedButton(onOpenUpdates, Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.open_update_controls))
                    }
                }
            }
            LelloSettingsSection(stringResource(R.string.nav_about)) {
                LelloOutlinedButton(onAbout, Modifier.fillMaxWidth()) { Text(stringResource(R.string.about_title)) }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(palette: ThemePalette) {
    LelloPaletteSwatch(listOf(Color(palette.background), Color(palette.accent), Color(palette.inTune)))
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

private fun DetectionRate.labelRes(): Int = when (this) {
    DetectionRate.Efficient -> R.string.detection_rate_efficient
    DetectionRate.Balanced -> R.string.detection_rate_balanced
    DetectionRate.High -> R.string.detection_rate_high
}

private fun BuiltInTheme.labelRes(): Int = when (this) {
    BuiltInTheme.System -> R.string.theme_system
    BuiltInTheme.Light -> R.string.theme_light
    BuiltInTheme.Dark -> R.string.theme_dark
    BuiltInTheme.Concert -> R.string.theme_concert
    BuiltInTheme.Ocean -> R.string.theme_ocean
    BuiltInTheme.Forest -> R.string.theme_forest
}
