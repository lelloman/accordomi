package com.lelloman.accordomi.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.lelloman.accordomi.MainActivity
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun tab(label: String) = compose.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
    )

    @Test
    fun fourBottomTabsAndEmbeddedToneOnBothTuningScreens() {
        listOf("Tuner", "Piano", "Settings", "About").forEach { tab(it).assertIsDisplayed() }
        compose.onNodeWithContentDescription("Open navigation").assertDoesNotExist()
        compose.onNodeWithText("Tone").assertDoesNotExist()
        for (destination in listOf("Tuner", "Piano")) {
            tab(destination).performClick().assertIsSelected()
            compose.onNodeWithTag("open_reference_tone").performClick()
            compose.onNodeWithTag("reference_tone_playback").performScrollTo().performClick()
            compose.onNodeWithText("Stop tone").assertIsDisplayed()
            compose.onNodeWithTag("close_reference_tone").performClick()
            compose.onNodeWithTag("open_reference_tone").performClick()
            compose.onNodeWithText("Play tone").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("close_reference_tone").performClick()
        }
        for (destination in listOf("Settings", "About")) {
            tab(destination).performClick().assertIsSelected()
            compose.onNodeWithTag("open_reference_tone").assertDoesNotExist()
        }
    }
}
