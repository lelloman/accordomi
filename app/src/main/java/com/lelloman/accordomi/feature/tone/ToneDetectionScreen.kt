package com.lelloman.accordomi.feature.tone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lelloman.accordomi.R
import com.lelloman.accordomi.ui.UiTestTags

@Composable
fun ToneDetectionRoute(
    viewModel: ToneDetectionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onRecordPermissionChanged(granted)
    }

    fun refreshPermission() {
        viewModel.onRecordPermissionChanged(context.hasRecordAudioPermission())
    }

    RefreshRecordPermissionOnResume(::refreshPermission)
    DisposableEffect(viewModel) {
        onDispose { viewModel.onRecordPermissionChanged(false) }
    }

    ToneDetectionScreen(
        uiState = uiState,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onOpenSettings = { context.openAppPermissionSettings() },
        onRetry = viewModel::onRetry,
    )
}

@Composable
internal fun RefreshRecordPermissionOnResume(onRefresh: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentRefresh by rememberUpdatedState(onRefresh)

    LaunchedEffect(lifecycleOwner) {
        currentRefresh()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun ToneDetectionScreen(
    uiState: ToneDetectionUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            if (!uiState.hasRecordPermission) {
                PermissionRequired(
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings,
                )
            } else {
                DetectionContent(
                    uiState = uiState,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun PermissionRequired(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.microphone_permission_explanation),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.testTag(UiTestTags.AllowMicrophone),
            ) {
                Text(stringResource(R.string.allow_permission))
            }
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag(UiTestTags.OpenAppPermissions),
            ) {
                Text(stringResource(R.string.open_app_permissions))
            }
        }
    }
}

@Composable
private fun DetectionContent(
    uiState: ToneDetectionUiState,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.DetectionContent),
    ) {
        val reading = uiState.reading
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.hasError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.audio_recording_failed),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .testTag(UiTestTags.RetryDetection),
                    ) {
                        Text(stringResource(R.string.try_again))
                    }
                }
            } else if (reading == null) {
                Text(
                    text = stringResource(
                        if (uiState.isListening) R.string.listening else R.string.ready,
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                )
            } else {
                ToneVisualization(
                    reading = reading,
                    style = uiState.visualizationStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.ToneVisualization),
                )
            }
        }
        AnimatedVisibility(
            visible = !uiState.hasError && reading != null && uiState.isLagging,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = fadeIn(animationSpec = tween(durationMillis = LagFadeInMillis)),
            exit = fadeOut(animationSpec = tween(durationMillis = LagFadeOutMillis)),
        ) {
            Surface(
                modifier = Modifier.testTag(UiTestTags.ProcessingLag),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.small,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning_small),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.processing_lag_warning),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private const val LagFadeInMillis = 250
private const val LagFadeOutMillis = 500

private fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

fun Context.openAppPermissionSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
