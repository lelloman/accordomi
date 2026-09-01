package com.lelloman.accordomi.data.tone

import com.lelloman.accordomi.core.di.DefaultDispatcher
import com.lelloman.accordomi.data.audio.AudioRecorder
import com.lelloman.accordomi.data.pitch.PitchDetectionResult
import com.lelloman.accordomi.data.pitch.PitchDetectorRegistry
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.ToneDetectionMethod
import com.lelloman.accordomi.domain.tone.ToneDetectionRepository
import com.lelloman.accordomi.domain.tone.ToneDetectionStatus
import com.lelloman.accordomi.domain.tone.TuningMath
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class DefaultToneDetectionRepository @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val pitchDetectorRegistry: PitchDetectorRegistry,
    private val settingsRepository: SettingsRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ToneDetectionRepository {
    override fun readings(): Flow<ToneDetectionStatus> = flow {
        val stabilizer = PitchStabilizer()
        val lagTracker = FrameLagTracker()
        var activeMethod: ToneDetectionMethod? = null
        val detectionSettings = settingsRepository.settings
            .map { it.toneDetectionMethod }
            .distinctUntilChanged()
        val referencePitch = settingsRepository.settings
            .map { it.referencePitchHz }
            .distinctUntilChanged()

        val stabilizedPitches = audioRecorder.frames()
            .conflate()
            .combine(detectionSettings) { frame, method ->
                if (activeMethod != null && activeMethod != method) {
                    stabilizer.reset()
                }
                activeMethod = method
                StabilizedPitch(
                    pitch = stabilizer.update(
                        pitchDetectorRegistry.detectorFor(method)
                            .detect(frame.samples, frame.sampleRate),
                    ),
                    isLagging = lagTracker.update(frame.sequenceNumber),
                )
            }

        emitAll(
            stabilizedPitches.combine(referencePitch) { stabilizedPitch, referencePitchHz ->
                ToneDetectionStatus(
                    reading = stabilizedPitch.pitch?.let { pitch ->
                        TuningMath.readingFor(
                            frequencyHz = pitch.frequencyHz,
                            clarity = pitch.clarity,
                            referencePitchHz = referencePitchHz,
                        )
                    },
                    isLagging = stabilizedPitch.isLagging,
                )
            },
        )
    }.flowOn(defaultDispatcher)

    private data class StabilizedPitch(
        val pitch: PitchDetectionResult?,
        val isLagging: Boolean,
    )
}
