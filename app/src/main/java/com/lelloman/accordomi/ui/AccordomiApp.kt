package com.lelloman.accordomi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.lelloman.accordomi.R
import com.lelloman.accordomi.domain.settings.*
import com.lelloman.accordomi.feature.about.AboutRoute
import com.lelloman.accordomi.feature.piano.PianoRoute
import com.lelloman.accordomi.feature.settings.SettingsRoute
import com.lelloman.accordomi.feature.tone.*
import com.lelloman.accordomi.ui.navigation.AccordomiDestinations
import com.lelloman.lellodesign.*

@Composable
fun AccordomiApp(selectedTheme: ThemeId = BuiltInTheme.System.id, onTheme: (ThemeId) -> Unit = {}) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: AccordomiDestinations.ToneDetection.route
    val destinations = AccordomiDestinations.topLevel.map { destination ->
        LelloDestination(destination.route, stringResource(destination.labelRes)) {
            Icon(painterResource(when (destination) {
                AccordomiDestinations.Piano -> R.drawable.ic_nav_piano
                AccordomiDestinations.ReferenceTone -> R.drawable.ic_nav_tone
                AccordomiDestinations.Settings -> R.drawable.ic_nav_settings
                else -> R.drawable.ic_nav_tuner
            }), contentDescription = null)
        }
    }
    LelloScaffold(
        productName = stringResource(R.string.app_name), title = stringResource(R.string.app_name),
        destinations = destinations, selectedId = if (route == AccordomiDestinations.About.route) AccordomiDestinations.Settings.route else route,
        onNavigate = { target -> navController.navigate(target) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true; restoreState = true
        } },
        mobileNavigation = LelloMobileNavigation.Bottom,
        labels = LelloScaffoldLabels(stringResource(R.string.open_navigation), stringResource(R.string.collapse_sidebar),
            stringResource(R.string.expand_sidebar), stringResource(R.string.collapsed), stringResource(R.string.expanded)),
        logo = { Image(painterResource(R.drawable.ic_brand), null, Modifier.size(36.dp).background(Color.White, RoundedCornerShape(8.dp))) },
        actions = {
            LelloAppearanceSelector(when (selectedTheme) {
                BuiltInTheme.System.id -> LelloAppearance.System
                BuiltInTheme.Light.id -> LelloAppearance.Light
                BuiltInTheme.Dark.id -> LelloAppearance.Dark
                else -> null
            }, { onTheme(when (it) {
                LelloAppearance.System -> BuiltInTheme.System.id
                LelloAppearance.Light -> BuiltInTheme.Light.id
                LelloAppearance.Dark -> BuiltInTheme.Dark.id
            }) }, labels = LelloAppearanceLabels(stringResource(R.string.settings_appearance), stringResource(R.string.theme_light),
                stringResource(R.string.theme_dark), stringResource(R.string.theme_system)), customLabel = stringResource(R.string.theme_custom_default_name))
        },
    ) { innerPadding ->
        NavHost(navController, startDestination = AccordomiDestinations.ToneDetection.route,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
            composable(AccordomiDestinations.ToneDetection.route) { ToneDetectionRoute() }
            composable(AccordomiDestinations.Piano.route) { PianoRoute() }
            composable(AccordomiDestinations.Settings.route) { SettingsRoute(onAbout = { navController.navigate(AccordomiDestinations.About.route) }) }
            composable(AccordomiDestinations.ReferenceTone.route) { ReferenceToneRoute() }
            composable(AccordomiDestinations.About.route) { AboutRoute() }
        }
    }
}
