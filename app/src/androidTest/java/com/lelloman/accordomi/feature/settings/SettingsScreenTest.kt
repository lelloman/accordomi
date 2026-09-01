package com.lelloman.accordomi.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.lelloman.accordomi.ui.UiTestTags
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
                            onDetectionRateChanged = {},
                            onToneVisualizationStyleChanged = {},
                            onThemeChanged = {},
                            onSaveCustomTheme = { _, _, _ -> },
                            onDeleteCustomTheme = {},
                            onOpenAppPermissionSettings = {},
                        )
                    }
                }
            }
        }

        listOf(
            UiTestTags.ReferencePitch,
            UiTestTags.CreateCustomTheme,
            *SettingsUiState().availableToneDetectionMethods.map {
                UiTestTags.detectionMethod(it.storageKey)
            }.toTypedArray(),
            *SettingsUiState().availableDetectionRates.map {
                UiTestTags.detectionRate(it.storageKey)
            }.toTypedArray(),
            *SettingsUiState().availableToneVisualizationStyles.map {
                UiTestTags.visualizationStyle(it.storageKey)
            }.toTypedArray(),
            UiTestTags.OpenAppPermissions,
        ).forEach { label ->
            composeRule.onNodeWithTag(label)
                .performScrollTo()
                .assertIsDisplayed()
        }
    }
}
