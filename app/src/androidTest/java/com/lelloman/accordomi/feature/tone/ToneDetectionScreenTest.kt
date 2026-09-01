package com.lelloman.accordomi.feature.tone

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
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

        composeRule.onNodeWithText("Allow").performClick()
        composeRule.onNodeWithText("Allow").performClick()
        composeRule.onNodeWithText("Open app permissions").performClick()

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

        composeRule.onNodeWithText("Ready").assertExists()
        composeRule.onNodeWithText("Allow").assertDoesNotExist()
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
