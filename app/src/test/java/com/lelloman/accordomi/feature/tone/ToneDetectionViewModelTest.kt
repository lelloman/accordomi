package com.lelloman.accordomi.feature.tone

import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.ObserveSettingsUseCase
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.ObserveToneDetectionUseCase
import com.lelloman.accordomi.domain.tone.PitchReading
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneDetectionRepository
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ToneDetectionViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

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

        assertEquals("transient failure", viewModel.uiState.value.errorMessage)
        assertEquals(1, toneRepository.subscriptionCount)

        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(2, toneRepository.subscriptionCount)
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.isListening)
        assertEquals("A4", viewModel.uiState.value.reading?.noteName)

        collection.cancel()
    }

    private class FailingThenSuccessfulToneRepository : ToneDetectionRepository {
        var subscriptionCount = 0

        override fun readings(): Flow<PitchReading?> = flow {
            subscriptionCount++
            if (subscriptionCount == 1) {
                error("transient failure")
            }
            emit(
                PitchReading(
                    frequencyHz = 440.0,
                    clarity = 1f,
                    noteName = "A4",
                    centsOff = 0.0,
                    targetFrequencyHz = 440.0,
                ),
            )
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())

        override suspend fun setReferencePitchHz(referencePitchHz: Double) = Unit

        override suspend fun setToneDetectionMethod(method: ToneDetectionMethod) = Unit

        override suspend fun setToneVisualizationStyle(style: ToneVisualizationStyle) = Unit
    }
}
