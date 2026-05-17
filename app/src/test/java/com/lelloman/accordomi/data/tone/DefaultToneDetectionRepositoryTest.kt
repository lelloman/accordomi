package com.lelloman.accordomi.data.tone

import com.lelloman.accordomi.data.audio.AudioFrame
import com.lelloman.accordomi.data.audio.AudioRecorder
import com.lelloman.accordomi.data.pitch.PitchDetectionResult
import com.lelloman.accordomi.data.pitch.PitchDetector
import com.lelloman.accordomi.data.pitch.PitchDetectorRegistry
import com.lelloman.accordomi.domain.settings.AppSettings
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneVisualizationStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultToneDetectionRepositoryTest {
    @Test
    fun usesSelectedToneDetectionMethod() = runTest {
        val selectedDetector = FakePitchDetector(ToneDetectionMethod.Yin)
        val repository = DefaultToneDetectionRepository(
            audioRecorder = FakeAudioRecorder(),
            pitchDetectorRegistry = PitchDetectorRegistry(setOf(selectedDetector)),
            settingsRepository = FakeSettingsRepository(
                AppSettings(toneDetectionMethod = ToneDetectionMethod.Yin),
            ),
            defaultDispatcher = StandardTestDispatcher(testScheduler),
        )

        val reading = repository.readings().first()

        assertEquals("A4", reading!!.noteName)
        assertEquals(1, selectedDetector.detectCallCount)
    }

    private class FakeAudioRecorder : AudioRecorder {
        override fun frames(): Flow<AudioFrame> =
            flowOf(AudioFrame(samples = floatArrayOf(0.1f), sampleRate = 44_100))
    }

    private class FakePitchDetector(
        override val method: ToneDetectionMethod,
    ) : PitchDetector {
        var detectCallCount = 0

        override fun detect(samples: FloatArray, sampleRate: Int): PitchDetectionResult {
            detectCallCount++
            return PitchDetectionResult(frequencyHz = 440.0, clarity = 1f)
        }
    }

    private class FakeSettingsRepository(
        settings: AppSettings,
    ) : SettingsRepository {
        override val settings: Flow<AppSettings> = flowOf(settings)

        override suspend fun setReferencePitchHz(referencePitchHz: Double) = Unit

        override suspend fun setToneDetectionMethod(method: ToneDetectionMethod) = Unit

        override suspend fun setToneVisualizationStyle(style: ToneVisualizationStyle) = Unit
    }
}
