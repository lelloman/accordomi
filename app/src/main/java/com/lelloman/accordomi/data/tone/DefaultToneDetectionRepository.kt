package com.lelloman.accordomi.data.tone

import com.lelloman.accordomi.core.di.DefaultDispatcher
import com.lelloman.accordomi.data.audio.AudioRecorder
import com.lelloman.accordomi.data.pitch.PitchDetectorRegistry
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.PitchReading
import com.lelloman.accordomi.domain.tone.ToneDetectionRepository
import com.lelloman.accordomi.domain.tone.TuningMath
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultToneDetectionRepository @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val pitchDetectorRegistry: PitchDetectorRegistry,
    private val settingsRepository: SettingsRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ToneDetectionRepository {
    override fun readings(): Flow<PitchReading?> =
        settingsRepository.settings.flatMapLatest { settings ->
            audioRecorder.frames().map { frame ->
                val pitchDetector = pitchDetectorRegistry.detectorFor(settings.toneDetectionMethod)
                pitchDetector.detect(frame.samples, frame.sampleRate)?.let { pitch ->
                    TuningMath.readingFor(
                        frequencyHz = pitch.frequencyHz,
                        clarity = pitch.clarity,
                        referencePitchHz = settings.referencePitchHz,
                    )
                }
            }
        }.flowOn(defaultDispatcher)
}
