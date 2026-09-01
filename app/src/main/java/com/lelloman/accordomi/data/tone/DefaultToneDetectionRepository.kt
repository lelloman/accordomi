package com.lelloman.accordomi.data.tone

import com.lelloman.accordomi.core.di.DefaultDispatcher
import com.lelloman.accordomi.data.audio.AudioRecorder
import com.lelloman.accordomi.data.pitch.PitchDetectionResult
import com.lelloman.accordomi.data.pitch.PitchDetectorRegistry
import com.lelloman.accordomi.domain.settings.SettingsRepository
import com.lelloman.accordomi.domain.tone.DetectionRate
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
import kotlinx.coroutines.flow.mapNotNull

class DefaultToneDetectionRepository @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val pitchDetectorRegistry: PitchDetectorRegistry,
    private val settingsRepository: SettingsRepository,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ToneDetectionRepository {
    override fun readings(): Flow<ToneDetectionStatus> = flow {
        val stabilizer = PitchStabilizer()
        val lagTracker = FrameLagTracker()
        var activeDetectionSettings: DetectionSettings? = null
        var lastAnalyzedSequenceNumber: Long? = null
        val detectionSettings = settingsRepository.settings
            .map { settings ->
                DetectionSettings(
                    method = settings.toneDetectionMethod,
                    rate = settings.detectionRate,
                )
            }
            .distinctUntilChanged()
        val referencePitch = settingsRepository.settings
            .map { it.referencePitchHz }
            .distinctUntilChanged()

        val stabilizedPitches = audioRecorder.frames()
            .conflate()
            .combine(detectionSettings) { frame, settings -> frame to settings }
            .mapNotNull { (frame, settings) ->
                val settingsChanged = activeDetectionSettings != settings
                if (settingsChanged) {
                    stabilizer.reset()
                    lagTracker.reset()
                    lastAnalyzedSequenceNumber = null
                }
                activeDetectionSettings = settings

                val previousSequenceNumber = lastAnalyzedSequenceNumber
                if (
                    previousSequenceNumber != null &&
                    frame.sequenceNumber - previousSequenceNumber < settings.rate.audioFrameStep
                ) {
                    return@mapNotNull null
                }
                lastAnalyzedSequenceNumber = frame.sequenceNumber

                StabilizedPitch(
                    pitch = stabilizer.update(
                        pitchDetectorRegistry.detectorFor(settings.method)
                            .detect(frame.samples, frame.sampleRate),
                    ),
                    isLagging = lagTracker.update(
                        sequenceNumber = frame.sequenceNumber,
                        expectedSequenceStep = settings.rate.audioFrameStep,
                    ),
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

    private data class DetectionSettings(
        val method: ToneDetectionMethod,
        val rate: DetectionRate,
    )
}
