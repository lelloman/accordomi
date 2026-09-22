package com.lelloman.accordomi.data.piano

import android.annotation.SuppressLint
import android.content.Context
import com.lelloman.accordomi.core.di.DefaultDispatcher
import com.lelloman.accordomi.core.di.IoDispatcher
import com.lelloman.accordomi.data.audio.AndroidAudioRecorder
import com.lelloman.accordomi.domain.piano.*
import com.lelloman.accordomi.nativeaudio.PianoAnalyzer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AndroidPianoCapture @Inject constructor(
    private val recorder: AndroidAudioRecorder,
    @param:ApplicationContext private val context: Context,
    @param:DefaultDispatcher private val worker: CoroutineDispatcher,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : PianoCaptureSource {
    @SuppressLint("MissingPermission") // Explicit permission gate and lifecycle stop in PianoRoute/ViewModel.
    override fun readings(expectedHz: Double, knownB: Double?, recording: String?): Flow<PianoFrame> = flow {
        val writer = withContext(io) { recording?.let { PianoWavWriter(recordingFile(it)) } }
        try {
            val analyzer = PianoAnalyzer()
            emitAll(recorder.pianoFrames()
                .onEach { frame -> withContext(io) { writer?.append(frame.samples, frame.sequenceNumber) } }
                .conflate()
                .map { frame ->
                    val measurement = if (knownB == null) analyzer.analyze(frame.samples, frame.sampleRate, expectedHz)
                        else analyzer.measure(frame.samples, frame.sampleRate, expectedHz, knownB)
                    PianoFrame(measurement.copy(startSample = frame.sequenceNumber * 16384, recording = recording ?: ""), frame.sequenceNumber, (65536 + frame.sequenceNumber * 16384).toDouble()/frame.sampleRate)
                })
        } finally { withContext(NonCancellable + io) { writer?.close() } }
    }.flowOn(worker)

    override suspend fun discard(recording: String) { withContext(io) { recordingFile(recording).delete() } }
    private fun recordingFile(name: String): File {
        require(name.matches(Regex("[a-zA-Z0-9-]+\\.wav")))
        return File(File(context.filesDir, "piano-recordings").apply { mkdirs() }, name)
    }
}
