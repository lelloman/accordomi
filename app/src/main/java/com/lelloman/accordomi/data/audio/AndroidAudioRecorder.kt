package com.lelloman.accordomi.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.lelloman.accordomi.core.di.IoDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class AndroidAudioRecorder @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AudioRecorder {
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun frames(): Flow<AudioFrame> = flow {
        val sampleRate = SampleRate
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBufferSize, FrameSize * BytesPerSample)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        val readBuffer = ShortArray(FrameSize)

        try {
            audioRecord.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = audioRecord.read(readBuffer, 0, readBuffer.size)
                if (read > 0) {
                    emit(
                        AudioFrame(
                            samples = FloatArray(read) { index ->
                                readBuffer[index] / Short.MAX_VALUE.toFloat()
                            },
                            sampleRate = sampleRate,
                        ),
                    )
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }.flowOn(ioDispatcher)

    private companion object {
        const val SampleRate = 44_100
        const val FrameSize = 4_096
        const val BytesPerSample = 2
    }
}

