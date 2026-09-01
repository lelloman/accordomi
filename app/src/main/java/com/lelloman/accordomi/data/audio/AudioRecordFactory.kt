package com.lelloman.accordomi.data.audio

import android.Manifest
import androidx.annotation.RequiresPermission

interface AudioRecordFactory {
    fun minimumBufferSize(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
    ): Int

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun create(
        audioSource: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSize: Int,
    ): AudioRecordSession
}

interface AudioRecordSession {
    val isInitialized: Boolean

    fun start()

    fun read(buffer: ShortArray): Int

    fun stop()

    fun release()
}

class AudioRecordingException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
