package com.lelloman.accordomi.data.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.lelloman.accordomi.core.di.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class AndroidAudioRecorder @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val audioRecordFactory: AudioRecordFactory,
) : AudioRecorder {
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun frames(): Flow<AudioFrame> = flow {
        val sampleRate = SampleRate
        val minBufferSize = audioRecordFactory.minimumBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) {
            throw AudioRecordingException(
                "Unable to determine a supported audio buffer size (error $minBufferSize).",
            )
        }
        val bufferSize = maxOf(minBufferSize, FrameSize * BytesPerSample)
        val audioRecord = try {
            audioRecordFactory.create(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        } catch (error: Exception) {
            throw AudioRecordingException("Unable to create the audio recorder.", error)
        }
        val readBuffer = ShortArray(HopSize)
        val frameBuffer = OverlappingAudioFrameBuffer(
            frameSize = FrameSize,
            hopSize = HopSize,
        )
        var nextSequenceNumber = 0L
        var recordingStarted = false
        var recordingFailure: Throwable? = null

        try {
            if (!audioRecord.isInitialized) {
                throw AudioRecordingException("Audio recorder initialization failed.")
            }

            audioRecord.start()
            recordingStarted = true
            while (currentCoroutineContext().isActive) {
                when (val read = audioRecord.read(readBuffer)) {
                    in 1..readBuffer.size -> frameBuffer
                        .append(readBuffer, read)
                        .forEach { samples ->
                            emit(
                                AudioFrame(
                                    samples = samples,
                                    sampleRate = sampleRate,
                                    sequenceNumber = nextSequenceNumber++,
                                ),
                            )
                        }
                    0 -> delay(EmptyReadRetryDelayMillis)
                    AudioRecord.ERROR_DEAD_OBJECT -> throw AudioRecordingException(
                        "Audio recorder became unavailable and must be recreated.",
                    )
                    AudioRecord.ERROR_INVALID_OPERATION -> throw AudioRecordingException(
                        "Audio recorder is not in a valid state for reading.",
                    )
                    AudioRecord.ERROR_BAD_VALUE -> throw AudioRecordingException(
                        "Audio recorder rejected the read buffer.",
                    )
                    AudioRecord.ERROR -> throw AudioRecordingException("Audio recording failed.")
                    else -> throw AudioRecordingException(
                        "Audio recorder returned an unexpected read result ($read).",
                    )
                }
            }
        } catch (error: Throwable) {
            val reportedError = when (error) {
                is AudioRecordingException, is CancellationException -> error
                else -> AudioRecordingException("Audio recorder operation failed.", error)
            }
            recordingFailure = reportedError
            throw reportedError
        } finally {
            val cleanupFailure = audioRecord.cleanup(recordingStarted)
            if (cleanupFailure != null) {
                if (recordingFailure != null) {
                    recordingFailure.addSuppressed(cleanupFailure)
                } else {
                    throw cleanupFailure
                }
            }
        }
    }.flowOn(ioDispatcher).buffer(capacity = 0)

    private fun AudioRecordSession.cleanup(recordingStarted: Boolean): Throwable? {
        var failure: Throwable? = null
        if (recordingStarted) {
            try {
                stop()
            } catch (error: Throwable) {
                failure = error
            }
        }

        try {
            release()
        } catch (error: Throwable) {
            if (failure == null) {
                failure = error
            } else {
                failure.addSuppressed(error)
            }
        }
        return failure
    }

    private companion object {
        const val SampleRate = 44_100
        const val FrameSize = 4_096
        const val HopSize = 1_024
        const val BytesPerSample = 2
        const val EmptyReadRetryDelayMillis = 10L
    }
}
