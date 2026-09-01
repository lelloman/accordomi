package com.lelloman.accordomi.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allControlsRemainReachableAtNarrowWidthAndLargeFontScale() {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 1.5f,
                ),
            ) {
                MaterialTheme {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .height(400.dp),
                    ) {
                        SettingsScreen(
                            uiState = SettingsUiState(),
                            onReferencePitchChanged = {},
                            onToneDetectionMethodChanged = {},
                            onToneVisualizationStyleChanged = {},
                            onOpenAppPermissionSettings = {},
                        )
                    }
                }
            }
        }

        listOf(
            "A4 frequency",
            "YIN",
            "Autocorrelation",
            "McLeod",
            "Text",
            "Needle",
            "Side wheel",
            "Open app permissions",
        ).forEach { label ->
            composeRule.onNodeWithText(label)
                .performScrollTo()
                .assertIsDisplayed()
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
