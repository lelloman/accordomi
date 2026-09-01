package com.lelloman.accordomi.feature.tone

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.lelloman.accordomi.domain.tone.PitchReading
import com.lelloman.accordomi.ui.UiTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ToneDetectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permissionActionsAreOnlyInvokedByTheUserAndRemainAvailableAfterDenial() {
        var requestCount = 0
        var openSettingsCount = 0
        composeRule.setContent {
            MaterialTheme {
                ToneDetectionScreen(
                    uiState = ToneDetectionUiState(hasRecordPermission = false),
                    onRequestPermission = { requestCount++ },
                    onOpenSettings = { openSettingsCount++ },
                    onRetry = {},
                )
            }
        }

        composeRule.runOnIdle {
            assertEquals(0, requestCount)
            assertEquals(0, openSettingsCount)
        }

        composeRule.onNodeWithTag(UiTestTags.AllowMicrophone).performClick()
        composeRule.onNodeWithTag(UiTestTags.AllowMicrophone).performClick()
        composeRule.onNodeWithTag(UiTestTags.OpenAppPermissions).performClick()

        composeRule.runOnIdle {
            assertEquals(2, requestCount)
            assertEquals(1, openSettingsCount)
        }
    }

    @Test
    fun grantedPermissionShowsDetectionContent() {
        composeRule.setContent {
            MaterialTheme {
                ToneDetectionScreen(
                    uiState = ToneDetectionUiState(hasRecordPermission = true),
                    onRequestPermission = {},
                    onOpenSettings = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.DetectionContent).assertExists()
        composeRule.onNodeWithTag(UiTestTags.AllowMicrophone).assertDoesNotExist()
    }

    @Test
    fun lagFlagOnlyAppearsWithAReadingAndDoesNotMoveTheVisualization() {
        val state = mutableStateOf(
            ToneDetectionUiState(
                hasRecordPermission = true,
                isListening = true,
                isLagging = true,
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                ToneDetectionScreen(
                    uiState = state.value,
                    onRequestPermission = {},
                    onOpenSettings = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.ProcessingLag).assertDoesNotExist()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                reading = PitchReading(
                    frequencyHz = 440.0,
                    clarity = 1f,
                    noteName = "A4",
                    centsOff = 0.0,
                    targetFrequencyHz = 440.0,
                ),
                isLagging = false,
            )
        }
        val boundsWithoutFlag = composeRule
            .onNodeWithTag(UiTestTags.ToneVisualization)
            .fetchSemanticsNode()
            .boundsInRoot

        composeRule.runOnIdle {
            state.value = state.value.copy(isLagging = true)
        }
        composeRule.onNodeWithTag(UiTestTags.ProcessingLag).assertExists()
        val boundsWithFlag = composeRule
            .onNodeWithTag(UiTestTags.ToneVisualization)
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(boundsWithoutFlag, boundsWithFlag)
    }

    @Test
    fun permissionStateRefreshesInitiallyAndWheneverTheScreenResumes() {
        val lifecycleOwner = TestLifecycleOwner()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            lifecycleOwner.handleEvent(Lifecycle.Event.ON_CREATE)
        }
        var refreshCount = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                RefreshRecordPermissionOnResume { refreshCount++ }
            }
        }

        composeRule.runOnIdle {
            assertEquals(1, refreshCount)
            lifecycleOwner.handleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleEvent(Lifecycle.Event.ON_RESUME)
        }

        composeRule.runOnIdle {
            assertEquals(2, refreshCount)
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry

        fun handleEvent(event: Lifecycle.Event) {
            registry.handleLifecycleEvent(event)
        }
    }
}
