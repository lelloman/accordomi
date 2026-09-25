package com.lelloman.accordomi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.lelloman.accordomi.R
import com.lelloman.accordomi.feature.about.AboutRoute
import com.lelloman.accordomi.feature.piano.PianoRoute
import com.lelloman.accordomi.feature.piano.PianoViewModel
import com.lelloman.accordomi.feature.settings.SettingsRoute
import com.lelloman.accordomi.feature.tone.*
import com.lelloman.accordomi.ui.navigation.AccordomiDestinations
import com.lelloman.lellodesign.*

@Composable
fun AccordomiApp() {
    val toneViewModel: ReferenceToneViewModel = hiltViewModel()
    val tunerViewModel: ToneDetectionViewModel = hiltViewModel()
    val pianoViewModel: PianoViewModel = hiltViewModel()
    val toneState by toneViewModel.uiState.collectAsStateWithLifecycle()
    val pianoState by pianoViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: AccordomiDestinations.ToneDetection.route
    val currentDestination = AccordomiDestinations.topLevel.firstOrNull { it.route == route }
        ?: AccordomiDestinations.ToneDetection
    LaunchedEffect(route) {
        toneViewModel.stopImmediately()
        if (route == AccordomiDestinations.ToneDetection.route) toneViewModel.configure(null, null, emptyMap())
    }
    DisposableEffect(lifecycleOwner, toneViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) toneViewModel.stopImmediately()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            toneViewModel.stopImmediately()
        }
    }
    val destinations = AccordomiDestinations.topLevel.map { destination ->
        LelloDestination(destination.route, stringResource(destination.labelRes)) {
            Icon(painterResource(when (destination) {
                AccordomiDestinations.Piano -> R.drawable.ic_nav_piano
                AccordomiDestinations.Settings -> R.drawable.ic_nav_settings
                AccordomiDestinations.About -> R.drawable.ic_nav_about
                else -> R.drawable.ic_nav_tuner
            }), contentDescription = null)
        }
    }
    AccordomiFrame(
        title = stringResource(currentDestination.labelRes),
        actions = {
            if (route == AccordomiDestinations.ToneDetection.route || route == AccordomiDestinations.Piano.route) {
                ReferenceToneAppBar(
                    state = toneState,
                    enabled = route != AccordomiDestinations.Piano.route || !pianoState.busy,
                    onSelectNote = toneViewModel::selectNote,
                    onSetFrequency = toneViewModel::setFrequency,
                    onTogglePlayback = {
                        if (!toneState.isPlaying) {
                            if (route == AccordomiDestinations.ToneDetection.route) tunerViewModel.setPaused(true)
                            if (route == AccordomiDestinations.Piano.route) pianoViewModel.stop()
                        }
                        toneViewModel.togglePlayback()
                    },
                )
            }
        },
        destinations = destinations, selectedId = route,
        onNavigate = { target -> navController.navigate(target) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true; restoreState = true
        } },
    ) { innerPadding ->
        NavHost(navController, startDestination = AccordomiDestinations.ToneDetection.route,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
            composable(AccordomiDestinations.ToneDetection.route) {
                ToneDetectionRoute(tonePlaying = toneState.isPlaying, viewModel = tunerViewModel)
            }
            composable(AccordomiDestinations.Piano.route) {
                PianoRoute(toneViewModel = toneViewModel, viewModel = pianoViewModel)
            }
            composable(AccordomiDestinations.Settings.route) { SettingsRoute() }
            composable(AccordomiDestinations.About.route) { AboutRoute() }
        }
    }
}

@Composable
internal fun AccordomiFrame(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    destinations: List<LelloDestination>,
    selectedId: String,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            Surface {
                Column(Modifier.statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 24.dp, end = 12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        actions()
                    }
                    HorizontalDivider()
                }
            }
        },
        bottomBar = { LelloBottomNavigation(destinations, selectedId, onNavigate) },
        content = content,
    )
}
