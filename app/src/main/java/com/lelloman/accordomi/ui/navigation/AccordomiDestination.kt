package com.lelloman.accordomi.ui.navigation

import androidx.annotation.StringRes
import com.lelloman.accordomi.R

data class AccordomiDestination(
    val route: String,
    @StringRes val labelRes: Int,
)

object AccordomiDestinations {
    val ToneDetection = AccordomiDestination("tone_detection", R.string.nav_tuner)
    val Settings = AccordomiDestination("settings", R.string.nav_settings)
    val About = AccordomiDestination("about", R.string.nav_about)

    val topLevel = listOf(ToneDetection, Settings, About)
}
