package com.lelloman.accordomi.ui.navigation

data class AccordomiDestination(
    val route: String,
    val label: String,
)

object AccordomiDestinations {
    val ToneDetection = AccordomiDestination("tone_detection", "Tuner")
    val Settings = AccordomiDestination("settings", "Settings")
    val About = AccordomiDestination("about", "About")

    val topLevel = listOf(ToneDetection, Settings, About)
}

