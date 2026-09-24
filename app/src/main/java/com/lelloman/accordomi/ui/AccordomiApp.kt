package com.lelloman.accordomi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.*
import com.lelloman.accordomi.R
import com.lelloman.accordomi.feature.about.AboutRoute
import com.lelloman.accordomi.feature.piano.PianoRoute
import com.lelloman.accordomi.feature.settings.SettingsRoute
import com.lelloman.accordomi.feature.tone.*
import com.lelloman.accordomi.ui.navigation.AccordomiDestinations
import com.lelloman.lellodesign.*

@Composable
fun AccordomiApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: AccordomiDestinations.ToneDetection.route
    val currentDestination = AccordomiDestinations.topLevel.firstOrNull { it.route == route }
        ?: AccordomiDestinations.ToneDetection
    val destinations = AccordomiDestinations.topLevel.map { destination ->
        LelloDestination(destination.route, stringResource(destination.labelRes)) {
            Icon(painterResource(when (destination) {
                AccordomiDestinations.Piano -> R.drawable.ic_nav_piano
                AccordomiDestinations.ReferenceTone -> R.drawable.ic_nav_tone
                AccordomiDestinations.Settings -> R.drawable.ic_nav_settings
                AccordomiDestinations.About -> R.drawable.ic_nav_about
                else -> R.drawable.ic_nav_tuner
            }), contentDescription = null)
        }
    }
    LelloScaffold(
        productName = stringResource(R.string.app_name), title = stringResource(currentDestination.labelRes),
        destinations = destinations, selectedId = route,
        onNavigate = { target -> navController.navigate(target) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true; restoreState = true
        } },
        mobileNavigation = LelloMobileNavigation.Drawer,
        labels = LelloScaffoldLabels(stringResource(R.string.open_navigation), stringResource(R.string.collapse_sidebar),
            stringResource(R.string.expand_sidebar), stringResource(R.string.collapsed), stringResource(R.string.expanded)),
    ) { innerPadding ->
        NavHost(navController, startDestination = AccordomiDestinations.ToneDetection.route,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
            composable(AccordomiDestinations.ToneDetection.route) { ToneDetectionRoute() }
            composable(AccordomiDestinations.Piano.route) { PianoRoute() }
            composable(AccordomiDestinations.Settings.route) { SettingsRoute() }
            composable(AccordomiDestinations.ReferenceTone.route) { ReferenceToneRoute() }
            composable(AccordomiDestinations.About.route) { AboutRoute() }
        }
    }
}
