package com.lelloman.accordomi.data.tone

import com.lelloman.accordomi.data.audio.AudioFrame
import com.lelloman.accordomi.data.audio.AudioRecorder
import com.lelloman.accordomi.data.pitch.PitchDetectionResult
import com.lelloman.accordomi.data.pitch.PitchDetector
import com.lelloman.accordomi.data.pitch.PitchDetectorRegistry
import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneDetectionStatus
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultToneDetectionRepositoryTest {
    @Test
    fun visualizationChangesDoNotRedetectAndReferenceChangesOnlyRemap() = runTest {
        val detector = FakePitchDetector(ToneDetectionMethod.Yin, frequencyHz = 440.0)
        val audioRecorder = FakeAudioRecorder()
        val settingsRepository = FakeSettingsRepository()
        val repository = createRepository(audioRecorder, settingsRepository, detector)
        val statuses = mutableListOf<ToneDetectionStatus>()
        backgroundScope.launch(StandardTestDispatcher(testScheduler)) {
            repository.readings().toList(statuses)
        }
        runCurrent()

        audioRecorder.emit(sequenceNumber = 0)
        runCurrent()
        settingsRepository.setToneVisualizationStyle(ToneVisualizationStyle.Needle)
        runCurrent()

        assertEquals(1, detector.detectCallCount)
        assertEquals(1, statuses.size)

        settingsRepository.setReferencePitchHz(442.0)
        runCurrent()

        assertEquals(1, detector.detectCallCount)
        assertEquals(2, statuses.size)
        assertEquals(0.0, statuses.first().reading!!.centsOff, 0.001)
        assertEquals(-7.851, statuses.last().reading!!.centsOff, 0.001)
        assertEquals(1, audioRecorder.framesCallCount)
    }

    @Test
    fun methodChangesResetStabilizationWithoutRestartingRecorder() = runTest {
        val yinDetector = FakePitchDetector(ToneDetectionMethod.Yin, frequencyHz = 440.0)
        val mcLeodDetector = FakePitchDetector(ToneDetectionMethod.McLeod, frequencyHz = 450.0)
        val audioRecorder = FakeAudioRecorder()
        val settingsRepository = FakeSettingsRepository()
        val repository = createRepository(
            audioRecorder,
            settingsRepository,
            yinDetector,
            mcLeodDetector,
        )
        val statuses = mutableListOf<ToneDetectionStatus>()
        backgroundScope.launch(StandardTestDispatcher(testScheduler)) {
            repository.readings().toList(statuses)
        }
        runCurrent()

        audioRecorder.emit(sequenceNumber = 0)
        runCurrent()
        settingsRepository.setToneDetectionMethod(ToneDetectionMethod.McLeod)
        runCurrent()

        assertEquals(1, yinDetector.detectCallCount)
        assertEquals(1, mcLeodDetector.detectCallCount)
        assertEquals(450.0, statuses.last().reading!!.frequencyHz, 0.0)
        assertEquals(1, audioRecorder.framesCallCount)
    }

    private fun TestScope.createRepository(
        audioRecorder: FakeAudioRecorder,
        settingsRepository: FakeSettingsRepository,
        vararg detectors: FakePitchDetector,
    ) = DefaultToneDetectionRepository(
        audioRecorder = audioRecorder,
        pitchDetectorRegistry = PitchDetectorRegistry(detectors.toSet()),
        settingsRepository = settingsRepository,
        defaultDispatcher = StandardTestDispatcher(testScheduler),
    )

    private class FakeAudioRecorder : AudioRecorder {
        private val frames = MutableSharedFlow<AudioFrame>(extraBufferCapacity = 1)
        var framesCallCount = 0
            private set

        override fun frames(): Flow<AudioFrame> {
            framesCallCount++
            return frames
        }

        fun emit(sequenceNumber: Long) {
            check(
                frames.tryEmit(
                    AudioFrame(
                        samples = floatArrayOf(0.1f),
                        sampleRate = 44_100,
                        sequenceNumber = sequenceNumber,
                    ),
                ),
            )
        }
    }

    private class FakePitchDetector(
        override val method: ToneDetectionMethod,
        private val frequencyHz: Double,
    ) : PitchDetector {
        var detectCallCount = 0
            private set

        override fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult {
            detectCallCount++
            return PitchDetectionResult(frequencyHz = frequencyHz, clarity = 1f)
        }
    }

    private class FakeSettingsRepository(
        initialSettings: AppSettings = AppSettings(),
    ) : SettingsRepository {
        private val mutableSettings = MutableStateFlow(initialSettings)
        override val settings: Flow<AppSettings> = mutableSettings

        override suspend fun setReferencePitchHz(referencePitchHz: Double) {
            mutableSettings.update { it.copy(referencePitchHz = referencePitchHz) }
        }

        override suspend fun setToneDetectionMethod(method: ToneDetectionMethod) {
            mutableSettings.update { it.copy(toneDetectionMethod = method) }
        }

        override suspend fun setToneVisualizationStyle(style: ToneVisualizationStyle) {
            mutableSettings.update { it.copy(toneVisualizationStyle = style) }
        }
    }
}
