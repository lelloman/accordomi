package com.lelloman.accordomi.data.audio

import android.Manifest
import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import javax.inject.Inject

class AndroidAudioRecordFactory @Inject constructor() : AudioRecordFactory {
    override fun minimumBufferSize(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
    ): Int = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun create(
        audioSource: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSize: Int,
    ): AudioRecordSession = AndroidAudioRecordSession(
        AudioRecord(
            audioSource,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize,
        ),
    )
}

private class AndroidAudioRecordSession(
    private val audioRecord: AudioRecord,
) : AudioRecordSession {
    override val isInitialized: Boolean
        get() = audioRecord.state == AudioRecord.STATE_INITIALIZED

    override fun start() {
        audioRecord.startRecording()
    }

    override fun read(buffer: ShortArray): Int = audioRecord.read(
        buffer,
        0,
        buffer.size,
        AudioRecord.READ_BLOCKING,
    )

    override fun stop() {
        audioRecord.stop()
    }

    override fun release() {
        audioRecord.release()
    }
}
