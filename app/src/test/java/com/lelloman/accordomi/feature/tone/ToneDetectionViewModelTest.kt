package com.lelloman.accordomi.feature.tone

import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.ThemeId
import com.lelloman.accordomi.domain.settings.ObserveSettingsUseCase
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.ObserveToneDetectionUseCase
import com.lelloman.accordomi.domain.tone.PitchReading
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneDetectionRepository
import com.lelloman.accordomi.domain.tone.ToneDetectionStatus
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ToneDetectionViewModelTest {
    private val mainScheduler = TestCoroutineScheduler()
    private val mainDispatcher = UnconfinedTestDispatcher(mainScheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun retryStartsFreshDetectionAndClearsError() = runTest {
        val toneRepository = FailingThenSuccessfulToneRepository()
        val viewModel = ToneDetectionViewModel(
            observeToneDetection = ObserveToneDetectionUseCase(toneRepository),
            observeSettings = ObserveSettingsUseCase(FakeSettingsRepository()),
        )
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onRecordPermissionChanged(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasError)
        assertEquals(1, toneRepository.subscriptionCount)

        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(2, toneRepository.subscriptionCount)
        assertFalse(viewModel.uiState.value.hasError)
        assertTrue(viewModel.uiState.value.isListening)
        assertEquals("A4", viewModel.uiState.value.reading?.noteName)

        collection.cancel()
    }

    @Test
    fun stopsDetectionAfterFiveHundredMillisecondsWithoutSubscribers() = runTest {
        val toneRepository = CancellableToneRepository()
        val viewModel = ToneDetectionViewModel(
            observeToneDetection = ObserveToneDetectionUseCase(toneRepository),
            observeSettings = ObserveSettingsUseCase(FakeSettingsRepository()),
        )
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        viewModel.onRecordPermissionChanged(true)

        assertEquals(1, toneRepository.subscriptionCount)

        collection.cancel()
        mainScheduler.advanceTimeBy(499)
        mainScheduler.runCurrent()
        assertEquals(0, toneRepository.cancellationCount)

        mainScheduler.advanceTimeBy(1)
        mainScheduler.runCurrent()

        assertEquals(1, toneRepository.cancellationCount)
        assertEquals(null, viewModel.uiState.value.reading)
        assertFalse(viewModel.uiState.value.hasError)
    }

    private class FailingThenSuccessfulToneRepository : ToneDetectionRepository {
        var subscriptionCount = 0

        override fun readings(): Flow<ToneDetectionStatus> = flow {
            subscriptionCount++
            if (subscriptionCount == 1) {
                error("transient failure")
            }
            emit(
                ToneDetectionStatus(
                    reading = PitchReading(
                    frequencyHz = 440.0,
                    clarity = 1f,
                    noteName = "A4",
                    centsOff = 0.0,
                    targetFrequencyHz = 440.0,
                    ),
                    isLagging = false,
                ),
            )
        }
    }

    private class CancellableToneRepository : ToneDetectionRepository {
        var subscriptionCount = 0
        var cancellationCount = 0

        override fun readings(): Flow<ToneDetectionStatus> = flow {
            subscriptionCount++
            try {
                awaitCancellation()
            } finally {
                cancellationCount++
            }
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())

        override suspend fun setReferencePitchHz(referencePitchHz: Double) = Unit

        override suspend fun setToneDetectionMethod(method: ToneDetectionMethod) = Unit

        override suspend fun setToneVisualizationStyle(style: ToneVisualizationStyle) = Unit

        override suspend fun setSelectedThemeId(themeId: ThemeId) = Unit
    }
}
