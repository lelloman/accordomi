package com.lelloman.accordomi.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lelloman.accordomi.feature.about.AboutRoute
import com.lelloman.accordomi.feature.settings.SettingsRoute
import com.lelloman.accordomi.feature.tone.ToneDetectionRoute
import com.lelloman.accordomi.ui.navigation.AccordomiDestinations

@Composable
fun AccordomiApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AccordomiDestinations.topLevel.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {},
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AccordomiDestinations.ToneDetection.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AccordomiDestinations.ToneDetection.route) {
                ToneDetectionRoute()
            }
            composable(AccordomiDestinations.Settings.route) {
                SettingsRoute()
            }
            composable(AccordomiDestinations.About.route) {
                AboutRoute()
            }
        }
    }
}
