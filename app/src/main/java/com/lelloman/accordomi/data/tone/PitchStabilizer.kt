package com.lelloman.accordomi.data.tone

import com.lelloman.accordomi.data.pitch.PitchDetectionResult
import com.lelloman.accordomi.nativeaudio.NativeAudio

class PitchStabilizer {
    private val state = DoubleArray(7)

    fun reset() { state.fill(0.0) }

    fun update(result: PitchDetectionResult?): PitchDetectionResult? =
        NativeAudio.stabilize(state, result?.frequencyHz ?: 0.0, result?.clarity ?: 0f)?.let {
            PitchDetectionResult(it[0], it[1].toFloat())
        }
}
