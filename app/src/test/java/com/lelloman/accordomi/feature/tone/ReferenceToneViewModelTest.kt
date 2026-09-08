package com.lelloman.accordomi.feature.tone

import com.lelloman.accordomi.domain.settings.*
import com.lelloman.accordomi.domain.tone.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceToneViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun usesConfiguredReferenceAndStopsOnNoteChangeAndLifecycleStop() = runTest(dispatcher) {
        val settings = FakeSettings()
        settings.settings.value = AppSettings(referencePitchHz = 442.0)
        val output = FakeOutput()
        val model = ReferenceToneViewModel(ObserveSettingsUseCase(settings), output)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        runCurrent()
        model.togglePlayback()
        runCurrent()
        assertEquals(442.0, output.frequencies.last(), 0.0)
        model.selectNote(81)
        runCurrent()
        assertFalse(model.uiState.value.isPlaying)
        advanceTimeBy(60)
        runCurrent()
        assertEquals(0, output.active)
        model.togglePlayback()
        runCurrent()
        assertEquals(884.0, output.frequencies.last(), 0.0)
        model.stopImmediately()
        advanceTimeBy(60)
        runCurrent()
        assertEquals(0, output.active)
    }

    @Test
    fun rapidStopAndRestartWaitsForCleanupAndKeepsNewPlaybackState() = runTest(dispatcher) {
        val output = FakeOutput()
        val model = ReferenceToneViewModel(ObserveSettingsUseCase(FakeSettings()), output)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        runCurrent()
        model.togglePlayback()
        runCurrent()
        model.stopImmediately()
        model.togglePlayback()
        runCurrent()
        advanceTimeBy(60)
        runCurrent()
        assertEquals(1, output.active)
        assertEquals(1, output.maximumActive)
        assertTrue(model.uiState.value.isPlaying)
        model.togglePlayback()
        advanceTimeBy(100)
        runCurrent()
        assertFalse(model.uiState.value.isPlaying)
        assertEquals(0, output.active)
    }

    @Test
    fun playbackFailureCanBeRetried() = runTest(dispatcher) {
        val output = FakeOutput().apply { fail = true }
        val model = ReferenceToneViewModel(ObserveSettingsUseCase(FakeSettings()), output)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }
        runCurrent()
        model.togglePlayback()
        runCurrent()
        assertTrue(model.uiState.value.hasError)
        assertFalse(model.uiState.value.isPlaying)
        output.fail = false
        model.togglePlayback()
        runCurrent()
        assertTrue(model.uiState.value.isPlaying)
        assertFalse(model.uiState.value.hasError)
        model.stopImmediately()
        advanceTimeBy(60)
        runCurrent()
    }

    private class FakeOutput : ReferenceToneOutput {
        var fail = false
        var active = 0
        var maximumActive = 0
        val frequencies = mutableListOf<Double>()
        override suspend fun play(frequencyHz: Double, shouldStop: () -> Boolean) {
            check(!fail)
            frequencies += frequencyHz
            active++
            maximumActive = maxOf(maximumActive, active)
            try {
                while (!shouldStop()) delay(10)
            } finally {
                withContext(NonCancellable) { delay(50) }
                active--
            }
        }
    }

    private class FakeSettings : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override suspend fun setReferencePitchHz(referencePitchHz: Double) { settings.value = settings.value.copy(referencePitchHz = referencePitchHz) }
        override suspend fun setToneDetectionMethod(method: ToneDetectionMethod) = Unit
        override suspend fun setDetectionRate(rate: DetectionRate) = Unit
        override suspend fun setToneVisualizationStyle(style: ToneVisualizationStyle) = Unit
        override suspend fun setSelectedThemeId(themeId: ThemeId) = Unit
        override suspend fun upsertCustomTheme(theme: CustomTheme) = Unit
        override suspend fun deleteCustomTheme(themeId: ThemeId) = Unit
    }
}
