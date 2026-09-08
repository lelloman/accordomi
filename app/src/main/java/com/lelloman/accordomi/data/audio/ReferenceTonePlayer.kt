package com.lelloman.accordomi.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.lelloman.accordomi.core.di.IoDispatcher
import com.lelloman.accordomi.domain.tone.ReferenceToneOutput
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class ReferenceTonePlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ReferenceToneOutput {
    /** Cancellation owns cleanup; a focus interruption ends playback without automatic resume. */
    override suspend fun play(frequencyHz: Double, shouldStop: () -> Boolean) = withContext(ioDispatcher) {
        val oscillator = ReferenceOscillator(frequencyHz)
        val manager = context.getSystemService(AudioManager::class.java)
        val interrupted = AtomicBoolean(false)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener({ change ->
                if (change != AudioManager.AUDIOFOCUS_GAIN) interrupted.set(true)
            }, Handler(Looper.getMainLooper()))
            .build()
        check(manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        var track: AudioTrack? = null
        try {
            val minimum = AudioTrack.getMinBufferSize(
                oscillator.sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
            )
            check(minimum > 0)
            val output = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(oscillator.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(maxOf(minimum, 2048))
                .build()
            track = output
            check(output.state == AudioTrack.STATE_INITIALIZED)
            val buffer = ShortArray(1024)
            output.play()
            while (!interrupted.get()) {
                currentCoroutineContext().ensureActive()
                val releasing = shouldStop()
                oscillator.fill(buffer, releasing)
                var offset = 0
                while (offset < buffer.size) {
                    currentCoroutineContext().ensureActive()
                    val written = output.write(buffer, offset, buffer.size - offset, AudioTrack.WRITE_BLOCKING)
                    check(written > 0) { "Audio output failed ($written)." }
                    offset += written
                }
                if (releasing) {
                    // stop() drains the streaming buffer, including the release envelope.
                    output.stop()
                    break
                }
            }
        } finally {
            try {
                track?.release()
            } finally {
                manager.abandonAudioFocusRequest(focus)
            }
        }
    }
}
