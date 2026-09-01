package com.lelloman.accordomi.data.tone

import com.lelloman.accordomi.core.di.DefaultDispatcher
import com.lelloman.accordomi.data.audio.AudioRecorder
import com.lelloman.accordomi.data.pitch.PitchDetectorRegistry
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.PitchReading
import com.lelloman.accordomi.domain.tone.ToneDetectionRepository
import com.lelloman.accordomi.domain.tone.ToneDetectionStatus
import com.lelloman.accordomi.domain.tone.TuningMath
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flow

class DefaultToneDetectionRepository @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val pitchDetectorRegistry: PitchDetectorRegistry,
    private val settingsRepository: SettingsRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ToneDetectionRepository {
    override fun readings(): Flow<ToneDetectionStatus> = flow {
        val stabilizer = PitchStabilizer()
        val lagTracker = FrameLagTracker()
        audioRecorder.frames()
            .conflate()
            .combine(settingsRepository.settings) { frame, settings ->
                val pitchDetector = pitchDetectorRegistry.detectorFor(settings.toneDetectionMethod)
                Triple(
                    settings,
                    pitchDetector.detect(frame.samples, frame.sampleRate),
                    frame.sequenceNumber,
                )
            }
            .collect { (settings, pitch, sequenceNumber) ->
                emit(
                    ToneDetectionStatus(
                        reading = stabilizer.update(pitch)?.let { stabilizedPitch ->
                            TuningMath.readingFor(
                                frequencyHz = stabilizedPitch.frequencyHz,
                                clarity = stabilizedPitch.clarity,
                                referencePitchHz = settings.referencePitchHz,
                            )
                        },
                        isLagging = lagTracker.update(sequenceNumber),
                    ),
                )
            }
    }.flowOn(defaultDispatcher)
}
