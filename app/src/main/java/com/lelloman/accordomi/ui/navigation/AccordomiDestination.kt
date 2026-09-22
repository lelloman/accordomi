package com.lelloman.accordomi.ui.navigation

import androidx.annotation.StringRes
import com.lelloman.accordomi.R

data class AccordomiDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
)

object AccordomiDestinations {
    val ToneDetection = AccordomiDestination("tone_detection", R.string.nav_tuner)
    val Piano = AccordomiDestination("piano", R.string.nav_piano)
    val Settings = AccordomiDestination("settings", R.string.nav_settings)
    val ReferenceTone = AccordomiDestination("reference_tone", R.string.nav_reference_tone)
    val About = AccordomiDestination("about", R.string.nav_about)

    val topLevel = listOf(ToneDetection, Piano, ReferenceTone, Settings, About)
}
